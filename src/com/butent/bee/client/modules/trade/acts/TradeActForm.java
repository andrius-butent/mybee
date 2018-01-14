package com.butent.bee.client.modules.trade.acts;

import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.widget.Label;
import com.butent.bee.shared.Service;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Element;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_BACKGROUND;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.COL_COMPANY_FINANCIAL_STATE;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.HandlesUpdateEvents;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.documents.DocumentConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.modules.trade.acts.TradeActUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Relation.Caching;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;
import com.butent.bee.client.Global;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class TradeActForm extends PrintFormInterceptor implements SelectorEvent.Handler,
    HandlesUpdateEvents {

  private static final String STYLE_PREFIX = TradeActKeeper.STYLE_PREFIX + "form-";

  private static final String STYLE_CREATE = STYLE_PREFIX + "create";
  private static final String STYLE_EDIT = STYLE_PREFIX + "edit";

  private static final String STYLE_HAS_SERVICES = STYLE_PREFIX + "has-services";
  private static final String STYLE_NO_SERVICES = STYLE_PREFIX + "no-services";

  private static final String STYLE_HAS_INVOICES = STYLE_PREFIX + "has-invoices";
  private static final String STYLE_NO_INVOICES = STYLE_PREFIX + "no-invoices";

  private static final String STYLE_HAS_CT_ACTS = STYLE_PREFIX + "has-cta";
  private static final String STYLE_NO_CT_ACTS = STYLE_PREFIX + "no-cta";

  private final Collection<HandlerRegistration> dataHandlerRegistry = new ArrayList<>();

  private TradeActKind lastKind;

  private boolean hasInvoicesOrSecondaryActs;
  private DataSelector contractSelector;
  private DataSelector companySelector;
  private DataSelector objectSelector;

  private ChildGrid tradeActItemsGrid;

  TradeActForm() {
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {

    if (widget instanceof DataSelector
        && BeeUtils.same(editableWidget.getColumnId(), COL_TA_COMPANY)) {
      editableWidget.getRelation().setCaching(Caching.NONE);
    }

    if (BeeUtils.same(editableWidget.getColumnId(), COL_TA_COMPANY)
            && widget instanceof DataSelector) {
      ((DataSelector)  widget).addSelectorHandler(new SelectorEvent.Handler() {
        @Override
        public void onDataSelector(SelectorEvent event) {
          if (event.isChanged() && event.getRelatedRow() != null) {
            ParameterList args = TradeActKeeper.createArgs(SVC_CREDIT_INFO);
            args.addDataItem(ClassifierConstants.COL_COMPANY, event.getRelatedRow().getId());

            BeeKeeper.getRpc().makePostRequest(args, response -> {
              response.notify(getFormView());
              if (response.hasErrors()) {
                return;
              }
              Map<String, String> result = Codec.deserializeLinkedHashMap(
                  response.getResponseAsString());

              double limit = BeeUtils.toDouble(
                  result.get(ClassifierConstants.COL_COMPANY_CREDIT_LIMIT));
              double debt = BeeUtils.toDouble(result.get(TradeConstants.VAR_DEBT));
              double overdue = BeeUtils.toDouble(result.get(TradeConstants.VAR_OVERDUE));
              String financialState = BeeUtils.trim(result.get(COL_COMPANY_FINANCIAL_STATE));
              String financialStateColor = BeeUtils.trim(result.get(COL_BACKGROUND));

              HtmlTable table = new HtmlTable();

              Label label = new Label(financialState);
              StyleUtils.setBackgroundColor(label, financialStateColor);
              table.setWidget(0, 1, label);
              table.setText(0, 0, Localized.dictionary().financialState() + ":");
              table.setText(1, 0, Localized.dictionary().creditLimit() + ":");
              table.setText(1, 1, BeeUtils.joinWords(limit,
                  result.get(AdministrationConstants.COL_CURRENCY)));
              table.setText(2, 0, Localized.dictionary().trdDebt() + ":");
              table.setText(2, 1, String.valueOf(debt));
              table.setText(3, 0, Localized.dictionary().trdOverdue() + ":");
              table.setText(3, 1, String.valueOf(overdue));
              String cap = BeeUtils.joinWords(result.get(ClassifierConstants.COL_COMPANY_NAME),
                      result.get(ClassifierConstants.COL_COMPANY_TYPE));

              Global.showModalWidget(cap, table).hideOnEscape();
            });
          }
        }
      });
    }

    if (widget instanceof DataSelector
      && BeeUtils.same(editableWidget.getColumnId(), COL_TRADE_SERIES)) {
      ((DataSelector) widget).addSelectorHandler(this);
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {

    if (widget instanceof DataSelector && BeeUtils.same(name, WIDGET_TA_CONTRACT)) {
      contractSelector = (DataSelector) widget;
      contractSelector.addSelectorHandler(this);
    }

    if (widget instanceof DataSelector && BeeUtils.same(name, TradeActConstants.COL_TA_OBJECT)) {
      objectSelector = (DataSelector) widget;
      objectSelector.addSelectorHandler(this);
    }

    if (widget instanceof DataSelector && BeeUtils.same(name, TradeActConstants.COL_TA_COMPANY)) {
      companySelector = (DataSelector) widget;
      companySelector.addSelectorHandler(this);
    }

    if (widget instanceof ChildGrid && BeeUtils.same(name, GRID_TRADE_ACT_ITEMS)) {
      tradeActItemsGrid = (ChildGrid) widget;
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterRefresh(FormView form, final IsRow row) {
    TradeActKind kind = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));
    Button commandCompose;

    setFormCaption(form, row);
    setFormStyles(form, row);

    Collection<UnboundSelector> unboundSelectors =
        UiHelper.getChildren(form.asWidget(), UnboundSelector.class);
    if (!BeeUtils.isEmpty(unboundSelectors)) {
      for (UnboundSelector us : unboundSelectors) {
        if (DataUtils.isNewRow(row) || !us.hasRelatedView(VIEW_TRADE_ACT_TEMPLATES)) {
          us.clearValue();
        }
      }
    }

    if (companySelector != null) {
      contractSelector.getOracle().setAdditionalFilter(getRelatedDocumentsFilter(), true);
    }

    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    if (!DataUtils.isNewRow(row)) {
      commandCompose = new Button(
          Localized.dictionary().taInvoiceCompose(),
          arg0 -> FormFactory.openForm(FORM_INVOICE_BUILDER,
              new TradeActInvoiceBuilder(row.getLong(Data.getColumnIndex(VIEW_TRADE_ACTS,
                  COL_TA_COMPANY)), row.getId())));

      if (kind != TradeActKind.RETURN && !DataUtils.isId(Data.getLong(VIEW_TRADE_ACTS, row,
          COL_TA_CONTINUOUS)) && !TradeActKeeper.isClientArea()) {
        header.addCommandItem(commandCompose);
      }
    }

    setEnabledItemsGrid(kind, form, row);

    createReqLabels(form, !TradeActUtils.getMultiReturnData(row).isNull());

    if (!TradeActKeeper.isClientArea() && !header.isActionEnabled(Action.SAVE)) {
      header.showAction(Action.SAVE, true);
    }

    super.afterRefresh(form, row);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {

    if (action.equals(Action.SAVE) && getFormView() != null
        && getActiveRow() != null) {
      FormView form = getFormView();
      IsRow row = getActiveRow();

      Long seriesId = Data.getLong(VIEW_TRADE_ACTS, row, COL_SERIES);
      String seriesName = Data.getString(VIEW_TRADE_ACTS, row, COL_SERIES_NAME);
      if (DataUtils.isId(seriesId) && !BeeUtils.isEmpty(seriesName)) {
        BeeKeeper.getStorage().set(TradeActKeeper.getStorageKey(COL_SERIES), BeeUtils.toString(seriesId));
        BeeKeeper.getStorage().set(TradeActKeeper.getStorageKey(COL_SERIES_NAME), seriesName);
      }

      return validateBeforeSave(form, row, true);
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public void beforeRefresh(FormView form, IsRow row) {
    objectSelector.setEnabled(true);

    super.beforeRefresh(form, row);
  }

  @Override
  public boolean beforeCreateWidget(String name, Element description) {
    if (BeeUtils.same(COL_TA_COMPANY, name)) {
      description.setAttribute("editForm", TradeActKeeper.isClientArea()
          ? ClassifierConstants.FORM_NEW_COMPANY : ClassifierConstants.FORM_COMPANY);
    }

    if (TradeActKeeper.isClientArea() && description.hasAttribute("disablable")) {
     description.setAttribute("disablable", "true");
    }

    return super.beforeCreateWidget(name, description);
  }

  @Override
  public FormInterceptor getInstance() {
    return new TradeActForm();
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintActForm();
  }

  @Override
  public boolean isWidgetEditable(EditableWidget editableWidget, IsRow row) {
    if (editableWidget != null && editableWidget.hasSource(COL_TA_COMPANY) && DataUtils.hasId(row)
        && !row.isNull(getDataIndex(COL_TA_COMPANY))) {

      return !hasInvoicesOrSecondaryActs;

    } else {
      return super.isWidgetEditable(editableWidget, row);
    }
  }

  @Override
  public void onCellUpdate(CellUpdateEvent event) {
  }

  @Override
  public void onLoad(FormView form) {
    dataHandlerRegistry.addAll(BeeKeeper.getBus().registerUpdateHandler(this, false));
    super.onLoad(form);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.setConsumed(!validateBeforeSave(getFormView(), getActiveRow(), false));

    if (!event.isConsumed()) {
      getActiveRow().setProperty(PRP_INSERT_COLS,
          Codec.beeSerialize(DataUtils.getColumnNames(event.getColumns())));
    }
  }

  @Override
  public void onRowUpdate(RowUpdateEvent event) {
    FormView form = getFormView();
    if (form == null) {
      return;
    }

    if (event.hasView(ClassifierConstants.VIEW_COMPANIES)
        && event.getRowId() == BeeUtils.unbox(form
            .getLongValue(COL_TA_COMPANY))) {

      String[][] cols = new String[][] {
          {ClassifierConstants.ALS_COMPANY_NAME, ClassifierConstants.COL_COMPANY_NAME},
          {ALS_CONTACT_PHYSICAL, "Physical"},
          {ClassifierConstants.ALS_COMPANY_TYPE_NAME, ClassifierConstants.ALS_COMPANY_TYPE_NAME}
      };

      IsRow row = form.getActiveRow();
      DataInfo eventData = Data.getDataInfo(event.getViewName());

      for (String[] col : cols) {
        row.setValue(form.getDataIndex(col[0]), event.getRow().getValue(eventData.getColumnIndex(
            col[1])));

        form.refreshBySource(col[0]);
      }
    }
  }

  @Override
  public boolean onStartEdit(final FormView form, final IsRow row,
      final ScheduledCommand focusCommand) {

    hasInvoicesOrSecondaryActs = false;

    if (form != null && DataUtils.hasId(row)) {
      TradeActKind kind = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));

      if (kind != null && kind.enableInvoices()) {
        ParameterList params = TradeActKeeper.createArgs(SVC_HAS_INVOICES_OR_SECONDARY_ACTS);
        params.addQueryItem(COL_TRADE_ACT, row.getId());

        BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            hasInvoicesOrSecondaryActs = response.hasResponse()
                && BeeConst.isTrue(response.getResponseAsString());

            form.updateRow(row, true);
            if (focusCommand != null) {
              focusCommand.execute();
            }
          }
        });

        return false;
      }
    }

    return super.onStartEdit(form, row, focusCommand);
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    if (event.isNewRow()
        && BeeUtils.same(event.getRelatedViewName(), ClassifierConstants.VIEW_COMPANY_OBJECTS)) {

      final BeeRow row = event.getNewRow();
      Data.setColumnReadOnly(ClassifierConstants.VIEW_COMPANY_OBJECTS,
          ClassifierConstants.ALS_COMPANY_NAME);

      Data.setValue(ClassifierConstants.VIEW_COMPANY_OBJECTS, row,
          ClassifierConstants.ALS_COMPANY_NAME,
          BeeUtils.trim(getStringValue(ClassifierConstants.ALS_COMPANY_NAME)));

      Data.setValue(ClassifierConstants.VIEW_COMPANY_OBJECTS, row,
          ClassifierConstants.COL_COMPANY,
          BeeUtils.trim(getStringValue(ClassifierConstants.COL_COMPANY)));

    } else if (BeeUtils.same(event.getRelatedViewName(),
        ClassifierConstants.VIEW_COMPANY_OBJECTS)) {

      TradeActKind actKind = TradeActKeeper.getKind(getActiveRow(), getDataIndex(COL_TA_KIND));

      Queries.getRowCount(VIEW_TRADE_ACTS, Filter.or(Filter.equals(COL_TA_PARENT,
          getActiveRowId()), Filter.equals(COL_TA_CONTINUOUS, getActiveRowId()),
          Filter.equals(COL_TA_RETURN, getActiveRowId())), parentsCount -> {

        Long parentId = getFormView().getLongValue(COL_TA_PARENT);
        Long returnActId = getFormView().getLongValue(COL_TA_RETURN);
        Long continuousActId = getFormView().getLongValue(COL_TA_CONTINUOUS);

        if (DataUtils.isId(parentId) || BeeUtils.isPositive(parentsCount)
            || BeeUtils.isPositive(returnActId) || BeeUtils.isPositive(continuousActId)) {
          objectSelector.setEnabled(false);
        } else if (Objects.equals(TradeActKind.RETURN, actKind)
            || Objects.equals(TradeActKind.CONTINUOUS, actKind)) {
          objectSelector.setEnabled(false);
        }
      });
    }

    if (BeeUtils.same(event.getRelatedViewName(), ClassifierConstants.VIEW_COMPANIES)) {

      if (event.isChanged()) {
        Filter relDocFilter = getRelatedDocumentsFilter();

        Filter relActiveDocFilter = Filter.and(relDocFilter,
            Filter.notNull(DocumentConstants.ALS_STATUS_MAIN));

        DataInfo viewDocuments = Data.getDataInfo(DocumentConstants.VIEW_DOCUMENTS);

        Queries.getRowSet(viewDocuments.getViewName(), viewDocuments.getColumnNames(false),
            relActiveDocFilter, null, new Queries.RowSetCallback() {
              @Override
              public void onSuccess(BeeRowSet result) {
                if (result.getNumberOfRows() != 1) {
                  if (companySelector != null) {
                    contractSelector.clearValue();
                  }
                  return;
                }

                if (companySelector != null) {
                  contractSelector.setSelection(result.getRow(0), null, true);
                }
              }
            });

        if (companySelector != null) {
          contractSelector.getOracle().setAdditionalFilter(relDocFilter, true);
        }
      }
    }

    if (BeeUtils.same(event.getRelatedViewName(), VIEW_TRADE_SERIES)) {
      if (event.isChanged()) {
        Long seriesId = event.getValue();

        if (BeeUtils.isPositive(seriesId)) {
          ParameterList params = TradeActKeeper.createArgs(SVC_GET_NEXT_ACT_NUMBER);

          params.addDataItem(COL_TA_SERIES, seriesId);
          params.addDataItem(Service.VAR_COLUMN, COL_TRADE_NUMBER);
          params.addDataItem(VAR_VIEW_NAME, getViewName());

          BeeKeeper.getRpc().makePostRequest(params, response -> {
            if (!response.hasErrors() && !response.isEmpty()) {

              String number = response.getResponseAsString();
              Data.setValue(getViewName(), getActiveRow(), COL_TRADE_NUMBER, number);
              getFormView().refreshBySource(COL_TRADE_NUMBER);
            }
          });
        }
      }
    }
  }

  @Override
  public void onUnload(FormView form) {
    EventUtils.clearRegistry(dataHandlerRegistry);

    super.onUnload(form);
  }

  boolean validateBeforeSave(FormView form, IsRow row, boolean beforeSave) {
    Long company = null;
    boolean valid = true;
    String regNo = null;
    int idxCompany = form.getDataIndex(COL_TA_COMPANY);
    int idxRegNo = form.getDataIndex(COL_TA_REGISTRATION_NO);
    int idxKind = form.getDataIndex(COL_TA_KIND);
    boolean isNew = DataUtils.isNewRow(row);
    TradeActKind kind = TradeActKeeper.getKind(row, idxKind);

    if (isNew && kind == TradeActKind.RETURN
        && !TradeActUtils.getMultiReturnData(row).isNull() && beforeSave) {
      valid = false;
      form.notifySevere(Localized.dictionary().allValuesEmpty(Localized.dictionary()
          .list(), Localized.dictionary().tradeActItems()));
      return valid;
    }

    if (idxCompany > -1) {
      company = row.getLong(idxCompany);
    }

    if (idxRegNo > -1 && idxKind > -1) {
      regNo = row.getString(idxRegNo);
    }

    if (company != null
        && TradeActKind.RETURN.ordinal() != BeeUtils.unbox(row.getInteger(idxKind))) {
      boolean value = BeeUtils.unbox(row
          .getBoolean(getDataIndex(ALS_CONTACT_PHYSICAL)));
      Long contact = row.getLong(form.getDataIndex(COL_TA_CONTACT));

      if (!value && contact == null) {
        form.notifySevere(Localized.dictionary().contact() + " "
            + Localized.dictionary().valueRequired());
        valid = false;
      } else {
        valid = true;
      }
    }

    if (TradeActKind.RETURN.ordinal() == BeeUtils.unbox(row.getInteger(idxKind))) {
      if (!BeeUtils.isEmpty(regNo)) {
        valid = true;
      } else {
        form.notifySevere(Localized.dictionary().taRegistrationNo() + " "
            + Localized.dictionary().valueRequired());
        valid = false;
      }
    }

    if (valid) {
      valid = createReqFields(form, !TradeActUtils.getMultiReturnData(row).isNull());
    }

    return valid;
  }

  private Filter getRelatedDocumentsFilter() {
    return Filter.in(Data.getIdColumn(DocumentConstants.VIEW_DOCUMENTS),
        DocumentConstants.VIEW_RELATED_DOCUMENTS, DocumentConstants.COL_DOCUMENT, Filter
            .equals(ClassifierConstants.COL_COMPANY, getActiveRow()
                .getString(getDataIndex(COL_TA_COMPANY))));
  }

  private static boolean createReqFields(FormView form, boolean mayDefaults) {

    IsRow row = form.getActiveRow();
    boolean valid;

    if (row == null) {
      return false;
    }

    int kind = row.getInteger(Data.getColumnIndex(VIEW_TRADE_ACTS, COL_TA_KIND));

    Widget w = form.getWidgetByName("label" + COL_TA_NAME);
    valid = (mayDefaults && StyleUtils.hasClassName(w.getElement(), "bee-hasDefaults"))
        || DataUtils.isId(row.getLong(form.getDataIndex(COL_TA_NAME)));

    if (!valid) {
      form.notifySevere(Data.getColumnLabel(VIEW_TRADE_ACTS, COL_TA_NAME) + " "
          + Localized.dictionary().valueRequired());
      return valid;
    }

    String[] fields = EnumUtils.getEnumByIndex(TradeActKind.class, kind).getReqFields();


    if (fields != null) {
      for (String field : fields) {
          String value = row.getString(Data.getColumnIndex(VIEW_TRADE_ACTS, field));
          w = form.getWidgetByName("label" + field);
          valid = !BeeUtils.isEmpty(value) || (mayDefaults && StyleUtils.hasClassName(w
              .getElement(), "bee-hasDefaults"));
          if (!valid) {
            form.notifySevere(Data.getColumnLabel(VIEW_TRADE_ACTS, field) + " "
                + Localized.dictionary().valueRequired());
            return valid;
          }
      }
    }
    return valid;
  }

  private static void createReqLabels(FormView form, boolean markAsDefaults) {
    IsRow row = form.getActiveRow();
    Widget wid;

    if (row == null) {
      return;
    }

    int kind = row.getInteger(Data.getColumnIndex(VIEW_TRADE_ACTS, COL_TA_KIND));

    String[] fields = EnumUtils.getEnumByIndex(TradeActKind.class, kind).getReqFields();

    if (fields != null) {
      for (String field : fields) {
        wid = form.getWidgetByName("label" + field, true);

        if (wid == null) {
          continue;
        }

        wid.addStyleName("bee-required");

        if (BeeUtils.same(field, COL_TA_DATE) && TradeActKind.RETURN.ordinal() == kind) {
          wid.removeStyleName("bee-hasDefaults");
        }
      }
    }

    if (!markAsDefaults) {
      return;
    }

    for (String field : VAR_COPY_TA_COLUMN_NAMES) {
      if (!BeeUtils.isEmpty(form.getStringValue(field))
          || BeeUtils.inListSame(field, COL_TA_INPUT_DRIVER, COL_TA_INPUT_VEHICLE)) {
        continue;
      }

      wid = form.getWidgetByName("label" + field, true);

      if (wid == null) {
        continue;
      }

      wid.addStyleName("bee-hasDefaults");
    }

    wid = form.getWidgetByName("labelNumber", true);
    if (wid != null) {
      wid.addStyleName("bee-hasDefaults");
    }
    wid.addStyleName("bee-hasDefaults");


  }

  private void setEnabledItemsGrid(TradeActKind kind, FormView form, IsRow row) {
    if (tradeActItemsGrid == null) {
      return;
    }

    tradeActItemsGrid.setEnabled(TradeActKeeper.isEnabledItemsGrid(kind, form, row));
  }

  private void setFormCaption(FormView form, IsRow row) {
    TradeActKind kind = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));

    String caption;

    if (DataUtils.isNewRow(row)) {
      form.removeStyleName(STYLE_EDIT);
      form.addStyleName(STYLE_CREATE);

      caption = BeeUtils.join(" - ", Localized.dictionary().tradeActNew(),
          (kind == null) ? null : kind.getCaption());

    } else {
      form.removeStyleName(STYLE_CREATE);
      form.addStyleName(STYLE_EDIT);

      caption = (kind == null) ? Localized.dictionary().tradeAct() : kind.getCaption();
    }

    if (form.getViewPresenter() != null && form.getViewPresenter().getHeader() != null) {
      form.getViewPresenter().getHeader().setCaption(caption);
    }
  }

  private void setFormStyles(FormView form, IsRow row) {
    TradeActKind kind = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));
    if (lastKind != kind) {
      if (lastKind != null) {
        form.removeStyleName(STYLE_PREFIX + lastKind.getStyleSuffix());
      }
      if (kind != null) {
        form.addStyleName(STYLE_PREFIX + kind.getStyleSuffix());
      }

      boolean hasServices = kind != null && kind.enableServices();
      form.setStyleName(STYLE_HAS_SERVICES, hasServices);
      form.setStyleName(STYLE_NO_SERVICES, !hasServices);

      boolean hasInvoices = kind != null && kind.enableInvoices();
      form.setStyleName(STYLE_HAS_INVOICES, hasInvoices);
      form.setStyleName(STYLE_NO_INVOICES, !hasInvoices);

      boolean isCta = kind == TradeActKind.CONTINUOUS;
      form.setStyleName(STYLE_HAS_CT_ACTS, isCta);
      form.setStyleName(STYLE_NO_CT_ACTS, !isCta);

      if (form.getWidgetByName(GRID_TRADE_ACTS) instanceof ChildGrid) {
        ChildGrid g = (ChildGrid) form.getWidgetByName(GRID_TRADE_ACTS);
        g.setEnabled(false);

      }
      lastKind = kind;
    }
  }

}
