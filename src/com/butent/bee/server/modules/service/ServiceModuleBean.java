package com.butent.bee.server.modules.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.butent.bee.shared.html.builder.Factory.*;
import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.*;
import static com.butent.bee.shared.modules.service.ServiceConstants.COL_COMMENT;
import static com.butent.bee.shared.modules.service.ServiceConstants.COL_EVENT_NOTE;
import static com.butent.bee.shared.modules.service.ServiceConstants.COL_PUBLISH_TIME;
import static com.butent.bee.shared.modules.tasks.TaskConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;

import com.butent.bee.server.data.BeeView;
import com.butent.bee.server.data.DataEvent.ViewQueryEvent;
import com.butent.bee.server.data.DataEventHandler;
import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.data.UserServiceBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.BeeModule;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.administration.ExtensionIcons;
import com.butent.bee.server.modules.mail.MailModuleBean;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.IsQuery;
import com.butent.bee.server.sql.SqlInsert;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.css.CssUnit;
import com.butent.bee.shared.css.values.FontWeight;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SearchResult;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.html.Tags;
import com.butent.bee.shared.html.builder.Document;
import com.butent.bee.shared.html.builder.Element;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.html.builder.elements.Tbody;
import com.butent.bee.shared.i18n.DateTimeFormatInfo.DateTimeFormatInfo;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Formatter;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.BeeParameter;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

@Stateless
public class ServiceModuleBean implements BeeModule {

  private static BeeLogger logger = LogUtils.getLogger(ServiceModuleBean.class);

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  UserServiceBean usr;
  @EJB
  MailModuleBean mail;
  @EJB
  ParamHolderBean prm;

  @Override
  public List<SearchResult> doSearch(String query) {
    List<SearchResult> result = new ArrayList<>();
    Set<String> columns = Sets.newHashSet(ALS_SERVICE_CATEGORY_NAME, COL_SERVICE_ADDRESS,
        ALS_SERVICE_CUSTOMER_NAME, ALS_SERVICE_CONTRACTOR_NAME);

    result.addAll(qs.getSearchResults(VIEW_SERVICE_OBJECTS, Filter.anyContains(columns, query)));

    result.addAll(qs.getSearchResults(VIEW_SERVICE_FILES,
        Filter.anyContains(Sets.newHashSet(AdministrationConstants.COL_FILE_CAPTION,
            AdministrationConstants.ALS_FILE_NAME), query)));

    return result;
  }

  @Override
  public ResponseObject doService(String svc, RequestInfo reqInfo) {
    ResponseObject response;

    if (BeeUtils.same(svc, SVC_CREATE_INVOICE_ITEMS)) {
      response = createInvoiceItems(reqInfo);
    } else if (BeeUtils.same(svc, SVC_CREATE_DEFECT_ITEMS)) {
      response = createDefectItems(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_CALENDAR_DATA)) {
      response = getCalendarData(reqInfo);

    } else if (BeeUtils.same(svc, SVC_COPY_DOCUMENT_CRITERIA)) {
      response = copyDocumentCriteria(reqInfo);

    } else if (BeeUtils.same(svc, SVC_UPDATE_SERVICE_MAINTENANCE_OBJECT)) {
      response = updateServiceMaintenanceObject(reqInfo);

    } else if (BeeUtils.same(svc, SVC_INFORM_CUSTOMER)) {
      response = informCustomer(reqInfo);

    } else if (BeeUtils.same(svc, SVC_GET_MAINTENANCE_NEW_ROW_VALUES)) {
      response = getMaintenanceNewRowValues();

    } else {
      String msg = BeeUtils.joinWords("service not recognized:", svc);
      logger.warning(msg);
      response = ResponseObject.error(msg);
    }

    return response;
  }

  @Override
  public Collection<BeeParameter> getDefaultParameters() {
    String module = getModule().getName();

    List<BeeParameter> params = Lists.newArrayList(
        BeeParameter.createRelation(module, PRM_DEFAULT_MAINTENANCE_TYPE, TBL_MAINTENANCE_TYPES,
            COL_TYPE_NAME),
        BeeParameter.createRelation(module, PRM_DEFAULT_WARRANTY_TYPE, TBL_WARRANTY_TYPES,
            COL_TYPE_NAME),
        BeeParameter.createRelation(module, PRM_MAINTENANCE_SERVICE_GROUP, TBL_ITEM_CATEGORY_TREE,
            COL_SERVICE_CATEGORY_NAME),
        BeeParameter.createNumber(module, PRM_URGENT_RATE),
        BeeParameter.createText(module, PRM_SMS_REQUEST_SERVICE_ADDRESS),
        BeeParameter.createText(module, PRM_SMS_REQUEST_SERVICE_USER_NAME),
        BeeParameter.createText(module, PRM_SMS_REQUEST_SERVICE_PASSWORD),
        BeeParameter.createText(module, PRM_SMS_REQUEST_SERVICE_FROM),
        BeeParameter.createText(module, PRM_EXTERNAL_MAINTENANCE_URL),
        BeeParameter.createText(module, PRM_SMS_REQUEST_CONTACT_INFO_FROM, false, VIEW_DEPARTMENTS)
    );

    return params;
  }

  @Override
  public Module getModule() {
    return Module.SERVICE;
  }

  @Override
  public String getResourcePath() {
    return getModule().getName();
  }

