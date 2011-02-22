package com.butent.bee.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.cli.CliWidget;
import com.butent.bee.client.composite.ButtonGroup;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.composite.TextEditor;
import com.butent.bee.client.composite.ValueSpinner;
import com.butent.bee.client.composite.VolumeSlider;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.grid.CellType;
import com.butent.bee.client.grid.FlexTable;
import com.butent.bee.client.layout.BeeLayoutPanel;
import com.butent.bee.client.layout.BlankTile;
import com.butent.bee.client.layout.Direction;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TilePanel;
import com.butent.bee.client.ui.FormService;
import com.butent.bee.client.ui.GwtUiCreator;
import com.butent.bee.client.ui.MenuService;
import com.butent.bee.client.ui.RowSetService;
import com.butent.bee.client.utils.BeeCommand;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeCheckBox;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.BeeListBox;
import com.butent.bee.client.widget.BeeSimpleCheckBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeResource;
import com.butent.bee.shared.BeeService;
import com.butent.bee.shared.BeeStage;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.ui.UiComponent;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class BeeUi implements Module {

  private class SplitCommand extends BeeCommand {
    Direction direction = null;
    boolean close = false;

    public SplitCommand(Direction direction) {
      super();
      this.direction = direction;
    }

    public SplitCommand(boolean close) {
      super();
      this.close = close;
    }

    @Override
    public void execute() {
      if (close) {
        closePanel();
      } else {
        createPanel(direction);
      }
    }
  }

  private final HasWidgets rootUi;

  private int minTileSize = 20;
  private boolean temporaryDetach = false;

  private Split screenPanel = null;
  private TilePanel activePanel = null;
  private BeeLayoutPanel menuPanel = null;
  private BeeLayoutPanel signature = null;

  private final String elDsn = "el-data-source";
  private final String elGrid = "el-grid-type";
  private final String elCell = "el-cell-type";

  public BeeUi(HasWidgets root) {
    this.rootUi = root;
  }

  public void activatePanel(TilePanel np) {
    Assert.notNull(np);

    TilePanel op = getActivePanel();
    if (op == np) {
      return;
    }

    deactivatePanel();

    if (!isRootTile(np)) {
      Widget w = np.getCenter();

      if (w instanceof BlankTile) {
        w.addStyleName(BeeStyle.ACTIVE_BLANK);
      } else if (w != null) {
        np.getWidgetContainerElement(w).addClassName(BeeStyle.ACTIVE_CONTENT);
      }
    }

    setActivePanel(np);
  }

  public void deactivatePanel() {
    TilePanel op = getActivePanel();

    if (op != null && !isRootTile(op)) {
      Widget w = op.getCenter();

      if (w instanceof BlankTile) {
        w.removeStyleName(BeeStyle.ACTIVE_BLANK);
      } else if (w != null) {
        op.getWidgetContainerElement(w).removeClassName(BeeStyle.ACTIVE_CONTENT);
      }
    }

    setActivePanel(null);
  }

  public void end() {
  }

  public TilePanel getActivePanel() {
    return activePanel;
  }

  public int getActivePanelHeight() {
    TilePanel p = getActivePanel();
    Assert.notNull(p);
    return p.getOffsetHeight();
  }

  public int getActivePanelWidth() {
    TilePanel p = getActivePanel();
    Assert.notNull(p);
    return p.getOffsetWidth();
  }

  public CellType getDefaultCellType() {
    return CellType.get(RadioGroup.getValue(getElCell()));
  }

  public int getDefaultGridType() {
    return RadioGroup.getValue(getElGrid());
  }

  public String getDsn() {
    return ArrayUtils.getQuietly(BeeConst.DS_TYPES, RadioGroup.getValue(getElDsn()));
  }

  public String getElCell() {
    return elCell;
  }

  public String getElDsn() {
    return elDsn;
  }

  public String getElGrid() {
    return elGrid;
  }

  public BeeLayoutPanel getMenuPanel() {
    return menuPanel;
  }

  public int getMinTileSize() {
    return minTileSize;
  }

  public String getName() {
    return getClass().getName();
  }

  public int getPriority(int p) {
    switch (p) {
      case PRIORITY_INIT:
        return DO_NOT_CALL;
      case PRIORITY_START:
        return 10;
      case PRIORITY_END:
        return DO_NOT_CALL;
      default:
        return DO_NOT_CALL;
    }
  }

  public HasWidgets getRootUi() {
    return rootUi;
  }

  public Split getScreenPanel() {
    return screenPanel;
  }

  public void init() {
  }

  public boolean isTemporaryDetach() {
    return temporaryDetach;
  }

  public void setActivePanel(TilePanel p) {
    activePanel = p;
  }

  public void setMenuPanel(BeeLayoutPanel menuPanel) {
    this.menuPanel = menuPanel;
  }

  public void setMinTileSize(int minTileSize) {
    this.minTileSize = minTileSize;
  }

  public void setScreenPanel(Split screenPanel) {
    this.screenPanel = screenPanel;
  }

  public void setTemporaryDetach(boolean temporaryDetach) {
    this.temporaryDetach = temporaryDetach;
  }

  public void showGrid(Object data, String... cols) {
    Assert.notNull(data);
    Widget grd = null;
    boolean addScroll = false;

    switch (getDefaultGridType()) {
      case 1:
        grd = Global.scrollGrid(getActivePanelWidth(), data, cols);
        break;
      case 2:
        grd = Global.cellGrid(data, getDefaultCellType(), cols);
        addScroll = true;
        break;
      default:
        grd = Global.simpleGrid(data, cols);
        addScroll = true;
    }

    updateActiveQuietly(grd, addScroll);
  }

  public void showResource(BeeResource resource) {
    Assert.notNull(resource);
    updateActivePanel(new TextEditor(resource));
  }

  public void start() {
    UiComponent.setUiCreator(new GwtUiCreator());
    createUi();
  }

  public void updateActivePanel(Widget w) {
    updateActivePanel(w, false);
  }

  public void updateActivePanel(Widget w, boolean scroll) {
    Assert.notNull(w);

    TilePanel p = getActivePanel();
    Assert.notNull(p, "panel not available");

    deactivatePanel();
    p.clear();
    p.add(w, scroll);
    activatePanel(p);
  }

  public void updateActiveQuietly(Widget w, boolean scroll) {
    if (w != null) {
      updateActivePanel(w, scroll);
    }
  }

  public void updateMenu(Widget w) {
    Assert.notNull(w);

    BeeLayoutPanel p = getMenuPanel();
    Assert.notNull(p);

    p.clear();
    p.add(w);
  }

  public void updateSignature() {
    signature.clear();
    String usr = BeeKeeper.getUser().getUserSign();

    if (!BeeUtils.isEmpty(usr)) {
      usr = BeeUtils.concat(1, "User:", usr);
    } else {
      usr = "Not logged in";
    }
    signature.add(new BeeLabel(usr));
  }

  private void closePanel() {
    TilePanel op = getActivePanel();
    Assert.notNull(op, "active panel not available");

    if (!(op.getCenter() instanceof BlankTile)) {
      deactivatePanel();
      op.clear();
      op.add(new BlankTile());
      activatePanel(op);
      return;
    }

    if (!(op.getParent() instanceof TilePanel)) {
      return;
    }

    TilePanel parent = (TilePanel) op.getParent();
    TilePanel np = null;

    for (TilePanel w : parent.getPanels()) {
      if (w != op) {
        np = w;
        break;
      }
    }

    Assert.notNull(np, "sibling panel not found");

    deactivatePanel();

    setTemporaryDetach(true);
    np.move(parent);
    setTemporaryDetach(false);

    while (parent.getCenter() instanceof TilePanel) {
      parent = (TilePanel) parent.getCenter();
    }

    activatePanel(parent);
  }

  private void createPanel(Direction direction) {
    TilePanel p = getActivePanel();
    Assert.notNull(p);

    int z = direction.isHorizontal() ? p.getCenterWidth() : p.getCenterHeight();
    z = Math.round((z - p.getSplitterSize()) / 2);
    if (z < getMinTileSize()) {
      Global.showError("no", z);
      return;
    }

    deactivatePanel();

    TilePanel center = new TilePanel();
    Widget w = p.getCenter();
    if (w != null) {
      boolean scroll = p.isWidgetScroll(w);

      setTemporaryDetach(true);
      p.remove(w);
      setTemporaryDetach(false);

      center.add(w, scroll);
      center.onLayout();
    }

    TilePanel tp = new TilePanel();
    BlankTile bt = new BlankTile();
    tp.add(bt);

    p.insert(tp, direction, z, null, false);
    p.add(center);

    activatePanel(tp);
  }

  private void createUi() {
    Widget w;
    Split p = new Split();

    w = initNorth();
    if (w != null) {
      p.addNorth(w, 70);
    }

    w = initSouth();
    if (w != null) {
      p.addSouth(w, 32);
    }

    w = initWest();
    if (w != null) {
      p.addWest(w, 200);
    }

    w = initEast();
    if (w != null) {
      p.addEast(w, 256, true);
    }

    w = initCenter();
    if (w != null) {
      p.add(w, true);
    }

    rootUi.add(p);

    setScreenPanel(p);
  }

  private Widget initCenter() {
    TilePanel p = new TilePanel();
    p.add(new BlankTile());

    setActivePanel(p);
    return p;
  }

  private Widget initEast() {
    return BeeKeeper.getLog().getArea();
  }

  private Widget initNorth() {
    Horizontal p = new Horizontal();
    p.setSpacing(5);

    p.add(new RadioGroup(getElDsn(), BeeKeeper.getStorage().checkInt(getElDsn(), 0),
        BeeConst.DS_TYPES));

    p.add(new ButtonGroup("Ping", BeeService.SERVICE_DB_PING,
        "Info", BeeService.SERVICE_DB_INFO,
        "Tables", BeeService.SERVICE_DB_TABLES));

    p.add(new BeeButton("Class", BeeService.SERVICE_GET_CLASS, BeeStage.STAGE_GET_PARAMETERS));
    p.add(new BeeButton("Xml", BeeService.SERVICE_GET_XML, BeeStage.STAGE_GET_PARAMETERS));
    p.add(new BeeButton("Jdbc", BeeService.SERVICE_GET_DATA, BeeStage.STAGE_GET_PARAMETERS));

    p.add(new BeeButton("Login", BeeService.SERVICE_GET_LOGIN, BeeStage.STAGE_GET_PARAMETERS));
    p.add(new BeeButton("Logout", BeeService.SERVICE_LOGOUT));

    p.add(new BeeCheckBox(Global.getVar(Global.VAR_DEBUG)));

    p.add(new BeeButton("North land", FormService.NAME, FormService.Stages.CHOOSE_FORM.name()));
    p.add(new BeeButton("CRUD", RowSetService.NAME, RowSetService.Stages.CHOOSE_TABLE.name()));

    p.add(new RadioGroup(getElGrid(), true, BeeKeeper.getStorage().checkInt(getElGrid(), 2),
        "simple", "scroll", "cell"));
    p.add(new RadioGroup(getElCell(), true, BeeKeeper.getStorage().checkEnum(getElCell(),
        CellType.TEXT_EDIT), CellType.values()));

    BeeLayoutPanel blp = new BeeLayoutPanel();
    blp.add(p);

    BeeImage bee = new BeeImage(Global.getImages().bee());
    blp.add(bee);

    blp.setWidgetLeftRight(p, 1, Unit.EM, 100, Unit.PX);
    blp.setWidgetTopBottom(p, 4, Unit.PX, 0, Unit.PX);
    blp.setWidgetRightWidth(bee, 10, Unit.PX, 64, Unit.PX);

    return blp;
  }

  private Widget initSouth() {
    BeeLayoutPanel p = new BeeLayoutPanel();

    CliWidget cli = new CliWidget();
    p.add(cli);

    Horizontal hor = new Horizontal();
    hor.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

    hor.add(new BeeButton("N", new SplitCommand(Direction.NORTH)));
    hor.add(new BeeButton("S", new SplitCommand(Direction.SOUTH)));
    hor.add(new BeeButton("E", new SplitCommand(Direction.EAST)));
    hor.add(new BeeButton("W", new SplitCommand(Direction.WEST)));

    BeeImage close = new BeeImage(Global.getImages().close(), new SplitCommand(true));
    hor.add(close);
    hor.setCellWidth(close, "32px");
    hor.setCellHorizontalAlignment(close, HasHorizontalAlignment.ALIGN_RIGHT);

    p.add(hor);

    BeeLabel ver = new BeeLabel("0.2.4");
    p.add(ver);

    p.setWidgetLeftWidth(cli, 1, Unit.EM, 50, Unit.PCT);
    p.setWidgetVerticalPosition(cli, Layout.Alignment.BEGIN);

    p.setWidgetLeftWidth(hor, 60, Unit.PCT, 200, Unit.PX);

    p.setWidgetRightWidth(ver, 1, Unit.EM, 5, Unit.EM);

    signature = new BeeLayoutPanel();
    p.add(signature);
    p.setWidgetLeftRight(signature, 74, Unit.PCT, 6, Unit.EM);
    updateSignature();

    return p;
  }

  private Widget initWest() {
    Split spl = new Split();

    FlexTable fp = new FlexTable();
    fp.setCellSpacing(3);

    int r = MenuConstants.MAX_MENU_DEPTH;
    String name;

    for (int i = MenuConstants.ROOT_MENU_INDEX; i < r; i++) {
      name = MenuConstants.varMenuLayout(i);
      fp.setWidget(i, 0, new BeeListBox(Global.getVar(name)));

      name = MenuConstants.varMenuBarType(i);
      fp.setWidget(i, 1, new BeeSimpleCheckBox(Global.getVar(name)));
    }

    ValueSpinner spinner = new ValueSpinner(Global.getVar(MenuConstants.VAR_ROOT_LIMIT), 0, 30, 3);
    DomUtils.setWidth(spinner, 60);
    fp.setWidget(r, 0, spinner);

    VolumeSlider slider = new VolumeSlider(Global.getVar(MenuConstants.VAR_ITEM_LIMIT), 0, 50, 5);
    slider.setPixelSize(80, 20);
    fp.setWidget(r + 1, 0, slider);

    fp.setWidget(r, 1, new BeeButton("Refresh", BeeService.SERVICE_REFRESH_MENU));
    fp.setWidget(r + 1, 1, new BeeButton("BEE", MenuService.NAME, "stage_dummy"));

    spl.addNorth(fp, 180);

    BeeLayoutPanel mp = new BeeLayoutPanel();
    spl.add(mp);

    setMenuPanel(mp);

    return spl;
  }

  private boolean isRootTile(TilePanel p) {
    if (p == null) {
      return false;
    } else {
      return !(p.getParent() instanceof TilePanel);
    }
  }
}
