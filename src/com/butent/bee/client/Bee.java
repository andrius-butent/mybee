package com.butent.bee.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import com.butent.bee.client.modules.crm.CrmKeeper;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;

/**
 * The entry point class of the application, initializes <code>BeeKeeper</code> class.
 */

public class Bee implements EntryPoint {

  public void onModuleLoad() {
    BeeConst.setClient();

    BeeKeeper bk =
        new BeeKeeper(RootLayoutPanel.get(), GWT.getModuleBaseURL() + GWT.getModuleName());

    bk.init();
    bk.start();

    if (GWT.isProdMode()) {
      GWT.setUncaughtExceptionHandler(new ExceptionHandler());
    }

    bk.register();
    
    CrmKeeper.register();

    BeeKeeper.getScreen().start();
    BeeKeeper.getBus().dispatchService(Service.REFRESH_MENU);
    BeeKeeper.getBus().registerExitHandler("Don't leave me this way");
  }
}
