package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataCache;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.data.RowInsertCallback;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.GridFactory.GridOptions;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.output.ReportUtils;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.presenter.PresenterCallback;
import com.butent.bee.client.style.ColorStyleProvider;
import com.butent.bee.client.style.ConditionalStyle;
import com.butent.bee.client.ui.EnablableWidget;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.ViewCallback;
import com.butent.bee.client.view.ViewFactory;
import com.butent.bee.client.view.ViewSupplier;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.FileGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.menu.MenuHandler;
import com.butent.bee.shared.menu.MenuService;
import com.butent.bee.shared.modules.administration.*;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.rights.SubModule;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Preloader;
import com.butent.bee.shared.ui.UserInterface;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class TradeActKeeper {

  public static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "ta-";

  private static final String STYLE_COMMAND_PREFIX = STYLE_PREFIX + "command-";
  private static final String STYLE_COMMAND_DISABLED = STYLE_COMMAND_PREFIX + "disabled";

  private static final BeeLogger logger = LogUtils.getLogger(TradeActKeeper.class);

  private static final String GRID_ALL_ACTS_KEY = GRID_TRADE_ACTS + BeeConst.STRING_UNDER
      + BeeConst.ALL;

  private static final DataCache cache = new DataCache();
  private static boolean cacheLoaded;

  private static Long returnedActStatus;
  private static boolean parametersLoaded;

  private static Pair<String, String> tradeActStatus;

  public static ParameterList createArgs(String method) {
    return BeeKeeper.getRpc().createParameters(Module.TRADE, SubModule.ACTS, method);
  }

  public static void ensureSendMailPrintableForm(FormView form) {
    HeaderView header = form.getViewPresenter().getHeader();

    if (header != null) {
      FaLabel mail = new FaLabel(FontAwesome.ENVELOPE_O);

      mail.addClickHandler((ClickEvent clickEvent) -> {
        Long id = form.getActiveRowId();

        ReportUtils.getPdf(form.getPrintElement().getString(), (fileInfo) -> {
          String invoice = BeeUtils.same(form.getViewName(), VIEW_SALES)
              ? BeeUtils.join("_", Localized.dictionary().trdInvoice(),
                  BeeUtils.join("", form.getStringValue(COL_TRADE_INVOICE_PREFIX),
                          form.getStringValue(COL_TRADE_INVOICE_NO)))
              : BeeUtils.join("_", form.getCaption(), form.getActiveRowId());

          if (!BeeUtils.isEmpty(invoice)) {
            fileInfo.setCaption(invoice + ".pdf");
          }

          Long companyId = form.getLongValue(COL_COMPANY);

          if (BeeUtils.same(form.getViewName(), VIEW_SALES)) {
            companyId = form.getLongValue(COL_CUSTOMER);
          }

          String content = BeeUtils.same(form.getViewName(), VIEW_SALES)
              ? ""
              : Localized.dictionary().tradeAct();

          Queries.getValue(VIEW_COMPANIES, BeeUtils.unbox(companyId),
              COL_EMAIL, new RpcCallback<String>() {
                @Override
                public void onSuccess(String email) {
                  NewMailMessage.create(email, invoice, content,
                      Collections.singleton(fileInfo), (messageId, saveMode) -> {
                        if (!BeeUtils.same(form.getViewName(), VIEW_SALES)) {
                          return;
                        }

                        DataInfo info = Data.getDataInfo(VIEW_SALE_FILES);

                        Queries.insert(info.getViewName(),
                            Arrays.asList(
                                info.getColumn(COL_SALE),
                                info.getColumn(AdministrationConstants.COL_FILE),
                                info.getColumn(COL_NOTES),
                                info.getColumn(MailConstants.COL_MESSAGE)),
                            Arrays.asList(
                                BeeUtils.toString(id),
                                BeeUtils.toString(fileInfo.getId()),
                                Format.renderDateTime(TimeUtils.nowMinutes()),
                                BeeUtils.toString(messageId)),
                            null,
                            new RowInsertCallback(info.getViewName()) {
                              @Override
                              public void onSuccess(BeeRow result) {
                                Data.onTableChange(info.getTableName(),
                                    DataChangeEvent.RESET_REFRESH);
                                form.updateCell("IsSentToEmail", BeeConst.STRING_TRUE);
                                form.refreshBySource("IsSentToEmail");
                                form.getViewPresenter().handleAction(Action.SAVE);
                                super.onSuccess(result);
                              }
                            });
                      });
                }
              });
        });
      });
      header.addCommandItem(mail);
    }
  }

  public static void register() {
    BeeKeeper.getBus().registerDataHandler(cache, false);

    for (TradeActKind kind : TradeActKind.values()) {
      if (kind.getGridSupplierKey() != null) {
        ViewFactory.registerSupplier(kind.getGridSupplierKey(), createActViewSupplier(kind));
      }
    }

    ViewFactory.registerSupplier(GRID_ALL_ACTS_KEY, createActViewSupplier(null));

    MenuService.TRADE_ACT_NEW.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        final TradeActKind kind = EnumUtils.getEnumByName(TradeActKind.class, parameters);

        if (kind == null) {
          logger.severe(MenuService.TRADE_ACT_NEW.name(), "kind not recognized", parameters);

        } else {
          ensureChache(() -> {
            DataInfo dataInfo = Data.getDataInfo(VIEW_TRADE_ACTS);
            BeeRow row = RowFactory.createEmptyRow(dataInfo, true);

            prepareNewTradeAct(row, kind);

            RowFactory.createRow(dataInfo, row, Modality.ENABLED);
          });
        }
      }
    });

    MenuService.TRADE_ACT_LIST.setHandler(new MenuHandler() {
      @Override
      public void onSelection(String parameters) {
        if (BeeUtils.same(parameters, BeeConst.ALL)) {
          ViewFactory.createAndShow(GRID_ALL_ACTS_KEY);

        } else {
          TradeActKind kind = EnumUtils.getEnumByName(TradeActKind.class, parameters);

          if (kind != null && kind.getGridSupplierKey() != null) {
            ViewFactory.createAndShow(kind.getGridSupplierKey());
          } else {
            logger.severe(GRID_TRADE_ACTS, "kind not recognized", parameters);
          }
        }
      }
    });

    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_ACTS, COL_TA_OPERATION,
        ColorStyleProvider.create(VIEW_TRADE_ACTS,
            ALS_OPERATION_BACKGROUND, ALS_OPERATION_FOREGROUND));
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_ACTS, COL_TA_STATUS,
        ColorStyleProvider.create(VIEW_TRADE_ACTS,
            ALS_STATUS_BACKGROUND, ALS_STATUS_FOREGROUND));

    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_ACT_TEMPLATES, COL_TA_OPERATION,
        ColorStyleProvider.create(VIEW_TRADE_ACT_TEMPLATES,
            ALS_OPERATION_BACKGROUND, ALS_OPERATION_FOREGROUND));
    ConditionalStyle.registerGridColumnStyleProvider(GRID_TRADE_ACT_TEMPLATES, COL_TA_STATUS,
        ColorStyleProvider.create(VIEW_TRADE_ACT_TEMPLATES,
            ALS_STATUS_BACKGROUND, ALS_STATUS_FOREGROUND));

    FormFactory.registerFormInterceptor(FORM_TRADE_ACT, new TradeActForm());
    FormFactory.registerFormInterceptor(FORM_INVOICE_BUILDER, new TradeActInvoiceBuilder());

    if (isClientArea()) {
      FormFactory.registerFormInterceptor(FORM_NEW_COMPANY, null);
      FormFactory.registerFormInterceptor(FORM_COMPANY_PERSON, new AbstractFormInterceptor() {
        @Override
        public void onSetActiveRow(IsRow row) {
          getFormView().getViewPresenter().handleAction(Action.CLOSE);
        }

        @Override
        public FormInterceptor getInstance() {
          return this;
        }
      });
    }

    AbstractGridInterceptor interceptor = new AbstractGridInterceptor() {
      @Override
      public boolean beforeAction(Action action, GridPresenter presenter) {
        if (Action.AUDIT == action) {
          return false;
        }
        return super.beforeAction(action, presenter);
      }

      @Override
      public GridInterceptor getInstance() {
        return this;
      }
    };

      GridFactory.registerGridInterceptor(GRID_TRADE_ACT_ITEMS, isClientArea() ? interceptor
          : new TradeActItemsGrid());
      GridFactory.registerGridInterceptor(GRID_TRADE_ACT_SERVICES, isClientArea() ? interceptor
          : new TradeActServicesGrid());
      GridFactory.registerGridInterceptor(VIEW_TRADE_ACT_FILES,
          new FileGridInterceptor(COL_TRADE_ACT, AdministrationConstants.COL_FILE,
              AdministrationConstants.COL_FILE_CAPTION, AdministrationConstants.ALS_FILE_NAME));

    if (isClientArea()) {
      GridFactory.registerGridInterceptor(GRID_SALES, new AbstractGridInterceptor() {

        @Override
        public void onEditStart(EditStartEvent event) {
          super.onEditStart(event);

          final IsRow row = event.getRowValue();

          if (!DataUtils.isId(row.getId())) {
            return;
          }

          Queries.update(VIEW_SALES, row.getId(), "Visited", Value.getValue(new DateTime()),
              new Queries.IntCallback() {
                @Override
                public void onSuccess(Integer result) {
                  Queries.getRow(VIEW_SALES, row.getId(), new RowCallback() {
                    @Override
                    public void onSuccess(BeeRow updRow) {
                      RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_SALES,  updRow);
                    }
                  });
                }
              });
        }

        @Override
        public GridInterceptor getInstance() {
          return this;
        }
      });
      GridFactory.registerGridInterceptor(VIEW_SALE_ITEMS, interceptor);
      GridFactory.registerGridInterceptor(VIEW_INVOICE_TRADE_ACTS, interceptor);
      GridFactory.registerGridInterceptor(GRID_COMPANY_PERSONS, new AbstractGridInterceptor() {
        @Override
        public void onEditStart(EditStartEvent event) {
          super.onEditStart(event);
          event.consume();
        }

        @Override
        public GridInterceptor getInstance() {
          return this;
        }
      });
    }

    SelectorEvent.register(new TradeActSelectorHandler());

    Preloader cacheLoader = input -> ensureChache(input);

    FormFactory.registerPreloader(FORM_TRADE_ACT, cacheLoader);
    FormFactory.registerPreloader(FORM_INVOICE_BUILDER, cacheLoader);

    GridFactory.registerPreloader(GRID_TRADE_ACT_TEMPLATES, cacheLoader);

    if (isClientArea()) {
      GridFactory.registerImmutableFilter(VIEW_SALES,
          Filter.equals(COL_TRADE_CUSTOMER, BeeKeeper.getUser().getCompany()));
    }
  }

  static void addCommandStyle(Widget command, String suffix) {
    command.addStyleName(STYLE_COMMAND_PREFIX + suffix);
  }

  static void ensureChache(final Runnable command) {
    if (cacheLoaded) {
      command.run();

    } else {
      List<String> viewNames = Lists.newArrayList(VIEW_TRADE_OPERATIONS, VIEW_TRADE_SERIES,
          VIEW_SERIES_MANAGERS, VIEW_WAREHOUSES, VIEW_TRADE_STATUSES);
      final long start = System.currentTimeMillis();

      cache.getData(viewNames, new DataCache.MultiCallback() {
        @Override
        public void onSuccess(Integer result) {
          cacheLoaded = true;
          logger.debug("trade act cache loaded", result, TimeUtils.elapsedMillis(start));

          command.run();
        }
      });
    }
  }

  static void ensureParameters(final ScheduledCommand command) {
    if (parametersLoaded) {
      command.execute();
    } else {
      parametersLoaded = true;
      returnedActStatus = Global.getParameterRelation(PRM_RETURNED_ACT_STATUS);
      logger.debug("trade act parameters loaded");
      command.execute();
    }
  }

  public static Collection<Long> extractWarehouses(BeeRowSet rowSet) {
    if (DataUtils.isEmpty(rowSet)) {
      return new HashSet<>();
    } else {
      return DataUtils.parseIdSet(rowSet.getTableProperty(TBL_WAREHOUSES));
    }
  }

  static Collection<Long> filterOperations(TradeActKind kind) {
    List<Long> operations = new ArrayList<>();
    if (kind == null) {
      return operations;
    }

    BeeRowSet rowSet = cache.getRowSet(VIEW_TRADE_OPERATIONS);
    if (DataUtils.isEmpty(rowSet)) {
      return operations;
    }

    int index = rowSet.getColumnIndex(COL_OPERATION_KIND);

    for (BeeRow row : rowSet) {
      if (getKind(row, index) == kind) {
        operations.add(row.getId());
      }
    }

    return operations;
  }

  static Set<Long> getActiveStatuses() {
    Set<Long> result = new HashSet<>();

    BeeRowSet rowSet = cache.getRowSet(VIEW_TRADE_STATUSES);
    if (!DataUtils.isEmpty(rowSet)) {
      int index = rowSet.getColumnIndex(COL_STATUS_ACTIVE);

      for (BeeRow row : rowSet) {
        if (!row.isNull(index)) {
          result.add(row.getId());
        }
      }
    }

    return result;
  }

  static Pair<Long, String> getDefaultOperation(TradeActKind kind) {
    if (kind == null) {
      return null;
    }
    BeeRowSet rowSet = cache.getRowSet(VIEW_TRADE_OPERATIONS);
    if (DataUtils.isEmpty(rowSet)) {
      return null;
    }

    Long id = null;
    String name = null;

    int nameIndex = rowSet.getColumnIndex(COL_OPERATION_NAME);
    int kindIndex = rowSet.getColumnIndex(COL_OPERATION_KIND);
    int defIndex = rowSet.getColumnIndex(COL_OPERATION_DEFAULT);
    int statusIndex = rowSet.getColumnIndex(ALS_TRADE_STATUS_NAME);
    int statusIdIndex = rowSet.getColumnIndex(ALS_TRADE_STATUS);

    for (BeeRow row : rowSet) {
      if (getKind(row, kindIndex) == kind) {
        if (BeeUtils.isTrue(row.getBoolean(defIndex))) {
          id = row.getId();
          name = row.getString(nameIndex);
          tradeActStatus = Pair.of(row.getString(statusIdIndex), row.getString(statusIndex));
          break;

        } else if (DataUtils.isId(id)) {
          id = null;
          break;

        } else {
          id = row.getId();
          name = row.getString(nameIndex);
          tradeActStatus = Pair.of(row.getString(statusIdIndex), row.getString(statusIndex));
        }
      }
    }

    if (DataUtils.isId(id)) {
      return Pair.of(id, name);
    } else if (kind == TradeActKind.SUPPLEMENT) {
      return getDefaultOperation(TradeActKind.SALE);
    } else {
      return null;
    }
  }

  static ItemPrice getItemPrice(Long operation) {
    if (DataUtils.isId(operation)) {
      return EnumUtils.getEnumByIndex(ItemPrice.class,
          cache.getInteger(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_PRICE));
    } else {
      return null;
    }
  }

  public static ItemPrice getItemPrice(String viewName, IsRow row) {
    return getItemPrice(Data.getLong(viewName, row, COL_TA_OPERATION));
  }

  static TradeActKind getKind(String viewName, IsRow row) {
    return getKind(row, Data.getColumnIndex(viewName, COL_TA_KIND));
  }

  static TradeActKind getKind(IsRow row, int index) {
    if (row == null || BeeConst.isUndef(index)) {
      return null;
    } else {
      return EnumUtils.getEnumByIndex(TradeActKind.class, row.getInteger(index));
    }
  }

  static TradeActKind getOperationKind(Long operation) {
    if (DataUtils.isId(operation)) {
      return EnumUtils.getEnumByIndex(TradeActKind.class,
          cache.getInteger(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_KIND));
    } else {
      return null;
    }
  }

  static String getOperationName(Long operation) {
    if (DataUtils.isId(operation)) {
      return cache.getString(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_NAME);
    } else {
      return null;
    }
  }

  static Long getReturnedActStatus() {
    return returnedActStatus;
  }

  static BeeRowSet getStatuses() {
    return cache.getRowSet(VIEW_TRADE_STATUSES);
  }

  static BeeRowSet getUserSeries(boolean checkDefaults) {
    Long userId = BeeKeeper.getUser().getUserId();
    if (!DataUtils.isId(userId)) {
      return null;
    }

    BeeRowSet seriesManagers = cache.getRowSet(VIEW_SERIES_MANAGERS);
    if (DataUtils.isEmpty(seriesManagers)) {
      return null;
    }

    int seriesIndex = seriesManagers.getColumnIndex(COL_SERIES);
    int managerIndex = seriesManagers.getColumnIndex(COL_SERIES_MANAGER);
    int defIndex = seriesManagers.getColumnIndex(COL_SERIES_DEFAULT);

    Set<Long> ms = new HashSet<>();
    for (BeeRow row : seriesManagers) {
      if (userId.equals(row.getLong(managerIndex))) {
        if (checkDefaults && BeeUtils.isTrue(row.getBoolean(defIndex))) {
          ms.clear();
          ms.add(row.getLong(seriesIndex));
          break;
        }

        ms.add(row.getLong(seriesIndex));
      }
    }

    if (ms.isEmpty()) {
      return null;
    }

    BeeRowSet series = cache.getRowSet(VIEW_TRADE_SERIES);
    if (DataUtils.isEmpty(series)) {
      return null;
    }

    BeeRowSet result = new BeeRowSet(series.getViewName(), series.getColumns());
    for (BeeRow row : series) {
      if (ms.contains(row.getId())) {
        result.addRow(row);
      }
    }

    if (result.isEmpty()) {
      return null;
    } else {
      return result;
    }
  }

  public static Map<Long, String> getWarehouses(Collection<Long> ids) {
    Map<Long, String> result = new LinkedHashMap<>();

    if (!BeeUtils.isEmpty(ids)) {
      for (Long id : ids) {
        if (DataUtils.isId(id)) {
          String code = cache.getString(VIEW_WAREHOUSES, id, COL_WAREHOUSE_CODE);
          if (!BeeUtils.isEmpty(code)) {
            result.put(id, code);
          }
        }
      }

      if (result.size() > 1) {
        TreeMap<String, Long> sorted = new TreeMap<>();
        for (Map.Entry<Long, String> entry : result.entrySet()) {
          sorted.put(entry.getValue(), entry.getKey());
        }

        result.clear();
        for (Map.Entry<String, Long> entry : sorted.entrySet()) {
          result.put(entry.getValue(), entry.getKey());
        }
      }
    }

    return result;
  }

  public static boolean isClientArea() {
    return BeeKeeper.getScreen().getUserInterface() == UserInterface.TRADE_ACTS;
  }

  static Long getWarehouseFrom(String viewName, IsRow row) {
    int index = Data.getColumnIndex(viewName, COL_TA_OPERATION);
    if (row == null || BeeConst.isUndef(index)) {
      return null;
    }

    Long operation = row.getLong(index);
    if (DataUtils.isId(operation)) {
      return cache.getLong(VIEW_TRADE_OPERATIONS, operation, COL_OPERATION_WAREHOUSE_FROM);
    } else {
      return null;
    }
  }

  static String getWarehouseCode(Long warehouse) {
    if (DataUtils.isId(warehouse)) {
      return cache.getString(VIEW_WAREHOUSES, warehouse, COL_WAREHOUSE_CODE);
    } else {
      return null;
    }
  }

  static boolean isEnabledItemsGrid(TradeActKind kind, FormView form, IsRow row) {
    if (row == null) {
      return false;
    }

    boolean hasContinuousTa = DataUtils.isId(row.getLong(form.getDataIndex(COL_TA_CONTINUOUS)));
    boolean isContinuousTa = kind == TradeActKind.CONTINUOUS;
    boolean hasMultiReturn = DataUtils.isId(row.getLong(form.getDataIndex(COL_TA_RETURN)));
    boolean hasReturn = BeeUtils.isPositive(row.getLong(form.getDataIndex(ALS_RETURNED_COUNT)));

    return  !hasContinuousTa && !isContinuousTa && !TradeActKeeper.isClientArea()
        && !hasMultiReturn && !hasReturn;
  }

  static boolean isUserSeries(Long series) {
    if (!DataUtils.isId(series)) {
      return false;
    }

    Long userId = BeeKeeper.getUser().getUserId();
    if (!DataUtils.isId(userId)) {
      return false;
    }

    BeeRowSet seriesManagers = cache.getRowSet(VIEW_SERIES_MANAGERS);
    if (DataUtils.isEmpty(seriesManagers)) {
      return false;
    }

    int seriesIndex = seriesManagers.getColumnIndex(COL_SERIES);
    int managerIndex = seriesManagers.getColumnIndex(COL_SERIES_MANAGER);

    for (BeeRow row : seriesManagers) {
      if (series.equals(row.getLong(seriesIndex)) && userId.equals(row.getLong(managerIndex))) {
        return true;
      }
    }
    return false;
  }

  static void prepareNewTradeAct(IsRow row, TradeActKind kind) {
    if (kind != null) {
      Data.setValue(VIEW_TRADE_ACTS, row, COL_TA_KIND, kind.ordinal());
      setDefaultOperation(row, kind);
    }

    BeeRowSet userSeries = getUserSeries(true);
    if (userSeries != null && userSeries.getNumberOfRows() == 1) {
      Data.setValue(VIEW_TRADE_ACTS, row, COL_TA_SERIES, userSeries.getRow(0).getId());
      Data.setValue(VIEW_TRADE_ACTS, row, COL_SERIES_NAME,
          userSeries.getString(0, userSeries.getColumnIndex(COL_SERIES_NAME)));
    }
  }

  static void setCommandEnabled(EnablableWidget command, boolean enabled) {
    if (command == null) {
      return;
    }
    command.setEnabled(enabled);
    command.setStyleName(STYLE_COMMAND_DISABLED, !enabled);
  }

  static void setDefaultOperation(IsRow row, TradeActKind kind) {
    Pair<Long, String> operation = getDefaultOperation(kind);
    if (operation != null) {
      Data.setValue(VIEW_TRADE_ACTS, row, COL_TA_OPERATION, operation.getA());
      Data.setValue(VIEW_TRADE_ACTS, row, COL_OPERATION_NAME, operation.getB());

      if (Data.getString(VIEW_TRADE_ACTS, row, COL_STATUS_NAME) == null) {
        Data.setValue(VIEW_TRADE_ACTS, row, COL_STATUS_NAME, tradeActStatus.getB());
        Data.setValue(VIEW_TRADE_ACTS, row, COL_TRADE_STATUS, tradeActStatus.getA());
      }
    }
  }

  private static ViewSupplier createActViewSupplier(final TradeActKind kind) {
    return new ViewSupplier() {
      @Override
      public void create(ViewCallback callback) {
        openActGrid(kind, ViewFactory.getPresenterCallback(callback));
      }
    };
  }

  private static void openActGrid(final TradeActKind kind, final PresenterCallback callback) {
    ensureParameters(new ScheduledCommand() {
      @Override
      public void execute() {
        ensureChache(() -> {
          String supplierKey;
          String caption;
          Filter filter;

          if (kind == null) {
            supplierKey = GRID_ALL_ACTS_KEY;

            if (isClientArea()) {
              caption = BeeUtils.join(" - ", Localized.dictionary().tradeActs(),
                  BeeKeeper.getUser().getCompanyName());
              filter = Filter.equals(COL_TA_COMPANY, BeeKeeper.getUser().getCompany());
            } else {
              caption = Localized.dictionary().tradeActsAll();
              filter = null;
            }

          } else {
            supplierKey = kind.getGridSupplierKey();

            caption = Localized.dictionary().tradeActs() + " - " + kind.getCaption();
            filter = kind.getFilter();
          }

          GridFactory.createGrid(GRID_TRADE_ACTS, supplierKey, new TradeActGrid(kind),
              EnumSet.of(UiOption.GRID), GridOptions.forCaptionAndFilter(caption, filter),
              callback);
        });
      }
    });
  }

  private TradeActKeeper() {
  }
}
