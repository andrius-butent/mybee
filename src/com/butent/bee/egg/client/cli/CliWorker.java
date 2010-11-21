package com.butent.bee.egg.client.cli;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.egg.client.BeeGlobal;
import com.butent.bee.egg.client.BeeKeeper;
import com.butent.bee.egg.client.communication.ParameterList;
import com.butent.bee.egg.client.communication.RpcList;
import com.butent.bee.egg.client.data.JsData;
import com.butent.bee.egg.client.dom.DomUtils;
import com.butent.bee.egg.client.dom.Features;
import com.butent.bee.egg.client.event.EventUtils;
import com.butent.bee.egg.client.grid.FlexTable;
import com.butent.bee.egg.client.layout.Direction;
import com.butent.bee.egg.client.layout.Flow;
import com.butent.bee.egg.client.layout.Split;
import com.butent.bee.egg.client.layout.TilePanel;
import com.butent.bee.egg.client.tree.BeeTree;
import com.butent.bee.egg.client.utils.BeeJs;
import com.butent.bee.egg.client.utils.JreEmulation;
import com.butent.bee.egg.client.widget.Audio;
import com.butent.bee.egg.client.widget.BeeLabel;
import com.butent.bee.egg.client.widget.Canvas;
import com.butent.bee.egg.client.widget.Svg;
import com.butent.bee.egg.client.widget.Video;
import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.BeeDate;
import com.butent.bee.egg.shared.BeeService;
import com.butent.bee.egg.shared.communication.ContentType;
import com.butent.bee.egg.shared.data.DataUtils;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.Codec;
import com.butent.bee.egg.shared.utils.PropUtils;
import com.butent.bee.egg.shared.utils.StringProp;
import com.butent.bee.egg.shared.utils.SubProp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class CliWorker {

  public static void clearLog() {
    BeeKeeper.getLog().clear();
  }

  public static void digest(String v) {
    String src = null;
    int p = v.indexOf(BeeConst.CHAR_SPACE);

    if (p > 0) {
      String z = v.substring(p + 1).trim();

      if (BeeUtils.isDigit(z)) {
        int x = BeeUtils.toInt(z);
        if (BeeUtils.betweenInclusive(x, 1, BeeUtils.exp10(6))) {
          src = BeeUtils.randomString(x, x, BeeConst.CHAR_SPACE, '\u0800');
        } else {
          src = z;
        }
      } else if (z.length() > 0) {
        src = z;
      }
    }

    if (src == null) {
      src = BeeUtils.randomString(10, 20, BeeConst.CHAR_SPACE, '\u0400');
    }

    if (src.length() > 100) {
      BeeKeeper.getLog().info("Source length", src.length());
    } else {
      BeeKeeper.getLog().info(Codec.escapeUnicode(src));
    }

    BeeKeeper.getLog().info("js", BeeJs.md5(src));
    BeeKeeper.getLog().info("js fast", BeeJs.md5fast(src));
    BeeKeeper.getLog().info(BeeConst.CLIENT, Codec.md5(src));

    BeeKeeper.getRpc().makePostRequest(BeeService.SERVICE_GET_DIGEST,
        ContentType.BINARY, src);
  }

  public static void doLog(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      String z = arr[1];

      if (BeeUtils.inList(z, BeeConst.STRING_ZERO, BeeConst.STRING_MINUS)) {
        BeeKeeper.getLog().hide();
      } else if (BeeUtils.isDigit(z)) {
        BeeKeeper.getLog().resize(BeeUtils.toInt(z));
      } else if (BeeUtils.startsSame(z, "clear")) {
        BeeKeeper.getLog().clear();
      } else {
        BeeKeeper.getLog().show();
        BeeKeeper.getLog().info((Object[]) arr);
        BeeKeeper.getLog().addSeparator();
      }

      return;
    }

    Level[] levels = new Level[]{
        Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO,
        Level.WARNING, Level.SEVERE};
    for (Level lvl : levels) {
      BeeKeeper.getLog().log(lvl, lvl.getName().toLowerCase());
    }
    BeeKeeper.getLog().addSeparator();
  }

  public static void doMenu(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_MENU);
      params.addPositionalHeader(arr[1]);
      BeeKeeper.getRpc().makeGetRequest(params);
    } else {
      BeeKeeper.getMenu().showMenu();
    }
  }

  public static void doScreen(String[] arr) {
    Split screen = BeeKeeper.getUi().getScreenPanel();
    Assert.notNull(screen);

    String p1 = BeeUtils.arrayGetQuietly(arr, 0);
    String p2 = BeeUtils.arrayGetQuietly(arr, 1);

    if (BeeUtils.same(p1, "screen")) {
      BeeKeeper.getUi().showGrid(screen.getInfo());
      return;
    }

    Direction dir = DomUtils.getDirection(p1);
    if (dir == null) {
      BeeGlobal.sayHuh(p1, p2);
      return;
    }

    if (BeeUtils.isEmpty(p2)) {
      BeeKeeper.getUi().showGrid(screen.getDirectionInfo(dir));
      return;
    }

    double size = BeeUtils.toDouble(p2);
    if (Double.isNaN(size)) {
      BeeGlobal.showError(p1, p2, "NaN");
      return;
    }

    screen.setDirectionSize(dir, size);
  }

  public static void eval(String v, String[] arr) {
    String xpr = v.substring(arr[0].length()).trim();

    if (BeeUtils.isEmpty(xpr)) {
      BeeGlobal.sayHuh(v);
    } else {
      BeeGlobal.showDialog(xpr, BeeJs.evalToString(xpr));
    }
  }

  public static void getCharsets() {
    ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader("cs");

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getFs() {
    ParameterList params = BeeKeeper.getRpc().createParameters(BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader("fs");

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getKeys(String[] arr) {
    int parCnt = BeeUtils.length(arr) - 1;
    if (parCnt <= 0) {
      BeeGlobal.showError("getKeys", "table not specified");
      return;
    }
    
    ParameterList params = BeeKeeper.getRpc().createParameters(
        BeeUtils.same(arr[0], "pk") ? BeeService.SERVICE_DB_PRIMARY : BeeService.SERVICE_DB_KEYS);
    for (int i = 0; i < parCnt; i++) {
      params.addPositionalHeader(arr[i + 1]);
    }
    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void getResource(String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      BeeGlobal.sayHuh(BeeUtils.transform(arr));
      return;
    }

    ParameterList params = BeeKeeper.getRpc().createParameters(
        BeeService.SERVICE_GET_RESOURCE);
    params.addPositionalHeader(arr);

    BeeKeeper.getRpc().makeGetRequest(params);
  }

  public static void playAudio(String[] arr) {
    if (!Features.supportsAudio()) {
      BeeKeeper.getLog().severe("audio not supported");
      return;
    }
    
    String src = BeeUtils.arrayGetQuietly(arr, 1);
    if (BeeUtils.isEmpty(src)) {
      BeeKeeper.getLog().warning("source not specified");
      return;
    }
    
    Audio widget = new Audio();
    widget.getElement().setAttribute("src", src);
    widget.getElement().setAttribute("controls", "controls");
    
    BeeKeeper.getUi().updateActivePanel(widget, true);
  }

  public static void playVideo(String[] arr) {
    if (!Features.supportsVideo()) {
      BeeKeeper.getLog().severe("video not supported");
      return;
    }
    
    String src = BeeUtils.arrayGetQuietly(arr, 1);
    if (BeeUtils.isEmpty(src)) {
      src = "http://people.opera.com/shwetankd/webm/sunflower.webm";
    }
    
    Video widget = new Video();
    widget.getElement().setAttribute("src", src);
    widget.getElement().setAttribute("controls", "controls");
    
    BeeKeeper.getUi().updateActivePanel(widget, true);
  }

  public static void showCanvas(String[] arr) {
    if (!Features.supportsCanvas()) {
      BeeKeeper.getLog().severe("canvas not supported");
      return;
    }
    
    Canvas widget = new Canvas();
    BeeKeeper.getUi().updateActivePanel(widget);
    
    if (BeeUtils.arrayLength(arr) <= 1) {
      sampleCanvas(widget.getElement());
    }
  }

  public static void showDate(String[] arr) {
    int len = BeeUtils.length(arr);
    BeeDate date;

    if (len == 2 && BeeUtils.isDigit(arr[1])) {
      date = new BeeDate(arr[1]);

    } else if (len >= 3) {
      int[] fields = new int[7];
      for (int i = 0; i < fields.length; i++) {
        if (i < len - 1) {
          fields[i] = BeeUtils.toInt(arr[i + 1]);
        } else {
          fields[i] = 0;
        }
      }
      date = new BeeDate(fields[0], fields[1], fields[2], fields[3], fields[4],
          fields[5], fields[6]);

    } else {
      date = new BeeDate();
    }

    List<StringProp> lst = PropUtils.createStringProp("Time", date.getTime(),
        "Year", date.getYear(), "Month", date.getMonth(), "Dom", date.getDom(),
        "Dow", date.getDow(), "Doy", date.getDoy(), "Hour", date.getHour(),
        "Minute", date.getMinute(), "Second", date.getSecond(), "Millis",
        date.getMillis(), "Log", date.toLog(), "String", date.toString(),
        "Date", new Date(date.getTime()).toString(), "Tz Offset",
        BeeGlobal.getTzo());

    BeeKeeper.getUi().showGrid(lst);
  }

  public static void showDateFormat() {
    int r = DateTimeFormat.PredefinedFormat.values().length;
    String[][] data = new String[r][2];

    int i = 0;
    for (DateTimeFormat.PredefinedFormat dtf : DateTimeFormat.PredefinedFormat.values()) {
      data[i][0] = dtf.toString();
      data[i][1] = DateTimeFormat.getFormat(dtf).format(new Date());
      i++;
    }

    BeeKeeper.getUi().showGrid(data, "Format", "Value");
  }

  public static void showDnd() {
    if (!EventUtils.supportsDnd()) {
      BeeKeeper.getLog().warning("dnd not supported");
      return;
    }

    List<StringProp> lst = EventUtils.showDnd();
    if (BeeUtils.isEmpty(lst)) {
      BeeGlobal.showDialog("dnd mappings empty");
    } else if (lst.size() <= 30) {
      BeeGlobal.modalGrid(BeeUtils.concat(1, "Dnd", BeeUtils.bracket(lst.size())), lst);
    } else {
      BeeKeeper.getUi().showGrid(lst);
    }
  }

  public static void showElement(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      BeeGlobal.sayHuh(v);
      return;
    }

    JavaScriptObject obj = DOM.getElementById(arr[1]);
    if (obj == null) {
      BeeGlobal.showError(arr[1], "element id not found");
      return;
    }

    String patt = BeeUtils.arrayGetQuietly(arr, 2);
    JsArrayString prp = BeeJs.getProperties(obj, patt);

    if (BeeJs.isEmpty(prp)) {
      BeeGlobal.showError(v, "properties not found");
      return;
    }

    JsData view = (JsData) DataUtils.createView(prp, "property", "type", "value");
    view.sort(0, true);

    if (view.getRowCount() <= 20) {
      BeeGlobal.modalGrid(v, view);
    } else {
      BeeKeeper.getUi().showGrid(view);
    }
  }

  public static void showFields(String[] arr) {
    if (BeeUtils.length(arr) > 1) {
      BeeGlobal.showFields(JreEmulation.copyOfRange(arr, 1, arr.length));
    } else {
      BeeGlobal.showFields();
    }
  }

  public static void showFunctions(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      BeeGlobal.sayHuh(v);
      return;
    }

    JavaScriptObject obj = BeeJs.eval(arr[1]);
    if (obj == null) {
      BeeGlobal.showError(arr[1], "not a js object");
      return;
    }

    String patt = BeeUtils.arrayGetQuietly(arr, 2);
    JsArrayString fnc = BeeJs.getFunctions(obj, patt);

    if (BeeJs.isEmpty(fnc)) {
      BeeGlobal.showError(v, "functions not found");
      return;
    }
    if (fnc.length() <= 5) {
      BeeGlobal.showDialog(v, fnc.join());
      return;
    }

    JsData view = (JsData) DataUtils.createView(fnc, "function");
    view.sort(0, true);

    if (BeeUtils.same(arr[0], "f") && view.getRowCount() <= 20) {
      BeeGlobal.modalGrid(v, view);
    } else {
      BeeKeeper.getUi().showGrid(view);
    }
  }

  public static void showGeo() {
    BeeLabel widget = new BeeLabel("Looking for location...");
    getGeo(widget.getElement());
    BeeKeeper.getUi().updateActivePanel(widget);
  }

  public static void showGwt() {
    BeeGlobal.modalGrid("GWT", PropUtils.createStringProp("Host Page Base URL",
        GWT.getHostPageBaseURL(), "Module Base URL", GWT.getModuleBaseURL(),
        "Module Name", GWT.getModuleName(), "Permutation Strong Name",
        GWT.getPermutationStrongName(), "Uncaught Exception Handler",
        GWT.getUncaughtExceptionHandler(), "Unique Thread Id",
        GWT.getUniqueThreadId(), "Version", GWT.getVersion(), "Is Client",
        GWT.isClient(), "Is Prod Mode", GWT.isProdMode(), "Is Script",
        GWT.isScript()));
  }

  public static void showInput() {
    FlexTable table = new FlexTable();
    table.setCellSpacing(3);
    
    String[] types = new String[] {"search", "tel", "url", "email",
        "datetime", "date", "month", "week", "time", "datetime-local",
        "number", "range", "color"};
    TextBox widget;
    
    int row = 0;
    for (String type : types) {
      table.setWidget(row, 0, new BeeLabel(type));
      
      if (Features.supportsInputType(type)) {
        widget = new TextBox();
        widget.getElement().setAttribute(DomUtils.ATTRIBUTE_TYPE, type);
        
        if (type.equals("search")) {
          if (Features.supportsAttributePlaceholder()) {
            widget.getElement().setAttribute("placeholder", "Search...");
            widget.getElement().setAttribute("results", "0");
          }
        } else if (type.equals("number")) {
          widget.getElement().setAttribute("min", "0");
          widget.getElement().setAttribute("max", "20");
          widget.getElement().setAttribute("step", "2");
          widget.getElement().setAttribute("value", "4");
        } else if (type.equals("range")) {
          widget.getElement().setAttribute("min", "0");
          widget.getElement().setAttribute("max", "50");
          widget.getElement().setAttribute("step", "5");
          widget.getElement().setAttribute("value", "30");
        }
        
        table.setWidget(row, 1, widget);
      } else {
        table.setWidget(row, 1, new BeeLabel("not supported"));
      }
      
      row++;
    }
    
    BeeKeeper.getUi().updateActivePanel(table, true);
  }

  public static void showProperties(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      BeeGlobal.sayHuh(v);
      return;
    }

    JavaScriptObject obj = BeeJs.eval(arr[1]);
    if (obj == null) {
      BeeGlobal.showError(arr[1], "not a js object");
      return;
    }

    String patt = BeeUtils.arrayGetQuietly(arr, 2);
    JsArrayString prp = BeeJs.getProperties(obj, patt);

    if (BeeJs.isEmpty(prp)) {
      BeeGlobal.showError(v, "properties not found");
      return;
    }

    JsData view = (JsData) DataUtils.createView(prp, "property", "type", "value");
    view.sort(0, true);

    if (BeeUtils.same(arr[0], "p") && view.getRowCount() <= 20) {
      BeeGlobal.modalGrid(v, view);
    } else {
      BeeKeeper.getUi().showGrid(view);
    }
  }

  public static void showRpc() {
    if (BeeKeeper.getRpc().getRpcList().isEmpty()) {
      BeeGlobal.showDialog("RpcList empty");
    } else {
      BeeKeeper.getUi().showGrid(BeeKeeper.getRpc().getRpcList().getDefaultInfo(),
          RpcList.DEFAULT_INFO_COLUMNS);
    }
  }

  public static void showStack() {
    BeeKeeper.getLog().stack();
    BeeKeeper.getLog().addSeparator();
  }

  public static void showSupport() {
    BeeKeeper.getUi().showGrid(Features.getInfo());
  }

  public static void showSvg(String[] arr) {
    if (!Features.supportsSvgInline()) {
      BeeKeeper.getLog().severe("svg not supported");
      return;
    }
    
    Svg widget = new Svg();
    Flow panel = new Flow();
    panel.add(widget);
    BeeKeeper.getUi().updateActivePanel(panel);
    
    if (BeeUtils.arrayLength(arr) <= 1) {
      sampleSvg(widget.getElement());
    }
  }

  public static void showTiles() {
    Widget tiles = BeeKeeper.getUi().getScreenPanel().getCenter();
    if (!(tiles instanceof TilePanel)) {
      BeeGlobal.showDialog("no tiles vailable");
    }

    BeeTree tree = new BeeTree();
    tree.addItem(((TilePanel) tiles).getTree(null, true));

    BeeGlobal.inform(tree);
  }
  
  public static void storage(String[] arr) {
    int parCnt = BeeUtils.arrayLength(arr) - 1;
    int len = BeeKeeper.getStorage().length();
    
    if (parCnt <= 1 && len <= 0) {
      BeeGlobal.inform("Storage empty");
      return;
    }
    
    if (parCnt <= 0) {
      BeeKeeper.getUi().showGrid(BeeKeeper.getStorage().getAll());
      return;
    }
    
    String key = arr[1];
    
    if (parCnt == 1) {
      if (key.equals(BeeConst.STRING_MINUS)) {
        BeeKeeper.getStorage().clear();
        BeeGlobal.inform(BeeUtils.concat(1, len, "items cleared"));
      } else {
        String z = BeeKeeper.getStorage().getItem(key);
        if (z == null) {
          BeeGlobal.showError(key, "key not found");
        } else {
          BeeGlobal.inform(key, z);
        }
      }
      return;
    }
    
    String value = BeeUtils.join(arr, 1, 2);
    
    if (key.equals(BeeConst.STRING_MINUS)) {
      BeeKeeper.getStorage().removeItem(value);
      BeeGlobal.inform(value, "removed");
    } else {
      BeeKeeper.getStorage().setItem(key, value);
      BeeGlobal.inform("Storage", BeeUtils.addName(key, value));
    }
  }

  public static void style(String v, String[] arr) {
    if (BeeUtils.length(arr) < 2) {
      NodeList<Element> nodes = Document.get().getElementsByTagName("style");
      if (nodes == null || nodes.getLength() <= 0) {
        BeeGlobal.showDialog("styles not available");
        return;
      }
      int stCnt = nodes.getLength();

      List<SubProp> lst = new ArrayList<SubProp>();
      PropUtils.addSub(lst, "Styles", "count", stCnt);

      for (int i = 0; i < stCnt; i++) {
        String ref = "$doc.getElementsByTagName('style').item(" + i
            + ").sheet.rules";
        int len = BeeJs.evalToInt(ref + ".length");
        PropUtils.addSub(lst, "Style " + BeeUtils.progress(i + 1, stCnt),
            "rules", len);

        for (int j = 0; j < len; j++) {
          JavaScriptObject obj = BeeJs.eval(ref + "[" + j + "]");
          if (obj == null) {
            PropUtils.addSub(lst, "Rule", BeeUtils.progress(j + 1, len),
                "not available");
            break;
          }

          JsArrayString prp = BeeJs.getProperties(obj, null);
          for (int k = 0; k < prp.length() - 2; k += 3) {
            PropUtils.addSub(
                lst,
                BeeUtils.concat(1, "Rule", BeeUtils.progress(j + 1, len),
                    prp.get(k * 3)), prp.get(k * 3 + 1), prp.get(k * 3 + 2));
          }
        }
      }

      BeeKeeper.getUi().showGrid(lst);
      return;
    }

    if (!v.contains("{")) {
      Element elem = Document.get().getElementById(arr[1]);
      if (elem == null) {
        BeeGlobal.showDialog("element id", arr[1], "not found");
        return;
      }

      List<StringProp> lst = DomUtils.getStyleInfo(elem.getStyle());
      if (BeeUtils.isEmpty(lst)) {
        BeeGlobal.showDialog("element id", arr[1], "has no style");
      } else {
        BeeKeeper.getUi().showGrid(lst);
      }
      return;
    }

    boolean start = false;
    boolean end = false;
    boolean immediate = false;

    StringBuilder sb = new StringBuilder();
    boolean tg = false;

    for (int i = 1; i < arr.length; i++) {
      if (!tg) {
        if (arr[i].contains("{")) {
          tg = true;
        } else {
          if (BeeUtils.same(arr[i], "start")) {
            start = true;
            continue;
          }
          if (BeeUtils.same(arr[i], "end")) {
            end = true;
            continue;
          }
          if (arr[i].equals("+")) {
            immediate = true;
            continue;
          }
        }
      }
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(arr[i]);
    }

    String st = sb.toString();
    if (!st.contains("{") || !st.contains("}")) {
      BeeGlobal.showError("Nah pop no style, a strictly roots", v, st);
      return;
    }

    BeeKeeper.getLog().info(st);
    BeeKeeper.getLog().addSeparator();

    if (start) {
      StyleInjector.injectAtStart(st, immediate);
    } else if (end) {
      StyleInjector.injectAtEnd(st, immediate);
    } else {
      StyleInjector.inject(st, immediate);
    }
  }
  
  public static void unicode(String[] arr) {
    StringBuilder sb = new StringBuilder();
    int len = BeeUtils.length(arr);

    if (len < 2 || len == 2 && BeeUtils.isDigit(BeeUtils.getElement(arr, 1))) {
      int n = (len < 2) ? 10 : BeeUtils.toInt(arr[1]);
      for (int i = 0; i < n; i++) {
        sb.append((char) BeeUtils.randomInt(Character.MIN_VALUE,
            Character.MAX_VALUE + 1));
      }

    } else {
      for (int i = 1; i < len; i++) {
        String s = arr[i];

        if (s.length() > 1
            && BeeUtils.inListIgnoreCase(s.substring(0, 1), "u", "x")
            && BeeUtils.isHexString(s.substring(1))) {
          sb.append(BeeUtils.fromHex(s.substring(1)));
        } else if (s.length() > 2 && BeeUtils.startsSame(s, "0x")
            && BeeUtils.isHexString(s.substring(2))) {
          sb.append(BeeUtils.fromHex(s.substring(2)));

        } else if (BeeUtils.isDigit(s)) {
          int n = BeeUtils.toInt(s);
          if (n > 0 && n < Character.MAX_VALUE && sb.length() > 0) {
            for (int j = 0; j < n; j++) {
              sb.append((char) (sb.charAt(sb.length() - 1) + 1));
            }
          } else {
            sb.append((char) n);
          }

        } else {
          sb.append(s);
        }
      }
    }

    String s = sb.toString();
    byte[] bytes = Codec.toBytes(s);

    int id = BeeKeeper.getRpc().invoke("stringInfo", ContentType.BINARY, s);
    BeeKeeper.getRpc().addUserData(id, "length", s.length(), "data", s,
        "adler32", Codec.adler32(bytes), "crc16", Codec.crc16(bytes), "crc32",
        Codec.crc32(bytes), "crc32d", Codec.crc32Direct(bytes));
  }

  public static void whereAmI() {
    BeeKeeper.getLog().info(BeeConst.whereAmI());
    BeeKeeper.getRpc().makeGetRequest(BeeService.SERVICE_WHERE_AM_I);
  }

  private static native void getGeo(Element element) /*-{
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(showPosition);
    } else {
      element.innerHTML = "no geolocation support";
    }

    function showPosition(position) {
      var lat = position.coords.latitude;
      var lng = position.coords.longitude;
      element.innerHTML = "Lat = " + lat + ", Lng = " + lng;
    }
  }-*/;
  
  private static native void sampleCanvas(Element el) /*-{
    var ctx = el.getContext("2d");
    
    for (var i = 0; i < 6; i++) {  
      for (var j = 0; j < 6; j++) {  
        ctx.fillStyle = 'rgb(' + Math.floor(255 - 42.5 * i) + ', ' + Math.floor(255 - 42.5 * j) + ', 0)';  
        ctx.fillRect(j*25, i*25, 25, 25);  
      }  
    }
  }-*/;
  
  private static void sampleSvg(Element el) {
    el.setInnerHTML("<circle cx=\"100\" cy=\"75\" r=\"50\" fill=\"blue\" stroke=\"firebrick\" stroke-width=\"3\"></circle>"
        + "<text x=\"60\" y=\"155\">Hello Svg</text>");
  }

}
