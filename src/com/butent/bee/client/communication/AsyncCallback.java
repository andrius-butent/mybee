package com.butent.bee.client.communication;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.ResponseData;
import com.butent.bee.client.utils.Duration;
import com.butent.bee.client.utils.JsUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeResource;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.CommUtils;
import com.butent.bee.shared.communication.ContentType;
import com.butent.bee.shared.communication.ResponseMessage;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.UserData;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.List;

/**
 * Manages responses to RPC calls on the client side.
 */

public class AsyncCallback implements RequestCallback {

  @Override
  public void onError(Request req, Throwable ex) {
    String msg = (ex instanceof RequestTimeoutException) ? "request timeout" : "request failure";
    BeeKeeper.getLog().severe(msg, ex);
  }

  @Override
  public void onResponseReceived(Request req, Response resp) {
    int statusCode = resp.getStatusCode();
    boolean debug = Global.isDebug();

    String qid = resp.getHeader(Service.RPC_VAR_QID);

    if (qid == null) {
      BeeKeeper.getBus().removeExitHandler();
      Window.Location.reload();
      return;
    }
    int id = BeeUtils.toInt(qid);
    RpcInfo info = BeeKeeper.getRpc().getRpcInfo(id);
    String svc = (info == null) ? BeeConst.STRING_EMPTY : info.getService();

    String msg;

    if (info == null) {
      BeeKeeper.getLog().warning("Rpc info not available");
    }
    if (BeeUtils.isEmpty(svc)) {
      BeeKeeper.getLog().warning("Rpc service",
          BeeUtils.bracket(Service.RPC_VAR_SVC), "not available");
    }

    if (statusCode != Response.SC_OK) {
      msg = BeeUtils.concat(1, NameUtils.addName(Service.RPC_VAR_QID, id),
          NameUtils.addName(Service.RPC_VAR_SVC, svc));
      if (!BeeUtils.isEmpty(msg)) {
        BeeKeeper.getLog().severe(msg);
      }

      msg = BeeUtils.concat(1, BeeUtils.bracket(statusCode), resp.getStatusText());
      BeeKeeper.getLog().severe("response status", msg);

      if (info != null) {
        info.endError(msg);
      }
      finalizeResponse();
      return;
    }
    String sid = resp.getHeader(Service.RPC_VAR_SID);

    if (!BeeUtils.isEmpty(sid)) {
      BeeKeeper.getUser().setSessionId(sid);

      String usr = resp.getHeader(Service.RPC_VAR_USER);

      if (!BeeUtils.isEmpty(usr)) {
        UserData userData = UserData.restore(Codec.decodeBase64(usr));
        BeeKeeper.getUser().setUserData(userData);
        BeeKeeper.getScreen().updateSignature(userData.getUserSign());
      }
    }

    ContentType ctp = CommUtils.getContentType(resp.getHeader(Service.RPC_VAR_CTP));

    String txt = CommUtils.getContent(ctp, resp.getText());
    int len = txt.length();

    int cnt = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_CNT));
    int cc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_COLS));
    int mc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_MSG_CNT));
    int pc = BeeUtils.toInt(resp.getHeader(Service.RPC_VAR_PART_CNT));

    if (debug) {
      BeeKeeper.getLog().info("response", NameUtils.addName(Service.RPC_VAR_QID, id),
          NameUtils.addName(Service.RPC_VAR_SVC, svc));

      BeeKeeper.getLog().info(NameUtils.addName(Service.RPC_VAR_CTP, ctp),
          NameUtils.addName("len", len), NameUtils.addName(Service.RPC_VAR_CNT, cnt));
      BeeKeeper.getLog().info(NameUtils.addName(Service.RPC_VAR_COLS, cc),
          NameUtils.addName(Service.RPC_VAR_MSG_CNT, mc),
          NameUtils.addName(Service.RPC_VAR_PART_CNT, pc));
    }

    String hSep = resp.getHeader(Service.RPC_VAR_SEP);
    String sep;

    if (BeeUtils.isHexString(hSep)) {
      sep = new String(BeeUtils.fromHex(hSep));
      BeeKeeper.getLog().warning("response separator", BeeUtils.bracket(hSep));
    } else {
      sep = Character.toString(CommUtils.DEFAULT_INFORMATION_SEPARATOR);
      if (!BeeUtils.isEmpty(hSep)) {
        BeeKeeper.getLog().severe("wrong response separator", BeeUtils.bracket(hSep));
      }
    }

    if (debug) {
      Header[] headers = resp.getHeaders();
      for (int i = 0; i < headers.length; i++) {
        if (!BeeUtils.isEmpty(headers[i])) {
          BeeKeeper.getLog().info("Header", i + 1, headers[i].getName(), headers[i].getValue());
        }
      }
      if (info != null) {
        info.setRespInfo(RpcUtils.responseInfo(resp));
      }
    }

    Collection<ResponseMessage> messages = null;
    if (mc > 0) {
      messages = Lists.newArrayList();
      for (int i = 0; i < mc; i++) {
        messages.add(new ResponseMessage(resp.getHeader(CommUtils.rpcMessageName(i)), true));
      }
      RpcUtils.dispatchMessages(messages);
    }

    int[] partSizes = null;
    if (pc > 0) {
      partSizes = new int[pc];
      for (int i = 0; i < pc; i++) {
        partSizes[i] = BeeUtils.toInt(resp.getHeader(CommUtils.rpcPartName(i)));
      }
    }

    if (info != null) {
      info.end(ctp, txt, len, cnt, cc, mc, messages, pc, partSizes);
    }

    Duration duration = new Duration();

    if (!BeeUtils.isEmpty(resp.getHeader(Service.RPC_VAR_RESP))) {
      ResponseObject response = ResponseObject.restore(txt);
      RpcUtils.dispatchMessages(response);
      ResponseCallback callback = null;

      if (info != null) {
        callback = info.getRespCallback();
      }
      if (callback != null) {
        callback.onResponse(response);
      } else {
        BeeKeeper.getLog().warning("No callback available");
      }

    } else if (len == 0) {
      if (mc == 0) {
        BeeKeeper.getLog().warning("response empty");
      }

    } else if (Service.isInvocation(svc)) {
      dispatchInvocation(svc, info, txt, mc, messages, cc, cnt, sep);

    } else if (pc > 0) {
      dispatchParts(svc, pc, partSizes, txt);

    } else if (CommUtils.isResource(ctp)) {
      dispatchResource(txt);

    } else if (txt.indexOf(sep) < 0) {
      BeeKeeper.getLog().info("<", id, "text", txt);

    } else {
      JsArrayString arr = splitResponse(txt, sep, cnt);
      dispatchResponse(svc, cc, arr);
    }
    duration.finish();

    BeeKeeper.getLog().info("<", id, len,
        (info == null) ? BeeConst.STRING_EMPTY : BeeUtils.bracket(info.getCompletedTime()),
        BeeUtils.bracket(duration.getCompletedTime()));
    finalizeResponse();
  }

  private void dispatchInvocation(String svc, RpcInfo info, String txt, int mc,
      Collection<ResponseMessage> messages, int cc, int cnt, String sep) {
    if (info == null) {
      BeeKeeper.getLog().severe("rpc info not available");
      return;
    }

    String method = info.getParameter(Service.RPC_VAR_METH);
    if (BeeUtils.isEmpty(method)) {
      BeeKeeper.getLog().severe("rpc parameter [method] not found");
      return;
    }

    if (BeeUtils.same(method, "stringInfo")) {
      ResponseHandler.unicodeTest(info, txt, messages);
    } else if (cnt > 0) {
      JsArrayString arr = splitResponse(txt, sep, cnt);
      dispatchResponse(svc, cc, arr);
    } else if (mc <= 0) {
      BeeKeeper.getLog().warning("unknown invocation method", method);
    }
  }

  private void dispatchParts(String svc, int pc, int[] sizes, String content) {
    if (BeeUtils.same(svc, Service.GET_XML_INFO)) {
      ResponseHandler.showXmlInfo(pc, sizes, content);
    } else {
      BeeKeeper.getLog().warning("unknown multipart response", svc);
    }
  }

  private void dispatchResource(String src) {
    BeeKeeper.getScreen().showResource(new BeeResource(src));
  }

  private void dispatchResponse(String svc, int cc, JsArrayString arr) {
    if (cc > 0) {
      List<BeeColumn> columns = Lists.newArrayList();
      for (int i = 0; i < cc; i++) {
        columns.add(BeeColumn.restore(arr.get(i)));
      }

      ResponseData table = new ResponseData(arr, columns);
      BeeKeeper.getScreen().showGrid(table);

    } else {
      for (int i = 0; i < arr.length(); i++) {
        if (!BeeUtils.isEmpty(arr.get(i))) {
          BeeKeeper.getLog().info(arr.get(i));
        }
      }

      if (BeeUtils.same(svc, Service.WHERE_AM_I)) {
        BeeKeeper.getLog().info(BeeConst.whereAmI());
      }
    }
  }

  private void finalizeResponse() {
    BeeKeeper.getLog().addSeparator();
  }

  private JsArrayString splitResponse(String txt, String sep, int cnt) {
    JsArrayString arr = JsUtils.split(txt, sep);
    if (cnt > 0 && arr.length() > cnt) {
      arr.setLength(cnt);
    }
    return arr;
  }
}
