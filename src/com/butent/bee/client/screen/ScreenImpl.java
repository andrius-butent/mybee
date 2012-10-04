package com.butent.bee.client.screen;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.Screen;
import com.butent.bee.client.Settings;
import com.butent.bee.client.cli.Shell;
import com.butent.bee.client.composite.ResourceEditor;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.StyleUtils.ScrollBars;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.utils.ServiceCommand;
import com.butent.bee.client.widget.BeeCheckBox;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeResource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

/**
 * Handles default (desktop) screen implementation.
 */

public class ScreenImpl implements Screen {
  
  private LayoutPanel rootPanel;

  private Split screenPanel = null;
  private Workspace workspace = null;

  private HasWidgets commandPanel = null;
  private HasWidgets menuPanel = null;

  private Widget signature = null;

  private BeeCheckBox logToggle = null;
  private final String logVisible = "log-visible";

  private Notification notification = null;
  
  public ScreenImpl() {
  }

  @Override
  public void addCommandItem(Widget widget) {
    Assert.notNull(widget);
    if (getCommandPanel() == null) {
      BeeKeeper.getLog().severe(getName(), "command panel not available");
    } else {
      widget.addStyleName("bee-MainCommandPanelItem");
      getCommandPanel().add(widget);
    }
  }

  @Override
  public void closeWidget(Widget widget) {
    Assert.notNull(widget, "closeWidget: view widget is null");
    getWorkspace().closeWidget(widget);
  }

  @Override
  public void end() {
  }

  @Override
  public int getActivePanelHeight() {
    return getWorkspace().getActivePanel().getOffsetHeight();
  }

  @Override
  public int getActivePanelWidth() {
    return getWorkspace().getActivePanel().getOffsetWidth();
  }

  @Override
  public Widget getActiveWidget() {
    return getWorkspace().getActiveContent();
  }

  @Override
  public HasWidgets getCommandPanel() {
    return commandPanel;
  }

  @Override
  public String getName() {
    return NameUtils.getClassName(getClass());
  }

  @Override
  public int getPriority(int p) {
    switch (p) {
      case PRIORITY_INIT:
        return DO_NOT_CALL;
      case PRIORITY_START:
        return DO_NOT_CALL;
      case PRIORITY_END:
        return DO_NOT_CALL;
      default:
        return DO_NOT_CALL;
    }
  }

  @Override
  public Split getScreenPanel() {
    return screenPanel;
  }

  @Override
  public void init() {
  }

  @Override
  public void notifyInfo(String... messages) {
    if (getNotification() != null) {
      getNotification().info(messages);
    }
  }

  @Override
  public void notifySevere(String... messages) {
    if (getNotification() != null) {
      getNotification().severe(messages);
    }
  }

  @Override
  public void notifyWarning(String... messages) {
    if (getNotification() != null) {
      getNotification().warning(messages);
    }
  }

  @Override
  public void setRootPanel(LayoutPanel rootPanel) {
    this.rootPanel = rootPanel;
  }

  @Override
  public void showGrid(Widget grid) {
    if (grid != null) {
      updateActivePanel(grid, ScrollBars.BOTH);
    }
  }

  @Override
  public void showInfo() {
    getWorkspace().showInfo();
  }

  @Override
  public void showResource(BeeResource resource) {
    Assert.notNull(resource);
    updateActivePanel(new ResourceEditor(resource));
  }
  
  @Override
  public void start() {
    createUi();
  }

  @Override
  public void updateActivePanel(Widget w) {
    updateActivePanel(w, ScrollBars.NONE);
  }

  @Override
  public void updateActivePanel(Widget w, ScrollBars scroll) {
    getWorkspace().updateActivePanel(w, scroll);
  }

  @Override
  public void updateCommandPanel(Widget w) {
    updatePanel(getCommandPanel(), w);
  }

  @Override
  public void updateMenu(Widget w) {
    updatePanel(getMenuPanel(), w);
  }

  @Override
  public void updateSignature(String userSign) {
    if (getSignature() != null) {
      getSignature().getElement().setInnerHTML(userSign);
    }
  }

  protected void createUi() {
    Widget w;
    Split p = new Split(0);
    p.addStyleName("bee-Screen");

    w = initNorth();
    if (w != null) {
      p.addNorth(w, 100);
    }

    w = initSouth();
    if (w != null) {
      p.addSouth(w, 32);
    }

    w = initWest();
    if (w != null) {
      p.addWest(w, 240);
    }

    w = initEast();
    if (w != null) {
      p.addEast(w, 256, ScrollBars.BOTH);
    }

    w = initCenter();
    if (w != null) {
      p.add(w);
    }

    getRootPanel().add(p);
    setScreenPanel(p);

    if (getLogToggle() != null && !getLogToggle().getValue()) {
      BeeKeeper.getLog().hide();
    }
    
    RootPanel.get().add(createLogo());
  }

  protected Notification getNotification() {
    return notification;
  }

