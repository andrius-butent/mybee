package com.butent.bee.client.screen;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Bee;
import com.butent.bee.client.Global;
import com.butent.bee.client.Screen;
import com.butent.bee.client.Settings;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Previewer;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.layout.Split;
import com.butent.bee.client.logging.ClientLogManager;
import com.butent.bee.client.modules.commons.PasswordService;
import com.butent.bee.client.render.PhotoRenderer;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.widget.Image;
import com.butent.bee.client.widget.DoubleLabel;
import com.butent.bee.client.widget.InlineLabel;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.Progress;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

/**
 * Handles default (desktop) screen implementation.
 */

public class ScreenImpl implements Screen {

  private static final BeeLogger logger = LogUtils.getLogger(ScreenImpl.class);

  private LayoutPanel rootPanel;
  private Split screenPanel = null;

  private CentralScrutinizer centralScrutinizer = null;

  private Workspace workspace = null;
  private HasWidgets commandPanel = null;

  private HasWidgets menuPanel = null;

  private HasWidgets userPhotoContainer = null;
  private HasText userSignature = null;

  private Notification notification = null;

  private Panel progressPanel = null;

  public ScreenImpl() {
  }

  @Override
  public boolean activateDomainEntry(Domain domain, Long key) {
    return getCentralScrutinizer().activate(domain, key);
  }

