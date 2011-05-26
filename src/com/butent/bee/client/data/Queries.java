package com.butent.bee.client.data;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JsArrayString;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.communication.RpcParameter;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfoCollection;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;

/**
 * Contains methods for getting {@code RowSets} and making POST requests.
 */

public class Queries {

  private static final int RESPONSE_FROM_CACHE = 0;

  /**
   * Requires implementing classes to have {@code onResponse) method. 
   */
  public interface IntCallback extends Callback<Integer, String> {
  }

  /**
   * Requires implementing classes to have {@code onResponse) method applied for a {@code RowSet}
   * object.
   */
  public interface RowSetCallback extends Callback<BeeRowSet, String> {
  }

  public interface VersionCallback extends Callback<Long, String> {
  }

  public static void deleteRow(String viewName, long rowId) {
    deleteRow(viewName, rowId, null);
  }

  public static void deleteRow(String viewName, long rowId, IntCallback callback) {
    deleteRows(viewName, Lists.newArrayList(rowId), callback);
  }

  public static void deleteRows(String viewName, List<Long> rowIds) {
    deleteRows(viewName, rowIds, null);
  }

  public static void deleteRows(final String viewName, List<Long> rowIds,
      final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(rowIds);

    final int requestCount = rowIds.size();
    Assert.isPositive(requestCount);

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_ROWS, new RowInfoCollection(rowIds).serialize());

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.DELETE_ROWS,
        RpcParameter.SECTION.DATA, lst), new ResponseCallback() {
      public void onResponse(JsArrayString arr) {
        String s = Codec.beeDeserialize(arr.get(0))[0];

        if (BeeUtils.isInt(s)) {
          int responseCount = BeeUtils.toInt(s);
          String message;
          if (responseCount == requestCount) {
            message = BeeUtils.concat(1, viewName, "deleted", responseCount, "rows");
            BeeKeeper.getLog().info(message);
          } else {
            message = BeeUtils.concat(1, viewName, "deleted", responseCount, "rows of",
                requestCount, "requested");
            BeeKeeper.getLog().warning(message);
          }

          if (callback != null) {
            if (responseCount > 0) {
              callback.onSuccess(responseCount);
            } else {
              callback.onFailure(message);
            }
          }

        } else {
          BeeKeeper.getLog().severe(viewName, "delete", requestCount, "rows");
          BeeKeeper.getLog().severe("response:", s);
          if (callback != null) {
            callback.onFailure(s);
          }
        }
      }
    });
  }

  public static void getRowCount(final String viewName, final Filter filter,
      final IntCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName);
    if (filter != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_WHERE, filter.serialize());
    }

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.COUNT_ROWS,
        RpcParameter.SECTION.DATA, lst), new ResponseCallback() {
      public void onResponse(JsArrayString arr) {
        String s = Codec.beeDeserialize(arr.get(0))[0];
        if (BeeUtils.isDigit(s)) {
          int rowCount = BeeUtils.toInt(s);
          BeeKeeper.getLog().info(viewName, filter, "row count:", rowCount);
          callback.onSuccess(rowCount);
        } else {
          String message = BeeUtils.concat(1, viewName, filter, "row count response:", s);
          BeeKeeper.getLog().severe(message);
          callback.onFailure(message);
        }
      }
    });
  }

  public static void getRowCount(String viewName, final IntCallback callback) {
    getRowCount(viewName, null, callback);
  }

  public static int getRowSet(String viewName, Filter filter, Order order,
      CachingPolicy cachingPolicy, RowSetCallback callback) {
    return getRowSet(viewName, filter, order, -1, -1, cachingPolicy, callback);
  }

  public static int getRowSet(String viewName, Filter filter, Order order,
      int offset, int limit, CachingPolicy cachingPolicy, RowSetCallback callback) {
    return getRowSet(viewName, filter, order, offset, limit, null, cachingPolicy, callback);
  }

  public static int getRowSet(String viewName, Filter filter, Order order,
      int offset, int limit, RowSetCallback callback) {
    return getRowSet(viewName, filter, order, offset, limit, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, final Filter filter, final Order order,
      final int offset, final int limit, String states, final CachingPolicy cachingPolicy,
      final RowSetCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notNull(callback);

    if (cachingPolicy != null && cachingPolicy.doRead()) {
      BeeRowSet rowSet = Global.getCache().getRowSet(viewName, filter, order, offset, limit);
      if (rowSet != null) {
        callback.onSuccess(rowSet);
        return RESPONSE_FROM_CACHE;
      }
    }

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName);

    if (filter != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_WHERE, filter.serialize());
    }
    if (order != null) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_ORDER, order.serialize());
    }

    if (offset >= 0 && limit > 0) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_OFFSET, offset,
          Service.VAR_VIEW_LIMIT, limit);
    }

    if (!BeeUtils.isEmpty(states)) {
      PropertyUtils.addProperties(lst, Service.VAR_VIEW_STATES, states);
    }

    return BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.QUERY,
        RpcParameter.SECTION.DATA, lst), new ResponseCallback() {
      public void onResponse(JsArrayString arr) {
        BeeRowSet rs = BeeRowSet.restore(arr.get(0));
        callback.onSuccess(rs);
        if (cachingPolicy != null && cachingPolicy.doWrite()) {
          Global.getCache().add(rs, filter, order, offset, limit);
        }
      }
    });
  }

  public static int getRowSet(String viewName, Filter filter, Order order,
      RowSetCallback callback) {
    return getRowSet(viewName, filter, order, CachingPolicy.NONE, callback);
  }

  public static int getRowSet(String viewName, Filter filter, RowSetCallback callback) {
    return getRowSet(viewName, filter, null, callback);
  }

  public static int getRowSet(String viewName, Order order, RowSetCallback callback) {
    return getRowSet(viewName, null, order, callback);
  }

  public static int getRowSet(String viewName, RowSetCallback callback) {
    return getRowSet(viewName, null, null, callback);
  }

  public static boolean isResponseFromCache(int id) {
    return id == RESPONSE_FROM_CACHE;
  }

  public static void updateCell(final String viewName, final long rowId, long version,
      final String columnId, final String oldValue, final String newValue,
      final VersionCallback callback) {
    Assert.notEmpty(viewName);
    Assert.notEmpty(columnId);

    if (Objects.equal(oldValue, newValue)) {
      BeeKeeper.getLog().warning("updateCell:", viewName, rowId, columnId,
          "value not changed:", newValue);
      return;
    }

    List<Property> lst = PropertyUtils.createProperties(Service.VAR_VIEW_NAME, viewName,
        Service.VAR_VIEW_ROW_ID, rowId, Service.VAR_VIEW_VERSION, version,
        Service.VAR_VIEW_COLUMN, columnId,
        Service.VAR_VIEW_OLD_VALUE, oldValue, Service.VAR_VIEW_NEW_VALUE, newValue);

    BeeKeeper.getRpc().makePostRequest(new ParameterList(Service.UPDATE_CELL,
        RpcParameter.SECTION.DATA, lst), new ResponseCallback() {
      public void onResponse(JsArrayString arr) {
        String s = Codec.beeDeserialize(arr.get(0))[0];

        if (BeeUtils.isLong(s)) {
          long newVersion = BeeUtils.toLong(s);
          if (callback != null) {
            callback.onSuccess(newVersion);
          }

        } else {
          BeeKeeper.getLog().warning("updateCell:", viewName, rowId, columnId,
              "old value:", oldValue, "new value:", newValue);
          BeeKeeper.getLog().warning("response:", s);
          if (callback != null) {
            callback.onFailure(s);
          }
        }
      }
    });
  }

  public static void updateCell(final BeeRowSet rs, final VersionCallback callback) {
    Assert.notNull(rs);

    BeeKeeper.getRpc().sendText(Service.UPDATE_CELL, Codec.beeSerialize(rs),
        new ResponseCallback() {
          public void onResponse(JsArrayString arr) {
            BeeRow s = BeeRow.restore(arr.get(0));

            if (!BeeUtils.isEmpty(s)) {
              long newVersion = s.getVersion();
              if (callback != null) {
                callback.onSuccess(newVersion);
              }

            } else {
              if (callback != null) {
                callback.onFailure("Error");
              }
            }
          }
        });
  }

  private Queries() {
  }
}
