package com.butent.bee.client.modules.ec;

import static com.butent.bee.shared.modules.ec.EcConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Settings;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.modules.ec.widget.FeaturedAndNovelty;
import com.butent.bee.client.modules.ec.widget.ItemPanel;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.logging.LogLevel;
import com.butent.bee.shared.modules.ec.EcItemList;

public class EcKeeper {

  public static ParameterList createArgs(String method) {
    ParameterList args = BeeKeeper.getRpc().createParameters(EC_MODULE);
    args.addQueryItem(EC_METHOD, method);
    return args;
  }

  public static void doGlobalSearch(String query) {
    Assert.notEmpty(query);

    ParameterList params = createArgs(SVC_GLOBAL_SEARCH);
    params.addDataItem(VAR_QUERY, query);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        dispatchMessages(response);
        EcItemList items = getResponseItems(response);
        if (items != null) {
          ItemPanel widget = new ItemPanel(items);
          BeeKeeper.getScreen().updateActivePanel(widget);
        }
      }
    });
  }

  public static String getContactsUrl() {
    return Settings.getProperty("ecContacts");
  }

  public static String getTermsUrl() {
    return Settings.getProperty("ecTerms");
  }

  public static void register() {
  }

  public static void searchItems(String service, String query, final Consumer<EcItemList> callback) {
    Assert.notEmpty(service);
    Assert.notEmpty(query);
    Assert.notNull(callback);

    ParameterList params = createArgs(service);
    params.addDataItem(VAR_QUERY, query);

    BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        response.notify(BeeKeeper.getScreen());

        if (!response.hasErrors()) {
          // dispatchMessages(response);
          EcItemList items = getResponseItems(response);
          if (items != null) {
            callback.accept(items);
          }
        }
      }
    });
  }

  public static void showFeaturedAndNoveltyItems() {
    BeeKeeper.getRpc().makeGetRequest(createArgs(SVC_FEATURED_AND_NOVELTY), new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        dispatchMessages(response);
        EcItemList items = getResponseItems(response);
        if (items != null) {
          FeaturedAndNovelty widget = new FeaturedAndNovelty(items);
          BeeKeeper.getScreen().updateActivePanel(widget);
        }
      }
    });
  }

  private static void dispatchMessages(ResponseObject response) {
    if (response != null && response.hasMessages()) {
      for (ResponseMessage message : response.getMessages()) {
        LogLevel level = message.getLevel();

        if (level == LogLevel.ERROR) {
          BeeKeeper.getScreen().notifySevere(message.getMessage());
        } else if (level == LogLevel.WARNING) {
          BeeKeeper.getScreen().notifyWarning(message.getMessage());
        } else {
          BeeKeeper.getScreen().notifyInfo(message.getMessage());
        }
      }
    }
  }

  private static EcItemList getResponseItems(ResponseObject response) {
    if (response != null && response.hasResponse(EcItemList.class)) {
      return EcItemList.restore(response.getResponseAsString());
    }
    return null;
  }

  private EcKeeper() {
  }
}
