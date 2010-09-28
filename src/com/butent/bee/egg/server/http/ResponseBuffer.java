package com.butent.bee.egg.server.http;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeDate;
import com.butent.bee.egg.shared.BeeResource;
import com.butent.bee.egg.shared.BeeService;
import com.butent.bee.egg.shared.data.BeeColumn;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.Codec;
import com.butent.bee.egg.shared.utils.StringProp;
import com.butent.bee.egg.shared.utils.SubProp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

public class ResponseBuffer {
  private char[] separator;
  private StringBuilder buffer = new StringBuilder();
  private int count = 0;

  private int columnCount = 0;

  private List<ResponseMessage> messages = new ArrayList<ResponseMessage>();
  private List<BeeResource> parts = new ArrayList<BeeResource>();

  private BeeService.DATA_TYPE dataType;

  private String contentType = null;
  private String characterEncoding = null;

  public ResponseBuffer() {
    setDefaultSeparator();
  }

  public ResponseBuffer(BeeService.DATA_TYPE dataType) {
    this.dataType = dataType;
  }

  public ResponseBuffer(char sep) {
    this.separator = new char[]{sep};
  }

  public ResponseBuffer(String sep) {
    if (BeeUtils.isHexString(sep)) {
      setHexSeparator(sep);
    } else {
      setDefaultSeparator();
    }
  }

  public void add(CharSequence s) {
    if (s != null && s.length() > 0) {
      checkSeparator(s);
      buffer.append(s);
    }

    addSeparator();
  }

  public void add(Collection<?> lst) {
    if (BeeUtils.isEmpty(lst)) {
      addSeparator();
    } else {
      for (Iterator<?> it = lst.iterator(); it.hasNext();) {
        add(it.next());
      }
    }
  }

  public void add(Object x) {
    add(BeeUtils.transform(x));
  }

  public void add(Object... lst) {
    if (lst.length == 0) {
      addSeparator();
    } else {
      for (int i = 0; i < lst.length; i++) {
        add(lst[i]);
      }
    }
  }

  public void addColumn(BeeColumn col) {
    Assert.notEmpty(col);

    add(col.serialize());
    setColumnCount(getColumnCount() + 1);
  }

  public void addColumns(BeeColumn... cols) {
    Assert.isPositive(cols.length);

    for (BeeColumn col : cols) {
      addColumn(col);
    }
  }

  public void addError(Throwable err) {
    messages.add(new ResponseMessage(Level.SEVERE, err.toString()));
  }

  public void addErrors(List<? extends Throwable> lst) {
    for (Throwable err : lst) {
      addError(err);
    }
  }

  public void addLine(Object... obj) {
    if (obj.length > 0) {
      add(BeeUtils.concat(1, obj));
    }
  }

  public void addMessage(Level level, Object... obj) {
    messages.add(new ResponseMessage(level, obj));
  }

  public void addMessage(Level level, String msg) {
    messages.add(new ResponseMessage(level, msg));
  }

  public void addMessage(Object... obj) {
    messages.add(new ResponseMessage(obj));
  }

  public void addMessage(String msg) {
    messages.add(new ResponseMessage(msg));
  }

  public void addMessages(Level level, String... msg) {
    for (String s : msg) {
      addMessage(level, s);
    }
  }

  public void addMessages(String... msg) {
    for (String s : msg) {
      addMessage(s);
    }
  }

  public void addPart(String uri, String content) {
    addPart(uri, content, null, false);
  }

  public void addPart(String uri, String content, BeeService.DATA_TYPE type) {
    addPart(uri, content, type, false);
  }

  public void addPart(String uri, String content, BeeService.DATA_TYPE type,
      boolean readOnly) {

    Assert.notNull(content);
    parts.add(new BeeResource(uri, content, type, readOnly));
    setDataType(BeeService.DATA_TYPE.MULTIPART);
  }

  public void addPropString(StringProp el) {
    Assert.notEmpty(el);

    add(el.getName());
    add(el.getValue());
    add(new BeeDate().toLog());
  }

  public void addPropSub(SubProp el) {
    Assert.notEmpty(el);

    add(el.getName());
    add(el.getSub());
    add(el.getValue());
    add(el.getDate().toLog());
  }

  public void addResource(String content) {
    addResource(null, content, null, true);
  }

  public void addResource(String content, BeeService.DATA_TYPE type) {
    addResource(null, content, type, true);
  }

  public void addResource(String uri, String content) {
    addResource(uri, content, null, false);
  }

  public void addResource(String uri, String content, BeeService.DATA_TYPE type) {
    addResource(uri, content, type, false);
  }

  public void addResource(String uri, String content,
      BeeService.DATA_TYPE type, boolean readOnly) {
    Assert.notNull(content);
    buffer.append(new BeeResource(uri, content, type, readOnly).serialize());
    count++;
    setDataType(BeeService.DATA_TYPE.RESOURCE);
  }

  public void addSeparator() {
    buffer.append(separator);
    count++;
  }

  public void addSevere(Object... obj) {
    messages.add(new ResponseMessage(Level.SEVERE, obj));
  }

  public void addStringColumns(String... cap) {
    int c = cap.length;
    String nm;

    for (int i = 0; i < StringProp.HEADER_COUNT + 1; i++) {
      if (c > i && !BeeUtils.isEmpty(cap[i])) {
        nm = cap[i].trim();
      } else if (i < StringProp.HEADER_COUNT) {
        nm = SubProp.COLUMN_HEADERS[i];
      } else {
        nm = "Date";
      }

      addColumn(new BeeColumn(nm));
    }
  }