  @Override
  public void activateWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "activateWidget: view widget is null");
    getWorkspace().activateWidget(widget);
  }

  @Override
  public void addCommandItem(IdentifiableWidget widget) {
    Assert.notNull(widget);
    if (getCommandPanel() == null) {
      logger.severe(getName(), "command panel not available");
    } else {
      widget.asWidget().addStyleName("bee-MainCommandPanelItem");
      getCommandPanel().add(widget.asWidget());
    }
  }

  @Override
  public void addDomainEntry(Domain domain, IdentifiableWidget widget, Long key, String caption) {
    getCentralScrutinizer().add(domain, widget, key, caption);
  }

  @Override
  public void closeProgress(String id) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item != null) {
        getProgressPanel().remove(item);
        if (!getProgressPanel().iterator().hasNext()) {
          getScreenPanel().setWidgetSize(getProgressPanel(), 0);
        }
      }
    }
  }

  @Override
  public void closeWidget(IdentifiableWidget widget) {
    Assert.notNull(widget, "closeWidget: view widget is null");
    getWorkspace().closeWidget(widget);
  }

  @Override
  public boolean containsDomainEntry(Domain domain, Long key) {
    return getCentralScrutinizer().contains(domain, key);
  }

  @Override
  public String createProgress(String caption, double max) {
    if (getProgressPanel() != null) {
      if (!getProgressPanel().iterator().hasNext()) {
        getScreenPanel().setWidgetSize(getProgressPanel(), 32);
      }

      Flow item = new Flow();
      item.addStyleName("bee-ProgressItem");

      if (!BeeUtils.isEmpty(caption)) {
        InlineLabel label = new InlineLabel(caption.trim());
        item.addStyleName("bee-ProgressCaption");
        item.add(label);
      }

      Progress progress = new Progress(max);
      item.add(progress);

      DoubleLabel percent = new DoubleLabel(Format.getDefaultPercentFormat(), true);
      item.addStyleName("bee-ProgressPercent");
      item.add(percent);

      getProgressPanel().add(item);
      return item.getId();
    } else {
      return null;
    }
  }

  @Override
  public int getActivePanelHeight() {
    return getWorkspace().getActiveTile().getOffsetHeight();
  }

  @Override
  public int getActivePanelWidth() {
    return getWorkspace().getActiveTile().getOffsetWidth();
  }

  @Override
  public IdentifiableWidget getActiveWidget() {
    return getWorkspace().getActiveContent();
  }

  @Override
  public HasWidgets getCommandPanel() {
    return commandPanel;
  }

  @Override
  public int getHeight() {
    return getScreenPanel().getOffsetHeight();
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
        return 0;
      default:
        return DO_NOT_CALL;
    }
  }

  @Override
  public Split getScreenPanel() {
    return screenPanel;
  }

  @Override
  public int getWidth() {
    return getScreenPanel().getOffsetWidth();
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  @Override
  public boolean hasNotifications() {
    return getNotification() != null && getNotification().isActive();
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
  public void onExit() {
    getRootPanel().clear();
  }

  @Override
  public void onLoad() {
    Global.getSearch().focus();
  }

  @Override
  public boolean removeDomainEntry(Domain domain, Long key) {
    return getCentralScrutinizer().remove(domain, key);
  }

  @Override
  public void setRootPanel(LayoutPanel rootPanel) {
    this.rootPanel = rootPanel;
  }

  @Override
  public void showInfo() {
    getWorkspace().showInfo();
  }

  @Override
  public void showWidget(IdentifiableWidget widget, boolean newPlace) {
    if (newPlace) {
      getWorkspace().openInNewPlace(widget);
    } else {
      getWorkspace().updateActivePanel(widget);
    }
  }

  @Override
  public void start() {
    createUi();

    if (getWorkspace() != null) {
      if (getCentralScrutinizer() != null && getWorkspace() != null) {
        getWorkspace().addActiveWidgetChangeHandler(getCentralScrutinizer());
      }

      Previewer.registermouseDownPriorHandler(getWorkspace());
    }
  }

  @Override
  public void updateActivePanel(IdentifiableWidget widget) {
    showWidget(widget, false);
  }

  @Override
  public void updateCommandPanel(IdentifiableWidget widget) {
    updatePanel(getCommandPanel(), widget);
  }

  @Override
  public void updateMenu(IdentifiableWidget widget) {
    updatePanel(getMenuPanel(), widget);
  }

  @Override
  public void updateProgress(String id, double value) {
    if (getProgressPanel() != null && !BeeUtils.isEmpty(id)) {
      Widget item = DomUtils.getChildById(getProgressPanel(), id);

      if (item instanceof HasWidgets) {
        Double max = null;

        for (Widget child : (HasWidgets) item) {
          if (child instanceof Progress) {
            ((Progress) child).setValue(value);
            max = ((Progress) child).getMax();
          }
        }

        if (max != null) {
          for (Widget child : (HasWidgets) item) {
            if (child instanceof DoubleLabel) {
              ((DoubleLabel) child).setValue(value / max);
            }
          }
        }
      }
    }
  }

  @Override
  public void updateUserData(UserData userData) {
    if (userData != null) {
      if (getUserPhotoContainer() != null) {
        getUserPhotoContainer().clear();
      
        String photoFileName = userData.getPhotoFileName();
        if (!BeeUtils.isEmpty(photoFileName)) {
          Image image = new Image(PhotoRenderer.getUrl(photoFileName));
          image.setAlt(userData.getLogin());
          image.addStyleName("bee-UserPhoto");
          
          getUserPhotoContainer().add(image);
        }
      }

      if (getUserSignature() != null) {
        getUserSignature().setText(BeeUtils.trim(userData.getUserSign()));
      }
    }
  }

  protected Widget createLogo(ScheduledCommand command) {
    String imageUrl = Settings.getLogoImage();
    if (BeeUtils.isEmpty(imageUrl)) {
      return null;
    }

    Image widget = new Image(imageUrl);
    widget.setAlt("logo");

    final String title = Settings.getLogoTitle();
    if (!BeeUtils.isEmpty(title)) {
      widget.setTitle(title);
    }

    final String openUrl = Settings.getLogoOpen();
    if (BeeUtils.isEmpty(openUrl)) {
      if (command == null) {
        widget.getElement().getStyle().setCursor(Cursor.DEFAULT);
      } else {
        widget.setCommand(command);
      }

    } else {
      if (BeeUtils.isEmpty(title)) {
        widget.setTitle(openUrl);
      }

      widget.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.open(openUrl, "_blank", null);
        }
      });
    }

    return widget;
  }

  protected void createUi() {
    Split p = new Split(0);
    p.addStyleName(getScreenStyle());

    Pair<? extends IdentifiableWidget, Integer> north = initNorth();
    if (north != null) {
      p.addNorth(north.getA(), north.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> south = initSouth();
    if (south != null) {
      p.addSouth(south.getA(), south.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> west = initWest();
    if (west != null) {
      p.addWest(west.getA(), west.getB());
    }

    Pair<? extends IdentifiableWidget, Integer> east = initEast();
    if (east != null) {
      p.addEast(east.getA(), east.getB());
    }

    IdentifiableWidget center = initCenter();
    if (center != null) {
      p.add(center);
    }

    getRootPanel().add(p);
    setScreenPanel(p);
  }

  protected Widget createUserContainer() {
    Horizontal userContainer = new Horizontal();
    
    if (Settings.showUserPhoto()) {
      Flow photoContainer = new Flow("bee-UserPhotoContainer");
      userContainer.add(photoContainer);
      setUserPhotoContainer(photoContainer);
    }

    Label signature = new Label();
    signature.addStyleName("bee-UserSignature");
    userContainer.add(signature);
    setUserSignature(signature);
    
    signature.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        PasswordService.change();
      }
    });

    Simple exitContainer = new Simple();
    exitContainer.addStyleName("bee-UserExitContainer");

    Image exit = new Image(Global.getImages().exit(), new Command() {
      @Override
      public void execute() {
        Global.getMsgBoxen().confirm(Localized.messages.endSession(Settings.getAppName()),
            Icon.QUESTION, Lists.newArrayList(Localized.constants.questionLogout()),
            new ConfirmationCallback() {
              @Override
              public void onConfirm() {
                Bee.exit();
              }
            }, null, StyleUtils.FontSize.MEDIUM.getClassName(), null);
      }
    });
    exit.addStyleName("bee-UserExit");

    exitContainer.setWidget(exit);
    userContainer.add(exitContainer);

    return userContainer;
  }

  protected Notification getNotification() {
    return notification;
  }

  protected LayoutPanel getRootPanel() {
    return rootPanel;
  }

  protected String getScreenStyle() {
    return "bee-Screen";
  }

  protected IdentifiableWidget initCenter() {
    Workspace area = new Workspace();
    setWorkspace(area);
    return area;
  }
  
  protected Pair<? extends IdentifiableWidget, Integer> initEast() {
    return Pair.of(ClientLogManager.getLogPanel(), ClientLogManager.getInitialPanelSize());
  }

  protected Pair<? extends IdentifiableWidget, Integer> initNorth() {
    Complex panel = new Complex();
    panel.addStyleName("bee-NorthContainer");

    Widget logo = createLogo(null);
    if (logo != null) {
      logo.addStyleName("bee-Logo");
      panel.add(logo);
    }

    panel.add(Global.getSearchWidget());

    Flow commandContainer = new Flow();
    commandContainer.addStyleName("bee-MainCommandPanel");
    panel.add(commandContainer);
    setCommandPanel(commandContainer);

    Flow menuContainer = new Flow();
    menuContainer.addStyleName("bee-MainMenu");
    panel.add(menuContainer);
    setMenuPanel(menuContainer);

    Widget userContainer = createUserContainer();
    userContainer.addStyleName("bee-UserContainer");
    panel.add(userContainer);

    Notification nw = new Notification();
    nw.addStyleName("bee-MainNotificationContainer");
    panel.add(nw);
    setNotification(nw);

    return Pair.of(panel, 100);
  }

  protected Pair<? extends IdentifiableWidget, Integer> initSouth() {
    Flow panel = new Flow();
    panel.addStyleName("bee-ProgressPanel");
    setProgressPanel(panel);

    return Pair.of(panel, 0);
  }

  protected Pair<? extends IdentifiableWidget, Integer> initWest() {
    setCentralScrutinizer(new CentralScrutinizer());
    getCentralScrutinizer().start();

    return Pair.of(getCentralScrutinizer(), 240);
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

  protected void setUserPhotoContainer(HasWidgets userPhotoContainer) {
    this.userPhotoContainer = userPhotoContainer;
  }

  protected void setUserSignature(HasText userSignature) {
    this.userSignature = userSignature;
  }

  private CentralScrutinizer getCentralScrutinizer() {
    return centralScrutinizer;
  }

  private HasWidgets getMenuPanel() {
    return menuPanel;
  }

  private Panel getProgressPanel() {
    return progressPanel;
  }

  private HasWidgets getUserPhotoContainer() {
    return userPhotoContainer;
  }

  private HasText getUserSignature() {
    return userSignature;
  }

  private void setCentralScrutinizer(CentralScrutinizer centralScrutinizer) {
    this.centralScrutinizer = centralScrutinizer;
  }

  private void setCommandPanel(HasWidgets commandPanel) {
    this.commandPanel = commandPanel;
  }

  private void setProgressPanel(Panel progressPanel) {
    this.progressPanel = progressPanel;
  }

  private void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  private void updatePanel(HasWidgets panel, IdentifiableWidget widget) {
    if (panel == null) {
      notifyWarning("updatePanel: panel is null");
      return;
    }
    if (widget == null) {
      notifyWarning("updatePanel: widget is null");
      return;
    }

    panel.clear();
    panel.add(widget.asWidget());
  }
}
