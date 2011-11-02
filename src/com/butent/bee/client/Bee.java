package com.butent.bee.client;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.RadioGroup;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.utils.XmlUtils;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.InputPassword;
import com.butent.bee.client.widget.InputText;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.List;

/**
 * The entry point class of the application, initializes <code>BeeKeeper</code> class.
 */

public class Bee implements EntryPoint {

  public void onModuleLoad() {
    BeeConst.setClient();

    BeeKeeper bk = new BeeKeeper(RootLayoutPanel.get(),
        GWT.getModuleBaseURL() + GWT.getModuleName());

    bk.init();
    bk.start();

    if (GWT.isProdMode()) {
      GWT.setUncaughtExceptionHandler(new ExceptionHandler());
    }

    bk.register();
    signIn();
  }
  
  private void doSignIn(InputText userBox, InputPassword pswdBox, final Popup popup) {
    String userName = userBox.getValue();
    if (BeeUtils.isEmpty(userName)) {
      Global.showError("Įveskite prisijungimo vardą");
      userBox.setFocus(true);
      return;
    }

    String password = pswdBox.getValue();
    if (BeeUtils.isEmpty(password)) {
      Global.showError("Įveskite slaptažodį");
      pswdBox.setFocus(true);
      return;
    }

    BeeKeeper.getRpc().makePostRequest(Service.LOGIN,
        XmlUtils.createString(Service.XML_TAG_DATA,
            Service.VAR_LOGIN, userName, Service.VAR_PASSWORD, Codec.md5(password)),
        new ResponseCallback() {
          public void onResponse(ResponseObject response) {
            if (response == null) {
              Global.showError("server error");
              return;
            }
            if (!response.hasResponse(UserData.class)) {
              if (response.hasMessages()) {
                List<String> messages = Lists.newArrayList();
                for (ResponseMessage msg : response.getMessages()) {
                  messages.add(BeeUtils.concat(1, msg.getLevel(), msg.getMessage()));
                }
                Global.showError(messages);
              } else {
                Global.showError("Wrong server response");
              }
              return;
            }
            
            popup.hide();
            onSignIn(UserData.restore((String) response.getResponse()));
          }
    });
  }

  private void onSignIn(UserData userData) {
    BeeKeeper.getScreen().start();

    BeeKeeper.getUser().setUserData(userData);
    BeeKeeper.getScreen().updateSignature(userData.getUserSign());

    BeeKeeper.getBus().dispatchService(Service.REFRESH_MENU);
  }
  
  private void signIn() {
    final Popup popup = new Popup(false, true);
    popup.setStyleName("bee-SignIn-Popup");

    Absolute panel = new Absolute(Position.RELATIVE);
    panel.addStyleName("bee-SignIn-Panel");

    BeeLabel caption = new BeeLabel("Būtent CRM");
    caption.addStyleName("bee-SignIn-Caption");
    panel.add(caption);

    BeeLabel userLabel = new BeeLabel("Prisijungimo vardas");
    userLabel.addStyleName("bee-SignIn-Label");
    userLabel.addStyleName("bee-SignIn-User");
    panel.add(userLabel);

    final InputText userBox = new InputText();
    userBox.addStyleName("bee-SignIn-Input");
    userBox.addStyleName("bee-SignIn-User");
    panel.add(userBox);

    BeeLabel pswdLabel = new BeeLabel("Slaptažodis");
    pswdLabel.addStyleName("bee-SignIn-Label");
    pswdLabel.addStyleName("bee-SignIn-Password");
    panel.add(pswdLabel);

    final InputPassword pswdBox = new InputPassword();
    pswdBox.addStyleName("bee-SignIn-Input");
    pswdBox.addStyleName("bee-SignIn-Password");
    panel.add(pswdBox);

    RadioGroup langWidget = new RadioGroup(false, 0,
        Lists.newArrayList("lt", "lv", "et", "en", "de", "ru", "pl"));
    langWidget.addStyleName("bee-SignIn-Language");
    panel.add(langWidget);

    BeeButton button = new BeeButton("Prisijungti");
    button.setStyleName("bee-SignIn-Button");
    panel.add(button);
    
    userBox.addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER
            && !BeeUtils.isEmpty(userBox.getValue())) {
          EventUtils.eatEvent(event);
          pswdBox.setFocus(true);
        }
      }
    });

    pswdBox.addKeyDownHandler(new KeyDownHandler() {
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER
            && !BeeUtils.isEmpty(pswdBox.getValue())) {
          EventUtils.eatEvent(event);
          doSignIn(userBox, pswdBox, popup);
        }
      }
    });
    
    button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doSignIn(userBox, pswdBox, popup);
      }
    });

    popup.setWidget(panel);
    popup.center();

    userBox.setFocus(true);
  }
}
