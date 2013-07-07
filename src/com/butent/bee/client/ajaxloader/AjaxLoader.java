package com.butent.bee.client.ajaxloader;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.user.client.Window;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Vector;

/**
 * Initializes and sets up functionalities from external APIs.
 */

public final class AjaxLoader {

  /**
   * Sets values for loading options of external APIs.
   */

  public static class AjaxLoaderOptions extends JavaScriptObject {
    public static AjaxLoaderOptions newInstance() {
      return JavaScriptObject.createObject().cast();
    }

    protected AjaxLoaderOptions() {
    }

    public final native void setBaseDomain(String baseDomain) /*-{
      this.base_domain = baseDomain;
    }-*/;

    public final native void setLanguage(String language) /*-{
      this.language = language;
    }-*/;

    public final native void setNoCss(boolean value) /*-{
      this.nocss = value;
    }-*/;

    public final native void setOtherParms(String otherParams) /*-{
      this.other_params = otherParams;
    }-*/;

    public final native void setPackages(JsArrayString packages) /*-{
      this.packages = packages;
    }-*/;

    public final void setPackages(String... packages) {
      setPackages(ArrayHelper.toJsArrayString(packages));
    }

    
    //CHECKSTYLE:OFF
    private native void setCallback(Runnable onLoad) /*-{
      this.callback = function() {
        @com.butent.bee.client.ajaxloader.ExceptionHelper::runProtected(Ljava/lang/Runnable;)(onLoad);
      }
    }-*/;
    //CHECKSTYLE:ON
  }

  private static boolean alreadyInjected;
  private static boolean initialized;
  private static boolean loaded;

  private static Vector<Runnable> queuedApiLoads = new Vector<Runnable>();

  public static ClientLocation getClientLocation() {
    if (!loaded) {
      return null;
    }
    return nativeGetClientLocation();
  }

  public static void init() {
    init(null);
  }

  public static void init(String apiKey) {
    init(apiKey, null);
  }

  public static void init(String apiKey, String hostname) {
    if (initialized) {
      return;
    }
    
    String key;
    if (BeeUtils.isEmpty(apiKey)) {
      key = AjaxKeyRepository.getKey();
    } else {
      key = BeeUtils.trim(apiKey);
    }

    boolean alreadyLoaded = injectJsapi(key, hostname);

    if (alreadyLoaded) {
      loaded = true;
    }
    initialized = true;
  }

  public static void load(Runnable onLoad) {
    Assert.notNull(onLoad);

    if (loaded) {
      onLoad.run();
    } else {
      queuedApiLoads.add(onLoad);
      init();
    }
  }

  public static void loadApi(String api, String version, Runnable onLoad) {
    loadApi(api, version, onLoad, null);
  }

  public static void loadApi(final String api, final String version,
      Runnable onLoad, AjaxLoaderOptions settings) {
    Assert.notNull(onLoad);
    init();

    final AjaxLoaderOptions options = (settings == null) 
        ? AjaxLoaderOptions.newInstance() : settings;
    options.setCallback(onLoad);

    Runnable apiLoad = new Runnable() {
      @Override
      public void run() {
        nativeLoadApi(api, version, options);
      }
    };

    if (loaded) {
      apiLoad.run();
    } else {
      queuedApiLoads.add(apiLoad);
    }
  }

  private static String getProtocol() {
    if ("https:".equals(Window.Location.getProtocol())) {
      return "https:";
    }
    return "http:";
  }

  private static boolean injectJsapi(String apiKey, String hostname) {
    if (alreadyInjected) {
      return true;
    }
    boolean alreadyLoaded = nativeCreateCallback();
    alreadyInjected = true;
    if (alreadyLoaded) {
      return true;
    }
    Document doc = Document.get();
    String key = (apiKey == null) ? "" : ("key=" + apiKey + "&");
    String host = BeeUtils.isEmpty(hostname) ? "www.google.com" : hostname;
    String src = getProtocol() + "//" + host + "/jsapi?" + key + "callback=__gwt_AjaxLoader_onLoad";
    ScriptElement script = doc.createScriptElement();
    script.setSrc(src);
    script.setType("text/javascript");
    doc.getBody().appendChild(script);
    return false;
  }

  private static native boolean nativeCreateCallback() /*-{
    if ($wnd['google'] && $wnd.google['load']) {
      return true;
    }
    $wnd.__gwt_AjaxLoader_onLoad = function() {
      @com.butent.bee.client.ajaxloader.AjaxLoader::onLoadCallback()();
    }
    return false;
  }-*/;

  private static native ClientLocation nativeGetClientLocation() /*-{
    return $wnd.google.loader.ClientLocation;
  }-*/;

  private static native void nativeLoadApi(String api, String version,
      JavaScriptObject settings) /*-{
    $wnd.google.load(api, version, settings);
  }-*/;

  private static void onLoadCallback() {
    loaded = true;

    for (Runnable r : queuedApiLoads) {
      r.run();
    }
    queuedApiLoads.clear();
  }

  private AjaxLoader() {
  }
}