  @Override
  public void init() {
    sys.registerDataEventHandler(new DataEventHandler() {
      @Subscribe
      @AllowConcurrentEvents
      public void setRowProperties(ViewQueryEvent event) {
        if (event.isAfter(VIEW_SERVICE_FILES)) {
          ExtensionIcons.setIcons(event.getRowset(), ALS_FILE_NAME, PROP_ICON);

        } else if (event.isAfter(VIEW_SERVICE_OBJECTS) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();
          List<Long> rowIds = rowSet.getRowIds();

          BeeView view = sys.getView(VIEW_SERVICE_OBJECT_CRITERIA);
          SqlSelect query = view.getQuery(usr.getCurrentUserId());

          query.setWhere(SqlUtils.and(query.getWhere(),
              SqlUtils.isNull(view.getSourceAlias(), COL_SERVICE_CRITERIA_GROUP_NAME),
              SqlUtils.inList(view.getSourceAlias(), COL_SERVICE_OBJECT, rowIds)));

          SimpleRowSet criteria = qs.getData(query);

          if (!DataUtils.isEmpty(criteria)) {
            for (SimpleRow row : criteria) {
              BeeRow r = rowSet.getRowById(row.getLong(COL_SERVICE_OBJECT));

              if (r != null) {
                r.setProperty(COL_SERVICE_CRITERION_NAME
                        + row.getValue(COL_SERVICE_CRITERION_NAME),
                    row.getValue(COL_SERVICE_CRITERION_VALUE));
              }
            }
          }
        } else if (event.isAfter(TBL_SERVICE_MAINTENANCE) && event.hasData()) {
          BeeRowSet rowSet = event.getRowset();

          SimpleRowSet maxStateRs = qs.getData(new SqlSelect()
              .addFields(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE)
              .addMax(TBL_MAINTENANCE_COMMENTS, sys.getIdName(TBL_MAINTENANCE_COMMENTS))
              .addFrom(TBL_MAINTENANCE_COMMENTS)
              .setWhere(SqlUtils.and(
                  SqlUtils.inList(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE,
                      rowSet.getRowIds()),
                  SqlUtils.notNull(TBL_MAINTENANCE_COMMENTS, COL_STATE_COMMENT)))
              .addGroup(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE)
          );

          if (DataUtils.isEmpty(maxStateRs)) {
            return;
          }

          IsCondition latestStateCondition = SqlUtils.inList(TBL_MAINTENANCE_COMMENTS,
              sys.getIdName(TBL_MAINTENANCE_COMMENTS), (Object[]) maxStateRs
                  .getColumn(maxStateRs.getColumnIndex(sys.getIdName(TBL_MAINTENANCE_COMMENTS))));

          SimpleRowSet commentsRowSet = qs.getData(new SqlSelect()
              .addFields(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE, COL_MAINTENANCE_STATE)
              .addFields(TBL_SERVICE_MAINTENANCE, COL_TYPE)
              .addFields(TBL_USER_ROLES, COL_ROLE)
              .addExpr(SqlUtils.nvl(
                  SqlUtils.field(TBL_MAINTENANCE_COMMENTS, COL_TERM),
                  SqlUtils.field(TBL_MAINTENANCE_COMMENTS, COL_PUBLISH_TIME)), ALS_STATE_TIME)
              .addFrom(TBL_MAINTENANCE_COMMENTS)
              .addFromLeft(TBL_SERVICE_MAINTENANCE, sys.joinTables(TBL_SERVICE_MAINTENANCE,
                  TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE))
              .addFromLeft(TBL_USER_ROLES,
                  SqlUtils.join(TBL_USER_ROLES, COL_USER, TBL_MAINTENANCE_COMMENTS, COL_PUBLISHER))
              .setWhere(latestStateCondition));

          Map<Pair<Long, String>, String> commentsMap = new HashMap<>();
          commentsRowSet.forEach(commentRow ->
              commentsMap.put(Pair.of(commentRow.getLong(COL_SERVICE_MAINTENANCE),
                  BeeUtils.join(BeeConst.STRING_SLASH, commentRow.getValue(COL_TYPE),
                  commentRow.getValue(COL_MAINTENANCE_STATE), commentRow.getValue(COL_ROLE))),
                  commentRow.getValue(ALS_STATE_TIME)));

          SqlSelect processQueryAll = new SqlSelect()
              .addFields(TBL_STATE_PROCESS, COL_DAYS_ACTIVE, COL_MAINTENANCE_TYPE,
                  COL_MAINTENANCE_STATE, COL_ROLE)
              .addFrom(TBL_STATE_PROCESS)
              .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_STATE_PROCESS, COL_DAYS_ACTIVE),
                  SqlUtils.isNull(TBL_STATE_PROCESS, COL_FINITE),
                  SqlUtils.inList(TBL_STATE_PROCESS, COL_MAINTENANCE_TYPE,
                      rowSet.getDistinctStrings(rowSet.getColumnIndex(COL_TYPE))),
                  SqlUtils.inList(TBL_STATE_PROCESS, COL_MAINTENANCE_STATE,
                      rowSet.getDistinctStrings(rowSet.getColumnIndex(COL_STATE)))));
          SimpleRowSet processRowSet = qs.getData(processQueryAll);

          Map<String, String> stateProcessMap = Maps.newHashMap();
          processRowSet.forEach(processRow ->
              stateProcessMap.put(BeeUtils.join(BeeConst.STRING_SLASH,
                  processRow.getValue(COL_MAINTENANCE_TYPE),
                  processRow.getValue(COL_MAINTENANCE_STATE),
                  processRow.getValue(COL_ROLE)), processRow.getValue(COL_DAYS_ACTIVE)));

          for (Pair<Long, String> keyPair: commentsMap.keySet()) {

            if (stateProcessMap.containsKey(keyPair.getB())) {
              Long rowIdValue = keyPair.getA();
              String daysActiveValue = stateProcessMap.get(keyPair.getB());
              DateTime stateDate = TimeUtils.toDateTimeOrNull(commentsMap.get(keyPair));

              if (stateDate != null && !BeeUtils.isEmpty(daysActiveValue)
                  && DataUtils.isId(rowIdValue)) {
                BeeRow maintenanceRow = rowSet.getRowById(rowIdValue);
                String oldProperty = maintenanceRow.getProperty(PROP_SERVICE_MAINTENANCE_LATE);
                Integer lateValue = TimeUtils
                    .dateDiff(stateDate, new DateTime()) - BeeUtils.toInt(daysActiveValue);

                if (BeeUtils.isEmpty(oldProperty) || BeeUtils.toInt(oldProperty) < lateValue) {
                  maintenanceRow.setProperty(PROP_SERVICE_MAINTENANCE_LATE, lateValue);
                }
              }
            }
          }
        }
      }
    });
  }

  private ResponseObject copyDocumentCriteria(RequestInfo reqInfo) {
    Long dataId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DOCUMENT_DATA));
    if (!DataUtils.isId(dataId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_DOCUMENT_DATA);
    }

    Long objId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SERVICE_OBJECT));
    if (!DataUtils.isId(objId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_SERVICE_OBJECT);
    }

    if (qs.sqlExists(TBL_SERVICE_CRITERIA_GROUPS, COL_SERVICE_OBJECT, objId)) {
      return ResponseObject.emptyResponse();
    }

    String aliasGroupOrdinal = COL_CRITERIA_GROUP + COL_CRITERIA_ORDINAL;

    SimpleRowSet rs = qs.getData(new SqlSelect()
        .addField(TBL_CRITERIA_GROUPS, COL_CRITERIA_ORDINAL, aliasGroupOrdinal)
        .addFields(TBL_CRITERIA_GROUPS, COL_CRITERIA_GROUP_NAME)
        .addFields(TBL_CRITERIA, COL_CRITERIA_GROUP, COL_CRITERIA_ORDINAL, COL_CRITERION_NAME,
            COL_CRITERION_VALUE)
        .addFrom(TBL_CRITERIA_GROUPS)
        .addFromLeft(TBL_CRITERIA,
            sys.joinTables(TBL_CRITERIA_GROUPS, TBL_CRITERIA, COL_CRITERIA_GROUP))
        .setWhere(SqlUtils.equals(TBL_CRITERIA_GROUPS, COL_DOCUMENT_DATA, dataId)));

    if (DataUtils.isEmpty(rs)) {
      return ResponseObject.emptyResponse();
    }

    Map<Long, Long> groups = new HashMap<>();
    Long svcGroupId;

    for (SimpleRow row : rs) {
      Long docGroupId = row.getLong(COL_CRITERIA_GROUP);

      if (groups.containsKey(docGroupId)) {
        svcGroupId = groups.get(docGroupId);

      } else {
        SqlInsert insGroup = new SqlInsert(TBL_SERVICE_CRITERIA_GROUPS)
            .addConstant(COL_SERVICE_OBJECT, objId);

        Integer groupOrdinal = row.getInt(aliasGroupOrdinal);
        if (groupOrdinal != null) {
          insGroup.addConstant(COL_SERVICE_CRITERIA_ORDINAL, groupOrdinal);
        }

        String groupName = row.getValue(COL_CRITERIA_GROUP_NAME);
        if (!BeeUtils.isEmpty(groupName)) {
          insGroup.addConstant(COL_SERVICE_CRITERIA_GROUP_NAME, groupName);
        }

        svcGroupId = qs.insertData(insGroup);
        groups.put(docGroupId, svcGroupId);
      }

      String criterion = row.getValue(COL_CRITERION_NAME);

      if (DataUtils.isId(svcGroupId) && !BeeUtils.isEmpty(criterion)) {
        SqlInsert insCrit = new SqlInsert(TBL_SERVICE_CRITERIA)
            .addConstant(COL_SERVICE_CRITERIA_GROUP, svcGroupId)
            .addConstant(COL_SERVICE_CRITERION_NAME, criterion);

        Integer ordinal = row.getInt(COL_CRITERIA_ORDINAL);
        if (ordinal != null) {
          insCrit.addConstant(COL_SERVICE_CRITERIA_ORDINAL, ordinal);
        }

        String value = row.getValue(COL_CRITERION_VALUE);
        if (!BeeUtils.isEmpty(value)) {
          insCrit.addConstant(COL_SERVICE_CRITERION_VALUE, value);
        }

        qs.insertData(insCrit);
      }
    }

    return ResponseObject.response(rs.getNumberOfRows());
  }

  private ResponseObject createDefectItems(RequestInfo reqInfo) {
    Long dfId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_DEFECT));
    if (!DataUtils.isId(dfId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_DEFECT);
    }

    Long currency = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
    if (!DataUtils.isId(currency)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CURRENCY);
    }

    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(VIEW_MAINTENANCE));
    if (ids.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_MAINTENANCE);
    }

    IsCondition where = sys.idInList(TBL_MAINTENANCE, ids);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_MAINTENANCE, COL_MAINTENANCE_ITEM, COL_TRADE_ITEM_QUANTITY,
            COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC, COL_MAINTENANCE_NOTES)
        .addFrom(TBL_MAINTENANCE)
        .addFields(TBL_ITEMS, COL_ITEM_ARTICLE)
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_MAINTENANCE, COL_MAINTENANCE_ITEM))
        .setWhere(where);

    IsExpression priceExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_ITEM_PRICE, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);
    String priceAlias = "Price_" + SqlUtils.uniqueName();

    IsExpression vatExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_VAT, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);
    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    query.addExpr(priceExch, priceAlias)
        .addExpr(vatExch, vatAlias)
        .addOrder(TBL_MAINTENANCE, sys.getIdName(TBL_MAINTENANCE));

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_MAINTENANCE, ids, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = row.getLong(COL_MAINTENANCE_ITEM);
      String article = row.getValue(COL_ITEM_ARTICLE);

      SqlInsert insert = new SqlInsert(TBL_SERVICE_DEFECT_ITEMS)
          .addConstant(COL_DEFECT, dfId)
          .addConstant(COL_DEFECT_ITEM, item)
          .addConstant(COL_ITEM_ARTICLE, article);

      Boolean vatPerc = row.getBoolean(COL_TRADE_VAT_PERC);
      Double vat;

      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(COL_TRADE_VAT);
      } else {
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(COL_TRADE_VAT_PLUS);
      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
      Double price = row.getDouble(priceAlias);

      insert.addConstant(COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));
      if (price != null) {
        insert.addConstant(COL_TRADE_ITEM_PRICE, price);
      }

      if (data.hasColumn(COL_MAINTENANCE_NOTES)) {
        String notes = row.getValue(COL_MAINTENANCE_NOTES);
        if (!BeeUtils.isEmpty(notes)) {
          insert.addConstant(COL_DEFECT_NOTE, notes);
        }
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }
    }

    if (!response.hasErrors()) {
      SqlUpdate update = new SqlUpdate(TBL_MAINTENANCE)
          .addConstant(COL_MAINTENANCE_DEFECT, dfId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;
  }

  private ResponseObject createInvoiceItems(RequestInfo reqInfo) {
    Long invId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_MAINTENANCE_INVOICE));
    if (!DataUtils.isId(invId)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_MAINTENANCE_INVOICE);
    }

    Long currency =
        BeeUtils.toLongOrNull(reqInfo.getParameter(COL_CURRENCY));
    if (!DataUtils.isId(currency)) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), COL_CURRENCY);
    }

    Set<Long> ids = DataUtils.parseIdSet(reqInfo.getParameter(VIEW_MAINTENANCE));
    if (ids.isEmpty()) {
      return ResponseObject.parameterNotFound(reqInfo.getService(), VIEW_MAINTENANCE);
    }

    Long mainItem = BeeUtils.toLongOrNull(reqInfo.getParameter(PROP_MAIN_ITEM));

    IsCondition where = sys.idInList(TBL_MAINTENANCE, ids);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_MAINTENANCE, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)
        .addFields(TBL_ITEMS, COL_ITEM_ARTICLE)
        .addFrom(TBL_MAINTENANCE)
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_MAINTENANCE, COL_MAINTENANCE_ITEM))
        .setWhere(where);

    IsExpression vatExch = ExchangeUtils.exchangeFieldTo(query,
        TBL_MAINTENANCE, COL_TRADE_VAT, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);

    String vatAlias = "Vat_" + SqlUtils.uniqueName();

    String priceAlias;
    String amountAlias;

    if (DataUtils.isId(mainItem) && ids.size() > 1) {
      IsExpression amountExch = ExchangeUtils.exchangeFieldTo(query,
          TradeModuleBean.getTotalExpression(TBL_MAINTENANCE),
          SqlUtils.field(TBL_MAINTENANCE, COL_CURRENCY),
          SqlUtils.field(TBL_MAINTENANCE, COL_MAINTENANCE_DATE),
          SqlUtils.constant(currency));

      priceAlias = null;
      amountAlias = "Amount_" + SqlUtils.uniqueName();

      query.addSum(TBL_MAINTENANCE, COL_TRADE_ITEM_QUANTITY)
          .addSum(amountExch, amountAlias)
          .addSum(vatExch, vatAlias)
          .addGroup(TBL_MAINTENANCE, COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC);

    } else {
      IsExpression priceExch = ExchangeUtils.exchangeFieldTo(query,
          TBL_MAINTENANCE, COL_TRADE_ITEM_PRICE, COL_CURRENCY, COL_MAINTENANCE_DATE, currency);

      priceAlias = "Price_" + SqlUtils.uniqueName();
      amountAlias = null;

      query.addFields(TBL_MAINTENANCE, COL_MAINTENANCE_ITEM, COL_TRADE_ITEM_QUANTITY,
          COL_MAINTENANCE_NOTES)
          .addExpr(priceExch, priceAlias)
          .addExpr(vatExch, vatAlias)
          .addOrder(TBL_MAINTENANCE, sys.getIdName(TBL_MAINTENANCE));
    }

    SimpleRowSet data = qs.getData(query);
    if (DataUtils.isEmpty(data)) {
      return ResponseObject.error(TBL_MAINTENANCE, ids, "not found");
    }

    ResponseObject response = new ResponseObject();

    for (SimpleRow row : data) {
      Long item = DataUtils.isId(mainItem) ? mainItem : row.getLong(COL_MAINTENANCE_ITEM);
      String article = row.getValue(COL_ITEM_ARTICLE);

      SqlInsert insert = new SqlInsert(TBL_SALE_ITEMS)
          .addConstant(COL_SALE, invId)
          .addConstant(COL_ITEM, item)
          .addConstant(COL_ITEM_ARTICLE, article);

      Boolean vatPerc = row.getBoolean(COL_TRADE_VAT_PERC);
      Double vat;

      if (BeeUtils.isTrue(vatPerc)) {
        insert.addConstant(COL_TRADE_VAT_PERC, vatPerc);
        vat = row.getDouble(COL_TRADE_VAT);
      } else {
        vat = row.getDouble(vatAlias);
      }

      if (BeeUtils.nonZero(vat)) {
        insert.addConstant(COL_TRADE_VAT, vat);
      }

      Boolean vatPlus = row.getBoolean(COL_TRADE_VAT_PLUS);
      if (BeeUtils.isTrue(vatPlus)) {
        insert.addConstant(COL_TRADE_VAT_PLUS, vatPlus);
      }

      Double quantity = row.getDouble(COL_TRADE_ITEM_QUANTITY);
      Double price;

      if (BeeUtils.isEmpty(amountAlias)) {
        price = row.getDouble(priceAlias);

      } else {
        Double amount = row.getDouble(amountAlias);
        if (BeeUtils.isTrue(vatPlus) && BeeUtils.nonZero(vat)) {
          if (BeeUtils.isTrue(vatPerc)) {
            if (BeeUtils.nonZero(amount)) {
              amount = amount * 100d / (100d + vat);
            }
          } else {
            amount = BeeUtils.unbox(amount) - vat;
          }
        }

        if (BeeUtils.isPositive(amount) && BeeUtils.isPositive(quantity)) {
          price = amount / quantity;
        } else {
          quantity = BeeConst.DOUBLE_ONE;
          price = amount;
        }
      }

      insert.addConstant(COL_TRADE_ITEM_QUANTITY, BeeUtils.unbox(quantity));
      if (price != null) {
        insert.addConstant(COL_TRADE_ITEM_PRICE, price);
      }

      if (data.hasColumn(COL_MAINTENANCE_NOTES)) {
        String notes = row.getValue(COL_MAINTENANCE_NOTES);
        if (!BeeUtils.isEmpty(notes)) {
          insert.addConstant(COL_TRADE_ITEM_NOTE, notes);
        }
      }

      ResponseObject insResponse = qs.insertDataWithResponse(insert);
      if (insResponse.hasErrors()) {
        response.addMessagesFrom(insResponse);
        break;
      }
    }

    if (!response.hasErrors()) {
      SqlUpdate update = new SqlUpdate(TBL_MAINTENANCE)
          .addConstant(COL_MAINTENANCE_INVOICE, invId)
          .setWhere(where);

      ResponseObject updResponse = qs.updateDataWithResponse(update);
      if (updResponse.hasErrors()) {
        response.addMessagesFrom(updResponse);
      }
    }

    return response;
  }

  private ResponseObject getCalendarData(RequestInfo reqInfo) {
    BeeRowSet settings = getSettings();
    if (DataUtils.isEmpty(settings)) {
      return ResponseObject.error(reqInfo.getService(), "user settings not available");
    }

    SimpleRowSet objectData = getCalendarObjects();
    if (DataUtils.isEmpty(objectData)) {
      return ResponseObject.response(settings);
    }

    settings.setTableProperty(TBL_SERVICE_OBJECTS, objectData.serialize());

    BeeRow row = settings.getRow(0);
    JustDate minDate = DataUtils.getDate(settings, row, COL_SERVICE_CALENDAR_MIN_DATE);
    JustDate maxDate = DataUtils.getDate(settings, row, COL_SERVICE_CALENDAR_MAX_DATE);

    if (minDate != null && maxDate != null && BeeUtils.isLess(maxDate, minDate)) {
      maxDate = JustDate.copyOf(minDate);
    }

    Long minTime = (minDate == null) ? null : TimeUtils.startOfDay(minDate, -1).getTime();
    Long maxTime = (maxDate == null) ? null : TimeUtils.startOfDay(maxDate, 1).getTime();

    SimpleRowSet datesData = getCalendarDates(minTime, maxTime);
    if (!DataUtils.isEmpty(datesData)) {
      settings.setTableProperty(TBL_SERVICE_DATES, datesData.serialize());
    }

    Set<Long> taskTypes = DataUtils.parseIdSet(
        settings.getString(0, COL_SERVICE_CALENDAR_TASK_TYPES));

    SimpleRowSet taskData = getCalendarTasks(taskTypes, minTime, maxTime);
    if (!DataUtils.isEmpty(taskData)) {
      settings.setTableProperty(TBL_TASKS, taskData.serialize());
    }

    SimpleRowSet rtData = getCalendarRecurringTasks(taskTypes, minTime, maxTime);
    if (!DataUtils.isEmpty(rtData)) {
      settings.setTableProperty(TBL_RECURRING_TASKS, rtData.serialize());

      BeeRowSet rtDates = qs.getViewData(VIEW_RT_DATES);
      if (!DataUtils.isEmpty(rtDates)) {
        settings.setTableProperty(VIEW_RT_DATES, rtDates.serialize());
      }
    }

    return ResponseObject.response(settings);
  }

  private SimpleRowSet getCalendarDates(Long minTime, Long maxTime) {
    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_SERVICE_DATES)
        .addFrom(TBL_SERVICE_DATES)
        .addOrder(TBL_SERVICE_DATES, COL_SERVICE_OBJECT, COL_SERVICE_DATE_FROM);

    if (minTime != null || maxTime != null) {
      HasConditions where = SqlUtils.and();

      if (minTime != null) {
        where.add(SqlUtils.or(SqlUtils.isNull(TBL_SERVICE_DATES, COL_SERVICE_DATE_UNTIL),
            SqlUtils.moreEqual(TBL_SERVICE_DATES, COL_SERVICE_DATE_UNTIL, minTime)));
      }
      if (maxTime != null) {
        where.add(SqlUtils.lessEqual(TBL_SERVICE_DATES, COL_SERVICE_DATE_FROM, maxTime));
      }

      query.setWhere(where);
    }

    return qs.getData(query);
  }

  private Multimap<Long, Property> getCalendarObjectCriteria(SimpleRowSet objects) {
    Multimap<Long, Property> criteria = ArrayListMultimap.create();

    BeeRowSet data = qs.getViewData(VIEW_SERVICE_OBJECT_CRITERIA,
        Filter.isNull(COL_SERVICE_CRITERIA_GROUP_NAME));
    if (DataUtils.isEmpty(data)) {
      return criteria;
    }

    Long[] objIds = objects.getLongColumn(sys.getIdName(TBL_SERVICE_OBJECTS));

    int objIndex = data.getColumnIndex(COL_SERVICE_OBJECT);
    int nameIndex = data.getColumnIndex(COL_SERVICE_CRITERION_NAME);
    int valueIndex = data.getColumnIndex(COL_SERVICE_CRITERION_VALUE);

    for (BeeRow row : data) {
      Long objId = row.getLong(objIndex);

      if (ArrayUtils.contains(objIds, objId)) {
        criteria.put(objId, new Property(row.getString(nameIndex), row.getString(valueIndex)));
      }
    }

    return criteria;
  }

  private SimpleRowSet getCalendarObjects() {
    String idName = sys.getIdName(TBL_SERVICE_OBJECTS);

    HasConditions where = SqlUtils.or(
        SqlUtils.in(TBL_SERVICE_OBJECTS, idName, TBL_RELATIONS, COL_SERVICE_OBJECT,
            SqlUtils.or(SqlUtils.notNull(TBL_RELATIONS, COL_TASK),
                SqlUtils.notNull(TBL_RELATIONS, COL_RECURRING_TASK))),
        SqlUtils.in(TBL_SERVICE_OBJECTS, idName, TBL_SERVICE_DATES, COL_SERVICE_OBJECT));

    String aliasCustomers = "Cust_" + SqlUtils.uniqueName();
    String aliasContractors = "Contr_" + SqlUtils.uniqueName();

    String companyIdName = sys.getIdName(TBL_COMPANIES);

    SqlSelect query = new SqlSelect()
        .addFields(TBL_SERVICE_OBJECTS, idName, COL_SERVICE_CATEGORY,
            COL_SERVICE_CUSTOMER, COL_SERVICE_CONTRACTOR, COL_SERVICE_ADDRESS)
        .addField(TBL_SERVICE_TREE, COL_SERVICE_CATEGORY_NAME, ALS_SERVICE_CATEGORY_NAME)
        .addField(aliasCustomers, COL_COMPANY_NAME, ALS_SERVICE_CUSTOMER_NAME)
        .addField(aliasContractors, COL_COMPANY_NAME, ALS_SERVICE_CONTRACTOR_NAME)
        .addConstant(BeeConst.STRING_SPACE, PROP_CRITERIA)
        .addFrom(TBL_SERVICE_OBJECTS)
        .addFromLeft(TBL_SERVICE_TREE, sys.joinTables(TBL_SERVICE_TREE,
            TBL_SERVICE_OBJECTS, COL_SERVICE_CATEGORY))
        .addFromLeft(TBL_COMPANIES, aliasCustomers,
            SqlUtils.join(aliasCustomers, companyIdName,
                TBL_SERVICE_OBJECTS, COL_SERVICE_CUSTOMER))
        .addFromLeft(TBL_COMPANIES, aliasContractors,
            SqlUtils.join(aliasContractors, companyIdName,
                TBL_SERVICE_OBJECTS, COL_SERVICE_CONTRACTOR))
        .setWhere(where)
        .addOrder(TBL_SERVICE_OBJECTS, COL_SERVICE_ADDRESS, idName);

    SimpleRowSet data = qs.getData(query);

    if (!DataUtils.isEmpty(data)) {
      Multimap<Long, Property> criteria = getCalendarObjectCriteria(data);

      if (!criteria.isEmpty()) {
        for (SimpleRow row : data) {
          Long objId = row.getLong(idName);

          if (criteria.containsKey(objId)) {
            row.setValue(PROP_CRITERIA, Codec.beeSerialize(criteria.get(objId)));
          }
        }
      }
    }

    return data;
  }

  private SimpleRowSet getCalendarRecurringTasks(Set<Long> taskTypes, Long minTime, Long maxTime) {
    String idName = sys.getIdName(TBL_RECURRING_TASKS);

    SqlSelect spawnQuery = new SqlSelect()
        .addFields(TBL_TASKS, COL_RECURRING_TASK)
        .addMax(TBL_TASKS, COL_START_TIME, ALS_LAST_SPAWN)
        .addFrom(TBL_TASKS)
        .setWhere(SqlUtils.notNull(TBL_TASKS, COL_RECURRING_TASK))
        .addGroup(TBL_TASKS, COL_RECURRING_TASK);

    String spawnAlias = "Spawn_" + SqlUtils.uniqueName();

    HasConditions where = SqlUtils.and(SqlUtils.in(TBL_RECURRING_TASKS, idName,
        TBL_RELATIONS, COL_RECURRING_TASK,
        SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT)));

    if (!taskTypes.isEmpty()) {
      where.add(SqlUtils.inList(TBL_RECURRING_TASKS, COL_TASK_TYPE, taskTypes));
    }

    if (minTime != null) {
      where.add(SqlUtils.or(SqlUtils.isNull(TBL_RECURRING_TASKS, COL_RT_SCHEDULE_UNTIL),
          SqlUtils.moreEqual(TBL_RECURRING_TASKS, COL_RT_SCHEDULE_UNTIL, minTime)));
    }
    if (maxTime != null) {
      where.add(SqlUtils.lessEqual(TBL_RECURRING_TASKS, COL_RT_SCHEDULE_FROM, maxTime));
    }

    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_RECURRING_TASKS)
        .addField(TBL_TASK_TYPES, COL_TASK_TYPE_NAME, ALS_TASK_TYPE_NAME)
        .addField(TBL_TASK_TYPES, COL_BACKGROUND, ALS_TASK_TYPE_BACKGROUND)
        .addField(TBL_TASK_TYPES, COL_FOREGROUND, ALS_TASK_TYPE_FOREGROUND)
        .addFields(spawnAlias, ALS_LAST_SPAWN)
        .addEmptyText(COL_RELATION)
        .addFrom(TBL_RECURRING_TASKS)
        .addFromLeft(TBL_TASK_TYPES,
            sys.joinTables(TBL_TASK_TYPES, TBL_RECURRING_TASKS, COL_TASK_TYPE))
        .addFromLeft(spawnQuery, spawnAlias,
            SqlUtils.join(spawnAlias, COL_RECURRING_TASK, TBL_RECURRING_TASKS, idName))
        .setWhere(where)
        .addOrder(TBL_RECURRING_TASKS, COL_RT_SCHEDULE_FROM, idName);

    SimpleRowSet rs = qs.getData(query);
    List<Long> taskIds = DataUtils.parseIdList(
        BeeUtils.joinItems(
            Lists.newArrayList(rs.getColumn(sys.getIdName(TBL_RECURRING_TASKS)))));

    Multimap<Long, Long> relMap =
        getCalendarRelations(COL_RECURRING_TASK, taskIds);

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      String idData =
          DataUtils.buildIdList(relMap.get(rs.getLong(i, sys.getIdName(TBL_RECURRING_TASKS))));
      rs.setValue(i, COL_RELATION, idData);
    }

    return rs;
  }

  private Multimap<Long, Long> getCalendarRelations(String field, List<Long> ids) {
    SqlSelect query = new SqlSelect()
        .addFields(TBL_RELATIONS, COL_SERVICE_OBJECT, COL_TASK, COL_RECURRING_TASK)
        .addFrom(TBL_RELATIONS)
        .setWhere(SqlUtils.and(
            SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT),
            SqlUtils.notNull(TBL_RELATIONS, field),
            SqlUtils.inList(TBL_RELATIONS, field, ids)));

    SimpleRowSet rs = qs.getData(query);

    Multimap<Long, Long> relMap = ArrayListMultimap.create();

    if (rs.isEmpty()) {
      return relMap;
    }

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      Long fieldId = rs.getLong(i, field);
      Long objectId = rs.getLong(i, COL_SERVICE_OBJECT);
      relMap.put(fieldId, objectId);
    }

    return relMap;
  }

  private SimpleRowSet getCalendarTasks(Set<Long> taskTypes, Long minTime, Long maxTime) {
    String idName = sys.getIdName(TBL_TASKS);

    HasConditions where = SqlUtils.and(SqlUtils.in(TBL_TASKS, idName, TBL_RELATIONS, COL_TASK,
        SqlUtils.notNull(TBL_RELATIONS, COL_SERVICE_OBJECT)));

    if (!taskTypes.isEmpty()) {
      where.add(SqlUtils.inList(TBL_TASKS, COL_TASK_TYPE, taskTypes));
    }

    if (minTime != null) {
      where.add(SqlUtils.moreEqual(TBL_TASKS, COL_FINISH_TIME, minTime));
    }
    if (maxTime != null) {
      where.add(SqlUtils.lessEqual(TBL_TASKS, COL_START_TIME, maxTime));
    }

    SqlSelect query = new SqlSelect()
        .addAllFields(TBL_TASKS)
        .addField(TBL_TASK_TYPES, COL_TASK_TYPE_NAME, ALS_TASK_TYPE_NAME)
        .addField(TBL_TASK_TYPES, COL_BACKGROUND, ALS_TASK_TYPE_BACKGROUND)
        .addField(TBL_TASK_TYPES, COL_FOREGROUND, ALS_TASK_TYPE_FOREGROUND)
        .addFields(TBL_TASK_USERS, COL_STAR)
        .addEmptyText(COL_RELATION)
        .addFrom(TBL_TASKS)
        .addFromLeft(TBL_TASK_TYPES,
            sys.joinTables(TBL_TASK_TYPES, TBL_TASKS, COL_TASK_TYPE))
        .addFromLeft(TBL_TASK_USERS,
            SqlUtils.and(
                SqlUtils.join(TBL_TASKS, idName, TBL_TASK_USERS, COL_TASK),
                SqlUtils.equals(TBL_TASK_USERS, COL_USER, usr.getCurrentUserId())))
        .setWhere(where)
        .addOrder(TBL_TASKS, COL_FINISH_TIME, idName);

    SimpleRowSet rs = qs.getData(query);
    List<Long> taskIds = DataUtils.parseIdList(
        BeeUtils.joinItems(
            Lists.newArrayList(rs.getColumn(sys.getIdName(TBL_TASKS)))));

    Multimap<Long, Long> relMap =
        getCalendarRelations(COL_TASK, taskIds);

    for (int i = 0; i < rs.getNumberOfRows(); i++) {
      String idData = DataUtils.buildIdList(relMap.get(rs.getLong(i, sys.getIdName(TBL_TASKS))));
      rs.setValue(i, COL_RELATION, idData);
    }

    return rs;
  }

  private ResponseObject getMaintenanceNewRowValues() {
    Map<String, String> columnValues = new HashMap<>();

    Pair<Long, String> typeInfo = prm.getRelationInfo(PRM_DEFAULT_MAINTENANCE_TYPE);
    Long typeId = typeInfo.getA();

    if (DataUtils.isId(typeId)) {
      columnValues.put(COL_TYPE, BeeUtils.toString(typeId));
      columnValues.put(ALS_MAINTENANCE_TYPE_NAME, typeInfo.getB());
      columnValues.put(COL_ADDRESS_REQUIRED,
          qs.getValueById(TBL_MAINTENANCE_TYPES, typeId, COL_ADDRESS_REQUIRED));
    }

    IsQuery stateSelect = new SqlSelect()
        .addFields(TBL_STATE_PROCESS, COL_MAINTENANCE_STATE)
        .addField(VIEW_MAINTENANCE_STATES, COL_STATE_NAME,  ALS_STATE_NAME)
        .addFrom(TBL_STATE_PROCESS)
        .addFromLeft(VIEW_MAINTENANCE_STATES,
            sys.joinTables(VIEW_MAINTENANCE_STATES, TBL_STATE_PROCESS, COL_MAINTENANCE_STATE))
        .setWhere(SqlUtils.and(SqlUtils.notNull(TBL_STATE_PROCESS, COL_INITIAL),
            SqlUtils.equals(TBL_STATE_PROCESS, COL_MAINTENANCE_TYPE, typeId),
            SqlUtils.in(TBL_STATE_PROCESS, AdministrationConstants.COL_ROLE,
                AdministrationConstants.VIEW_USER_ROLES, AdministrationConstants.COL_ROLE,
                SqlUtils.equals(AdministrationConstants.VIEW_USER_ROLES,
                    AdministrationConstants.COL_USER, usr.getCurrentUserId()))
            ))
        .setLimit(1);
    SimpleRow stateRow = qs.getRow(stateSelect);

    if (stateRow != null) {
      columnValues.put(AdministrationConstants.COL_STATE, stateRow.getValue(COL_MAINTENANCE_STATE));
      columnValues.put(ALS_STATE_NAME, stateRow.getValue(ALS_STATE_NAME));
    }

    IsQuery departmentSelect = new SqlSelect()
        .addFields(VIEW_DEPARTMENTS, sys.getIdName(VIEW_DEPARTMENTS), ALS_DEPARTMENT_NAME)
        .addFrom(VIEW_DEPARTMENTS)
        .addFromLeft(VIEW_DEPARTMENT_EMPLOYEES,
            sys.joinTables(VIEW_DEPARTMENTS, VIEW_DEPARTMENT_EMPLOYEES, COL_DEPARTMENT))
        .setWhere(SqlUtils.equals(VIEW_DEPARTMENT_EMPLOYEES, COL_COMPANY_PERSON,
            usr.getCompanyPerson(usr.getCurrentUserId())));
    SimpleRowSet departmentRowSet = qs.getData(departmentSelect);

    if (departmentRowSet != null && departmentRowSet.getNumberOfRows() == 1) {
      columnValues.put(COL_DEPARTMENT,
          departmentRowSet.getRow(0).getValue(sys.getIdName(VIEW_DEPARTMENTS)));
      columnValues.put(ALS_DEPARTMENT_NAME,
          departmentRowSet.getRow(0).getValue(ALS_DEPARTMENT_NAME));
    }

    return ResponseObject.response(columnValues);
  }

  private BeeRowSet getSettings() {
    long userId = usr.getCurrentUserId();
    Filter filter = Filter.equals(COL_USER, userId);

    BeeRowSet rowSet = qs.getViewData(VIEW_SERVICE_SETTINGS, filter);
    if (!DataUtils.isEmpty(rowSet)) {
      return rowSet;
    }

    SqlInsert sqlInsert = new SqlInsert(TBL_SERVICE_SETTINGS).addConstant(COL_USER, userId);

    ResponseObject response = qs.insertDataWithResponse(sqlInsert);
    if (response.hasErrors()) {
      return null;
    } else {
      return qs.getViewData(VIEW_SERVICE_SETTINGS, filter);
    }
  }

  private ResponseObject informCustomer(RequestInfo reqInfo) {
    Long commentId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_COMMENT));

    if (DataUtils.isId(commentId)) {

      SimpleRow commentInfoRow = qs.getRow(new SqlSelect()
          .addFields(TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE, COL_COMMENT,
              COL_PUBLISH_TIME, COL_EVENT_NOTE, COL_TERM, COL_SEND_EMAIL, COL_SEND_SMS)
          .addFields(TBL_EMAILS, COL_EMAIL)
          .addFields(TBL_CONTACTS, COL_PHONE)
          .addFields(TBL_PERSONS, COL_FIRST_NAME)
          .addFields(TBL_SERVICE_OBJECTS, COL_MODEL)
          .addField(TBL_COMPANIES, COL_COMPANY_NAME, ALS_MANUFACTURER_NAME)
          .addFields(TBL_SERVICE_MAINTENANCE, COL_DEPARTMENT)
          .addFrom(TBL_MAINTENANCE_COMMENTS)
          .addFromInner(TBL_SERVICE_MAINTENANCE, sys.joinTables(TBL_SERVICE_MAINTENANCE,
              TBL_MAINTENANCE_COMMENTS, COL_SERVICE_MAINTENANCE))
          .addFromInner(TBL_SERVICE_OBJECTS, sys.joinTables(TBL_SERVICE_OBJECTS,
              TBL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT))
          .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES,
              TBL_SERVICE_OBJECTS, COL_MANUFACTURER))
          .addFromLeft(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_SERVICE_MAINTENANCE, COL_CONTACT))
          .addFromLeft(TBL_CONTACTS,
              sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
          .addFromLeft(TBL_EMAILS,
              sys.joinTables(TBL_EMAILS, TBL_CONTACTS, COL_EMAIL))
          .addFromLeft(TBL_PERSONS,
              sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
          .addFromLeft(VIEW_MAINTENANCE_STATES,
              sys.joinTables(VIEW_MAINTENANCE_STATES, TBL_SERVICE_MAINTENANCE, COL_STATE))
          .setWhere(sys.idEquals(TBL_MAINTENANCE_COMMENTS, commentId)));

      if (commentInfoRow != null) {
        boolean isSendEmail = false;
        boolean isSendSms = false;

        Dictionary dic = usr.getDictionary();
        DateTimeFormatInfo dtfInfo = usr.getDateTimeFormatInfo();

        if (!BeeUtils.toBoolean(commentInfoRow.getValue(COL_SEND_EMAIL))) {
          ResponseObject mailResponse = informCustomerWithEmail(dic, dtfInfo, commentInfoRow);
          isSendEmail = !mailResponse.hasErrors();
        }

        String error = BeeConst.STRING_EMPTY;

        if (!BeeUtils.toBoolean(commentInfoRow.getValue(COL_SEND_SMS))) {
          String from = BeeConst.STRING_EMPTY;

          if (!BeeUtils.isEmpty(prm.getText(PRM_SMS_REQUEST_CONTACT_INFO_FROM))) {
            SqlSelect phoneFromSelect = new SqlSelect()
                .addFrom(TBL_CONTACTS)
                .addFields(TBL_CONTACTS, COL_PHONE);

            switch (prm.getText(PRM_SMS_REQUEST_CONTACT_INFO_FROM)) {
              case AdministrationConstants.VIEW_DEPARTMENTS:
                Long departmentId = commentInfoRow.getLong(COL_DEPARTMENT);

                if (DataUtils.isId(departmentId)) {
                  phoneFromSelect.addFromLeft(AdministrationConstants.VIEW_DEPARTMENTS,
                      sys.joinTables(TBL_CONTACTS, AdministrationConstants.VIEW_DEPARTMENTS,
                          COL_CONTACT))
                      .setWhere(sys.idEquals(AdministrationConstants.VIEW_DEPARTMENTS,
                          commentInfoRow.getLong(COL_DEPARTMENT)));
                } else {
                  phoneFromSelect = null;
                }
                break;

              case VIEW_COMPANIES:
                phoneFromSelect.addFromLeft(TBL_COMPANIES,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANIES, COL_CONTACT))
                    .addFromLeft(TBL_COMPANY_PERSONS,
                        sys.joinTables(TBL_COMPANIES, TBL_COMPANY_PERSONS, COL_COMPANY))
                    .setWhere(sys.idEquals(TBL_COMPANY_PERSONS,
                        usr.getCompanyPerson(usr.getCurrentUserId())));
                break;

              case VIEW_COMPANY_PERSONS:
                phoneFromSelect.addFromLeft(TBL_COMPANY_PERSONS,
                    sys.joinTables(TBL_CONTACTS, TBL_COMPANY_PERSONS, COL_CONTACT))
                    .setWhere(sys.idEquals(TBL_COMPANY_PERSONS,
                        usr.getCompanyPerson(usr.getCurrentUserId())));
                break;
            }

            if (phoneFromSelect != null) {
              from = qs.getValue(phoneFromSelect);
            }
          } else {
            from = prm.getText(PRM_SMS_REQUEST_SERVICE_FROM);
          }

          if (BeeUtils.isEmpty(from)) {
            error = dic.svcEmptySmsFromError();

          } else {
            ResponseObject smsResponse = informCustomerWithSms(dic, dtfInfo, commentInfoRow, from);
            isSendSms = !smsResponse.hasErrors();
          }
        }

        Map<String, Value> updatableValues = Maps.newHashMap();
        updatableValues.put(COL_CUSTOMER_SENT, null);

        if (isSendEmail) {
          updatableValues.put(COL_SEND_EMAIL, Value.getValue(isSendEmail));
        }

        if (isSendSms) {
          updatableValues.put(COL_SEND_SMS, Value.getValue(isSendSms));
        }

        SqlUpdate updateQuery = new SqlUpdate(TBL_MAINTENANCE_COMMENTS)
            .setWhere(sys.idEquals(TBL_MAINTENANCE_COMMENTS, commentId));
        updatableValues.forEach(updateQuery::addConstant);

        qs.updateData(updateQuery);

        ResponseObject result = ResponseObject.response(updatableValues);

        if (!BeeUtils.isEmpty(error)) {
          result.addError(error);
        }
        return result;
      }
    }

    return ResponseObject.emptyResponse();
  }

  private ResponseObject informCustomerWithEmail(Dictionary dic, DateTimeFormatInfo dtfInfo,
      SimpleRow commentInfoRow) {

    Long accountId = mail.getSenderAccountId(SVC_INFORM_CUSTOMER);

    if (!DataUtils.isId(accountId)) {
      return ResponseObject.error("No default account specified");
    }

    String recipientEmail = commentInfoRow.getValue(COL_EMAIL);

    if (BeeUtils.isEmpty(recipientEmail)) {
      return ResponseObject.error("No recipient email specified");
    }

    String maintenanceId = commentInfoRow.getValue(COL_SERVICE_MAINTENANCE);
    String deviceDescription = BeeUtils.joinWords(commentInfoRow.getValue(ALS_MANUFACTURER_NAME),
        commentInfoRow.getValue(COL_MODEL));

    String emailHeader = BeeUtils.joinWords(dic.svcRepair(), maintenanceId, deviceDescription);

    Document doc = new Document();
    doc.getHead().append(meta().encodingDeclarationUtf8());

    Div panel = div();
    doc.getBody().append(panel);
    Tbody fields = tbody().append(
        tr().append(
            td().text(dic.svcRepair()), td().text(maintenanceId)));

    if (!BeeUtils.isEmpty(deviceDescription)) {
      fields.append(tr().append(
          td().text(dic.svcDevice()), td().text(deviceDescription)));
    }

    fields.append(tr().append(
            td().text(dic.date()),
            td().text(Formatter.renderDateTime(dtfInfo,
                commentInfoRow.getDateTime(COL_PUBLISH_TIME)))),
        tr().append(
            td().text(dic.svcMaintenanceState()),
            td().text(commentInfoRow.getValue(COL_EVENT_NOTE))));
    DateTime termValue = commentInfoRow.getDateTime(COL_TERM);

    if (termValue != null) {
      fields.append(tr().append(
          td().text(dic.svcTerm()), td().text(Formatter.renderDateTime(dtfInfo, termValue))));
    }

    List<Element> cells = fields.queryTag(Tags.TD);
    for (Element cell : cells) {
      if (cell.index() == 0) {
        cell.setPaddingRight(1, CssUnit.EM);
        cell.setFontWeight(FontWeight.BOLDER);
      }
    }

    panel.append(table().append(fields));

    String externalLink = prm.getText(PRM_EXTERNAL_MAINTENANCE_URL);
    String externalServiceUrl = BeeConst.STRING_EMPTY;

    if (!BeeUtils.isEmpty(externalLink)) {
      externalServiceUrl = BeeUtils.join(BeeConst.STRING_EMPTY, externalLink, maintenanceId);
    }

    doc.getBody().append(div().text("<br />"));

    doc.getBody().append(div().text(dic.svcMaintenanceEmailContent(
        BeeUtils.notEmpty(commentInfoRow.getValue(COL_FIRST_NAME), ""),
        BeeUtils.notEmpty(commentInfoRow.getValue(COL_COMMENT), ""),
        externalServiceUrl)));

    String signature = qs.getValue(new SqlSelect()
        .addFields(MailConstants.TBL_SIGNATURES, MailConstants.COL_SIGNATURE_CONTENT)
        .addFrom(MailConstants.TBL_SIGNATURES)
        .addFromInner(MailConstants.TBL_ACCOUNTS, sys.joinTables(MailConstants.TBL_SIGNATURES,
            MailConstants.TBL_ACCOUNTS, MailConstants.COL_SIGNATURE))
        .setWhere(sys.idEquals(MailConstants.TBL_ACCOUNTS, accountId)));

    doc.getBody().append(div().text("<br />"));
    doc.getBody().append(div().text(signature));

    String subject = BeeUtils.joinWords(dic.svcRepair(), maintenanceId);

    return mail.sendStyledMail(accountId, recipientEmail, subject, doc.buildLines(), emailHeader);
  }

  private ResponseObject informCustomerWithSms(Dictionary dic, DateTimeFormatInfo dtfInfo,
      SimpleRow commentInfoRow, String from) {

    String phone = commentInfoRow.getValue(COL_PHONE);
    phone = phone.replaceAll("\\D+", "");

    Long maintenanceId = commentInfoRow.getLong(COL_SERVICE_MAINTENANCE);
    String externalLink = prm.getText(PRM_EXTERNAL_MAINTENANCE_URL);
    String externalServiceUrl = BeeConst.STRING_EMPTY;

    if (!BeeUtils.isEmpty(externalLink)) {
      externalServiceUrl = BeeUtils.join(BeeConst.STRING_EMPTY, externalLink, maintenanceId);
    }

    DateTime termValue = commentInfoRow.getDateTime(COL_TERM);
    String message = BeeUtils.joinWords(dic.svcRepair(),
        maintenanceId + BeeConst.STRING_COLON,
        BeeUtils.notEmpty(commentInfoRow.getValue(COL_COMMENT), ""),
        termValue != null ? BeeUtils.joinWords(dic.svcTerm() + BeeConst.STRING_COLON,
            Formatter.renderDateTime(dtfInfo, termValue)) : "", externalServiceUrl);

    if (BeeUtils.isEmpty(message) || BeeUtils.isEmpty(phone)) {
      return ResponseObject.error("message or phone is empty");
    }

    String address = prm.getText(PRM_SMS_REQUEST_SERVICE_ADDRESS);
    String userName = prm.getText(PRM_SMS_REQUEST_SERVICE_USER_NAME);
    String password = prm.getText(PRM_SMS_REQUEST_SERVICE_PASSWORD);


    if (BeeUtils.isEmpty(address)) {
      logger.warning(BeeUtils.joinWords(PRM_SMS_REQUEST_SERVICE_ADDRESS, " is empty"));
      return ResponseObject.error(PRM_SMS_REQUEST_SERVICE_ADDRESS + " is empty");
    }
    if (BeeUtils.isEmpty(userName)) {
      logger.warning(BeeUtils.joinWords(PRM_SMS_REQUEST_SERVICE_USER_NAME, " is empty"));
      return ResponseObject.error(PRM_SMS_REQUEST_SERVICE_USER_NAME + " is empty");
    }
    if (BeeUtils.isEmpty(password)) {
      logger.warning(BeeUtils.joinWords(PRM_SMS_REQUEST_SERVICE_PASSWORD, " is empty"));
      return ResponseObject.error(PRM_SMS_REQUEST_SERVICE_PASSWORD + " is empty");
    }

    Client client = ClientBuilder.newClient();
    UriBuilder uriBuilder = UriBuilder.fromPath(address);

    uriBuilder.queryParam("username", userName);
    uriBuilder.queryParam("password", password);
    uriBuilder.queryParam("message", message);
    uriBuilder.queryParam("from", from);
    uriBuilder.queryParam("to", phone);
    WebTarget webtarget = client.target(uriBuilder);

    Invocation.Builder builder = webtarget.request(MediaType.TEXT_PLAIN_TYPE)
        .acceptEncoding(BeeConst.CHARSET_UTF8);

    Response response = builder.get();
    String smsResponseMessage = response.readEntity(String.class);

    if (response.getStatus() == 200 && !BeeUtils.isEmpty(smsResponseMessage)
        && BeeUtils.containsSame(smsResponseMessage, "OK")) {
      return ResponseObject.emptyResponse();
    }

    logger.warning(BeeUtils.joinWords("Method informCustomerWithSms", smsResponseMessage));
    return ResponseObject.error(smsResponseMessage);
  }

  private ResponseObject updateServiceMaintenanceObject(RequestInfo reqInfo) {
    Long maintenanceId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SERVICE_MAINTENANCE));
    Long objectId = BeeUtils.toLongOrNull(reqInfo.getParameter(COL_SERVICE_OBJECT));

    if (!DataUtils.isId(maintenanceId) && !DataUtils.isId(objectId)) {
      return ResponseObject.error(reqInfo.getService(), "parameters not found");
    }

    IsCondition latestMaintenanceCondition = null;

    if (DataUtils.isId(objectId)) {
      SqlSelect maxServiceMaintenanceDateQuery = new SqlSelect()
          .addMax(TBL_SERVICE_MAINTENANCE, COL_MAINTENANCE_DATE)
          .addFrom(TBL_SERVICE_MAINTENANCE)
          .setWhere(SqlUtils.equals(TBL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT, objectId));
      DateTime maxServiceMaintenanceDate = qs.getDateTime(maxServiceMaintenanceDateQuery);

      if (maxServiceMaintenanceDate != null) {
        latestMaintenanceCondition = SqlUtils.equals(TBL_SERVICE_MAINTENANCE, COL_MAINTENANCE_DATE,
            maxServiceMaintenanceDate);
      }
    }
    IsCondition maintenanceFilter;

    if (DataUtils.isId(maintenanceId)) {
      maintenanceFilter = sys.idEquals(TBL_SERVICE_MAINTENANCE, maintenanceId);
    } else {
      maintenanceFilter = SqlUtils.equals(TBL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT, objectId);
    }

    SqlSelect serviceMaintenanceQuery = new SqlSelect()
        .addFields(TBL_SERVICE_MAINTENANCE, sys.getIdName(TBL_SERVICE_MAINTENANCE), COL_ENDING_DATE,
            COL_COMPANY, COL_CONTACT, COL_SERVICE_OBJECT)
            .addFields(TBL_SERVICE_OBJECTS,
                    COL_SERVICE_CUSTOMER, ALS_CONTACT_PERSON)
            .addFrom(TBL_SERVICE_MAINTENANCE)
            .addFromLeft(TBL_SERVICE_OBJECTS, sys.joinTables(VIEW_SERVICE_OBJECTS,
                    TBL_SERVICE_MAINTENANCE, COL_SERVICE_OBJECT))
            .setWhere(SqlUtils.and(latestMaintenanceCondition, maintenanceFilter));

    SimpleRowSet serviceMaintenanceRs = qs.getData(serviceMaintenanceQuery);

    if (!DataUtils.isEmpty(serviceMaintenanceRs)) {
      SimpleRow maintenanceRow = serviceMaintenanceRs.getRow(0);

      String maintenanceCompany = maintenanceRow.getValue(COL_COMPANY);
      String maintenanceContact = maintenanceRow.getValue(COL_CONTACT);
      String objectCompany = maintenanceRow.getValue(COL_SERVICE_CUSTOMER);
      String objectContact = maintenanceRow.getValue(ALS_CONTACT_PERSON);

      if (!BeeUtils.same(maintenanceCompany, objectCompany)
              || !BeeUtils.same(maintenanceContact, objectContact)) {
        SqlUpdate update = null;
        Long responseResult = null;

        if (DataUtils.isId(maintenanceId)) {
          update = new SqlUpdate(TBL_SERVICE_OBJECTS)
                  .addConstant(COL_SERVICE_CUSTOMER,
                          maintenanceRow.getValue(COL_COMPANY))
                  .addConstant(ALS_CONTACT_PERSON,
                          maintenanceRow.getValue(COL_CONTACT))
                  .setWhere(sys.idEquals(TBL_SERVICE_OBJECTS,
                          maintenanceRow.getLong(COL_SERVICE_OBJECT)));
          responseResult = maintenanceRow.getLong(COL_SERVICE_OBJECT);

        } else {
          maintenanceId = maintenanceRow.getLong(sys.getIdName(TBL_SERVICE_MAINTENANCE));

          if (maintenanceRow.getValue(COL_ENDING_DATE) == null && DataUtils.isId(maintenanceId)) {
            update = new SqlUpdate(TBL_SERVICE_MAINTENANCE)
                    .addConstant(COL_COMPANY,
                            maintenanceRow.getValue(COL_SERVICE_CUSTOMER))
                    .addConstant(COL_CONTACT,
                            maintenanceRow.getValue(ALS_CONTACT_PERSON))
                    .setWhere(sys.idEquals(TBL_SERVICE_MAINTENANCE, maintenanceId));
            responseResult = maintenanceId;
          }
        }

        if (update != null) {
          ResponseObject response = qs.updateDataWithResponse(update);

          if (response.hasErrors()) {
            return response;
          }

          return ResponseObject.response(responseResult);
        }
      }
    }
    return ResponseObject.emptyResponse();
  }
}