  protected LayoutPanel getRootPanel() {
    return rootPanel;
  }

  protected Widget getSignature() {
    return signature;
  }

  protected Widget initCenter() {
    Workspace area = new Workspace();
    setWorkspace(area);
    return area;
  }

  protected Widget initEast() {
    return BeeKeeper.getLog().getArea();
  }

  protected Widget initNorth() {
    Complex panel = new Complex();
    panel.addStyleName("bee-NorthContainer");
    
    panel.add(Global.getSearchWidget());
    
    Flow commandContainer = new Flow();
    commandContainer.addStyleName("bee-MainCommandPanel");
    panel.add(commandContainer);
    setCommandPanel(commandContainer);

    Flow menuContainer = new Flow();
    menuContainer.addStyleName("bee-MainMenu");
    panel.add(menuContainer);
    setMenuPanel(menuContainer);

    Flow userContainer = new Flow();
    userContainer.addStyleName("bee-UserContainer");
    
    BeeLabel user = new BeeLabel();
    user.addStyleName("bee-UserSignature");
    userContainer.add(user);
    setSignature(user);

    Simple exitContainer = new Simple();
    exitContainer.addStyleName("bee-UserExitContainer");
    BeeImage exit = new BeeImage(Global.getImages().exit().getSafeUri(), new Command() {
      @Override
      public void execute() {
        Global.confirm(Global.CONSTANTS.logout(), new ServiceCommand(Service.LOGOUT));
      }
    });
    exit.addStyleName("bee-UserExit");
    exitContainer.setWidget(exit);
    userContainer.add(exitContainer);
    
    panel.add(userContainer);

    Notification nw = new Notification();
    nw.addStyleName("bee-MainNotificationContainer");
    panel.add(nw);
    setNotification(nw);

    return panel;
  }

  protected Widget initSouth() {
    return null;
  }
  
  protected Widget initWest() {
    TabbedPages tp = new TabbedPages();

    tp.add(Global.getFavorites(), new BeeImage(Global.getImages().bookmark()));
    tp.setTabStyle(tp.getPageCount() - 1, "bee-FavoriteTab", true);

    tp.add(Global.getReports(), "Ataskaitos");
    tp.setTabStyle(tp.getPageCount() - 1, "bee-ReportTab", true);

    Flow admPanel = new Flow();
    admPanel.addStyleName("bee-AdminPanel");

    BeeCheckBox log = new BeeCheckBox("Log");
    log.addStyleName("bee-LogToggle");
    log.setValue(BeeKeeper.getStorage().getBoolean(getLogVisible()));

    log.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
          BeeKeeper.getLog().show();
        } else {
          BeeKeeper.getLog().hide();
        }
        BeeKeeper.getStorage().setItem(getLogVisible(), BeeUtils.toString(event.getValue()));
      }
    });
    setLogToggle(log);
    
    admPanel.add(log);

    BeeCheckBox debug = new BeeCheckBox(Global.getVar(Global.VAR_DEBUG));
    debug.addStyleName("bee-DebugToggle");
    admPanel.add(debug);
    
    Shell shell = new Shell();
    shell.addStyleName("bee-AdminShell");
    admPanel.add(shell);

    tp.add(admPanel, "Admin");
    tp.setTabStyle(tp.getPageCount() - 1, "bee-AdminTab", true);

    return tp;
  }

  protected void setMenuPanel(HasWidgets menuPanel) {
    this.menuPanel = menuPanel;
  }

  protected void setNotification(Notification notification) {
    this.notification = notification;
  }

  protected void setScreenPanel(Split screenPanel) {
    this.screenPanel = screenPanel;
  }
  
  protected void setSignature(Widget signature) {
    this.signature = signature;
  }

  private Widget createLogo() {
    BeeImage logo = new BeeImage(Global.getImages().logo2().getSafeUri());
    logo.addStyleName("bee-Logo");

    String ver = Settings.getVersion();
    if (!BeeUtils.isEmpty(ver)) {
      logo.setTitle(ver);
    }
    logo.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Window.open("http://www.butent.com", "", "");
      }
    });

    Simple container = new Simple();
    container.addStyleName("bee-LogoContainer");
    
    container.setWidget(logo);
    return container;
  }

  private BeeCheckBox getLogToggle() {
    return logToggle;
  }

  private String getLogVisible() {
    return logVisible;
  }

  private HasWidgets getMenuPanel() {
    return menuPanel;
  }

  private Workspace getWorkspace() {
    return workspace;
  }

  private void setCommandPanel(HasWidgets commandPanel) {
    this.commandPanel = commandPanel;
  }

  private void setLogToggle(BeeCheckBox logToggle) {
    this.logToggle = logToggle;
  }

  private void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }
  
  private void updatePanel(HasWidgets p, Widget w) {
    if (p == null) {
      notifyWarning("updatePanel: panel is null");
      return;
    }
    if (w == null) {
      notifyWarning("updatePanel: widget is null");
      return;
    }

    p.clear();
    p.add(w);
  }
}