  public void addStringProp(Collection<StringProp> lst, String... cap) {
    Assert.notEmpty(lst);
    if (getColumnCount() == 0) {
      addStringColumns(cap);
    }

    for (StringProp el : lst) {
      addPropString(el);
    }
  }

  public void addSub(Collection<SubProp> lst, String... cap) {
    Assert.notEmpty(lst);
    addSubColumns(cap);

    for (SubProp el : lst) {
      addPropSub(el);
    }
  }

  public void addSubColumns(String... cap) {
    int c = cap.length;
    String nm;

    for (int i = 0; i < SubProp.COLUMN_COUNT; i++) {
      if (c > i && !BeeUtils.isEmpty(cap[i])) {
        nm = cap[i].trim();
      } else {
        nm = SubProp.COLUMN_HEADERS[i];
      }
      addColumn(new BeeColumn(nm));
    }
  }

  public void addWarning(Object... obj) {
    messages.add(new ResponseMessage(Level.WARNING, obj));
  }

  public void addWarning(Throwable err) {
    messages.add(new ResponseMessage(Level.WARNING, err.toString()));
  }

  public void addWarnings(List<?> lst) {
    for (Object w : lst) {
      addWarning(w);
    }
  }

  public void appendStringProp(String root, Collection<StringProp> lst) {
    Assert.notEmpty(root);
    Assert.notEmpty(lst);

    for (StringProp el : lst) {
      add(root);
      add(el.getName());
      add(el.getValue());
      add(new BeeDate().toLog());
    }
  }

  public void appendSub(Collection<SubProp> lst) {
    Assert.notEmpty(lst);
    for (SubProp el : lst) {
      addPropSub(el);
    }
  }

  public void build(Object... obj) {
    if (obj.length == 0) {
      return;
    }

    for (Object z : obj) {
      add(z);
    }
  }

  public void clearData() {
    setBuffer(new StringBuilder());
    setCount(0);
    setColumnCount(0);
    setDefaultSeparator();
  }

  public StringBuilder getBuffer() {
    return buffer;
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  public int getColumnCount() {
    return columnCount;
  }

  public String getContentType() {
    return contentType;
  }

  public int getCount() {
    return count;
  }

  public BeeService.DATA_TYPE getDataType() {
    return dataType;
  }

  public String getHexSeparator() {
    return Codec.toHex(getSeparator());
  }

  public String getMessage(int i) {
    return messages.get(i).transform();
  }

  public int getMessageCount() {
    return messages.size();
  }

  public List<ResponseMessage> getMessages() {
    return messages;
  }

  public int getPartCount() {
    return parts.size();
  }

  public List<BeeResource> getParts() {
    return parts;
  }

  public char[] getSeparator() {
    return separator;
  }

  public int getSize() {
    return buffer.length();
  }

  public String getString() {
    return buffer.toString();
  }

  public boolean isDefaultSeparator() {
    return (separator != null && separator.length == 1 && separator[0] == BeeService.DEFAULT_INFORMATION_SEPARATOR);
  }

  public String now() {
    return new BeeDate().toLog();
  }

  public void setBuffer(StringBuilder buffer) {
    this.buffer = buffer;
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }

  public void setColumnCount(int columnCount) {
    this.columnCount = columnCount;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setDataType(BeeService.DATA_TYPE dataType) {
    this.dataType = dataType;
  }

  public void setHexSeparator(String sep) {
    if (BeeUtils.isHexString(sep)) {
      this.separator = BeeUtils.fromHex(sep);
    }
  }

  public void setMessages(List<ResponseMessage> messages) {
    this.messages = messages;
  }

  public void setParts(List<BeeResource> parts) {
    this.parts = parts;
  }

  public void setSeparator(char[] separator) {
    this.separator = separator;
  }

  private void checkSeparator(CharSequence s) {
    if (!BeeUtils.contains(s, separator)) {
      return;
    }

    char[] newSep = nextSeparator(s);
    updateSeparator(newSep);
  }

  private char[] nextSeparator(CharSequence s) {
    if (separator == null || separator.length == 0) {
      return null;
    }

    int n = separator.length;
    char[] newSep = new char[n];
    System.arraycopy(separator, 0, newSep, 0, n);

    while (BeeUtils.contains(buffer, newSep) || BeeUtils.contains(s, newSep)) {
      if (newSep[n - 1] < Character.MAX_VALUE) {
        newSep[n - 1]++;
      } else {
        char[] arr = new char[n + 1];
        System.arraycopy(newSep, 0, arr, 0, n);
        arr[n] = Character.MIN_VALUE;
        newSep = arr;
      }
    }

    return newSep;
  }

  private void setDefaultSeparator() {
    separator = new char[]{BeeService.DEFAULT_INFORMATION_SEPARATOR};
  }

  private void updateSeparator(char[] newSep) {
    if (newSep == null || newSep.length == 0) {
      return;
    }

    if (count > 0 && separator != null && separator.length > 0) {
      if (separator.length == 1 && newSep.length == 1) {
        for (int i = 0; i < buffer.length(); i++) {
          if (buffer.charAt(i) == separator[0]) {
            buffer.setCharAt(i, newSep[0]);
          }
        }
      } else {
        String s = buffer.toString().replace(new String(separator),
            new String(newSep));
        setBuffer(new StringBuilder(s));
      }
    }

    setSeparator(newSep);
  }

}
