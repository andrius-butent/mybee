package com.butent.bee.client.modules.orders;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.orders.OrdersConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.IntCallback;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.ConfirmationCallback;
import com.butent.bee.client.grid.ChildGrid;
import com.butent.bee.client.layout.TabbedPages;
import com.butent.bee.client.modules.mail.NewMailMessage;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory.WidgetDescriptionCallback;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.validation.CellValidateEvent;
import com.butent.bee.client.validation.CellValidateEvent.Handler;
import com.butent.bee.client.validation.CellValidation;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.view.form.interceptor.PrintFormInterceptor;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BiConsumer;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.IntegerValue;
import com.butent.bee.shared.data.value.NumberValue;
import com.butent.bee.shared.i18n.Dictionary;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.io.FileInfo;
import com.butent.bee.shared.modules.orders.OrdersConstants.OrdersStatus;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderForm extends PrintFormInterceptor {

  private final Dictionary loc = Localized.dictionary();
  private Label warehouseLabel;

  @Override
  public FormInterceptor getInstance() {
    return new OrderForm();
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      WidgetDescriptionCallback callback) {
    if (BeeUtils.same(name, TBL_ORDER_ITEMS)) {
      ((ChildGrid) widget).setGridInterceptor(new OrderItemsGrid());
    } else if (BeeUtils.same(name, COL_WAREHOUSE) && widget instanceof Label) {
      warehouseLabel = (Label) widget;
    }
  }

  @Override
  public void afterRefresh(final FormView form, final IsRow row) {
    Button prepare = new Button(loc.ordPrepare(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        updateStatus(form, OrdersStatus.PREPARED);
        form.setEnabled(true);
        ((ListBox) form.getWidgetBySource(COL_ORDERS_STATUS)).setEditing(false);
        update();
      }
    });

    Button cancel = new Button(loc.ordCancel(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.confirm(loc.ordAskCancel(), new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            updateStatus(form, OrdersStatus.CANCELED);
            save(form);
          }
        });
      }
    });

    Button approve = new Button(loc.ordApprove(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.confirm(loc.ordAskApprove(), new ConfirmationCallback() {
          @Override
          public void onConfirm() {
            String id = row.getString(Data.getColumnIndex(VIEW_ORDERS, COL_WAREHOUSE));
            if (BeeUtils.isEmpty(id)) {
              form.notifySevere(Localized.dictionary().warehouse() + " "
                  + Localized.dictionary().valueRequired());
              return;
            }

            ParameterList params = OrdersKeeper.createSvcArgs(SVC_FILL_RESERVED_REMAINDERS);
            params.addDataItem(COL_ORDER, row.getId());
            params.addDataItem(COL_WAREHOUSE, form.getLongValue(COL_WAREHOUSE));

            BeeKeeper.getRpc().makePostRequest(params, new ResponseCallback() {

              @Override
              public void onResponse(ResponseObject response) {
                if (!response.hasErrors()) {
                  updateStatus(form, OrdersStatus.APPROVED);
                  save(form);
                }
              }
            });
          }
        });
      }
    });

    Button send = new Button(loc.send(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        beforeSendMail();
      }
    });

    Button finish = new Button(loc.crmActionFinish(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.confirm(loc.ordAskFinish(), new ConfirmationCallback() {

          @Override
          public void onConfirm() {
            checkIsFinish(form);
          }
        });
      }
    });

    HeaderView header = form.getViewPresenter().getHeader();
    header.clearCommandPanel();

    final int idxStatus = form.getDataIndex(COL_ORDERS_STATUS);

    if (BeeConst.isUndef(idxStatus)) {
      return;
    }

    Integer status = row.getInteger(idxStatus);

    if (status == null) {
      return;
    }

    GridView parentGrid = getGridView();
    if (DataUtils.isNewRow(row)) {
      if (parentGrid == null) {
        return;
      } else if (parentGrid.getGridName() == VIEW_ORDERS
          && Objects.equals(status, OrdersStatus.PREPARED.ordinal())) {
        updateStatus(form, OrdersStatus.APPROVED);
      }
    }

    if (!Objects.equals(row.getInteger(idxStatus), OrdersStatus.APPROVED.ordinal())) {
      warehouseLabel.setStyleName(StyleUtils.NAME_REQUIRED, false);
    } else {
      warehouseLabel.setStyleName(StyleUtils.NAME_REQUIRED, true);
    }

    boolean isOrder =
        Objects.equals(row.getInteger(idxStatus), OrdersStatus.APPROVED.ordinal())
            || Objects.equals(row.getInteger(idxStatus), OrdersStatus.FINISH.ordinal());

    String caption;

    if (DataUtils.isNewRow(row)) {
      caption = isOrder
          ? Localized.dictionary().newOrder() : Localized.dictionary().newOffer();

      UnboundSelector template = (UnboundSelector) form.getWidgetByName(COL_TEMPLATE);
      template.clearValue();
    } else {
      caption = isOrder
          ? Localized.dictionary().order() : Localized.dictionary().offer();
    }

    if (!BeeUtils.isEmpty(caption)) {
      header.setCaption(caption);
    }

    if (!isOrder && !DataUtils.isNewRow(row)) {

      status = row.getInteger(idxStatus);

      if (Objects.equals(status, OrdersStatus.CANCELED.ordinal())) {
        header.addCommandItem(prepare);
        form.setEnabled(false);
      } else if (Objects.equals(status, OrdersStatus.PREPARED.ordinal())) {
        header.addCommandItem(cancel);
        header.addCommandItem(send);
        header.addCommandItem(approve);
      } else if (Objects.equals(status, OrdersStatus.SENT.ordinal())) {
        header.addCommandItem(cancel);
        header.addCommandItem(approve);
      }
    } else if (Objects.equals(status, OrdersStatus.APPROVED.ordinal())
        && !DataUtils.isNewRow(row)) {
      header.addCommandItem(send);
      header.addCommandItem(finish);
    }

    if (Objects.equals(form.getIntegerValue(COL_ORDERS_STATUS), OrdersStatus.FINISH.ordinal())) {
      form.setEnabled(false);
    }

    if (BeeUtils.isEmpty(row.getString(Data.getColumnIndex(VIEW_ORDERS, COL_SOURCE)))) {
      String gridName =
          "CompanyOrders".equals(parentGrid.getGridName()) ? "Offers" : parentGrid.getGridName();

      row.setValue(Data.getColumnIndex(VIEW_ORDERS, COL_SOURCE), gridName);
      form.getOldRow().setValue(Data.getColumnIndex(VIEW_ORDERS, COL_SOURCE), gridName);
    }

    Widget child = form.getWidgetByName(VIEW_ORDER_CHILD_INVOICES);

    if (child != null) {
      Widget tabs = form.getWidgetByName(NameUtils.getClassName(TabbedPages.class));

      if (tabs != null && tabs instanceof TabbedPages) {
        int idx = ((TabbedPages) tabs).getContentIndex(child);

        if (!BeeConst.isUndef(idx)) {
          if (Objects.equals(OrdersStatus.APPROVED.ordinal(), row.getInteger(idxStatus))
              || Objects.equals(OrdersStatus.FINISH.ordinal(), row.getInteger(idxStatus))) {
            ((TabbedPages) tabs).enablePage(idx);
          } else {
            ((TabbedPages) tabs).disablePage(idx);
          }
        }
      }
    }
  }

  @Override
  public void onLoad(FormView form) {
    form.addCellValidationHandler(COL_WAREHOUSE, new Handler() {

      @Override
      public Boolean validateCell(CellValidateEvent event) {
        CellValidation cv = event.getCellValidation();
        final String newValue = cv.getNewValue();
        String oldValue = cv.getOldValue();

        if (newValue != oldValue
            && oldValue != null
            && DataUtils.hasId(getActiveRow())
            && Objects.equals(getActiveRow().getInteger(Data.getColumnIndex(VIEW_ORDERS,
                COL_ORDERS_STATUS)), OrdersStatus.APPROVED.ordinal())) {

          Global.confirm(Localized.dictionary().ordAskChangeWarehouse() + " "
              + Localized.dictionary().saveChanges(), new ConfirmationCallback() {

            @Override
            public void onConfirm() {
              if (DataUtils.isId(newValue)) {

                Filter filter =
                    Filter.equals(COL_ORDER, getActiveRowId());
                Queries.update(VIEW_ORDER_ITEMS, filter, COL_RESERVED_REMAINDER,
                    new NumberValue(BeeConst.DOUBLE_ZERO), new IntCallback() {

                      @Override
                      public void onSuccess(Integer result) {
                        getActiveRow().setValue(Data.getColumnIndex(VIEW_ORDERS, COL_WAREHOUSE),
                            newValue);
                        update();
                      }
                    });
              }
            }
          });
          return false;
        } else if (!Objects.equals(getActiveRow().getInteger(Data.getColumnIndex(VIEW_ORDERS,
            COL_ORDERS_STATUS)), OrdersStatus.APPROVED.ordinal()) && newValue != oldValue
            && DataUtils.hasId(getActiveRow())) {
          getActiveRow().setValue(Data.getColumnIndex(VIEW_ORDERS, COL_WAREHOUSE),
              newValue);
          update();
          return false;
        }
        return true;
      }
    });
  }

  @Override
  public void onStartNewRow(FormView form, IsRow oldRow, IsRow newRow) {

    GridView parentGrid = getGridView();
    if (parentGrid == null) {
      return;
    } else if ("Offers".equals(parentGrid.getGridName())) {
      int statusIdx = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
      int endDateIdx = Data.getColumnIndex(VIEW_ORDERS, COL_END_DATE);

      if (Objects.equals(OrdersStatus.PREPARED.ordinal(), newRow.getInteger(statusIdx))) {
        DateTime now = TimeUtils.nowMillis();
        int year = now.getYear();
        int month = now.getMonth() + 3;

        if (month > 12) {
          year++;
          month = month - 12;
        }
        newRow.setValue(endDateIdx, new DateTime(year, month, now.getDom()));
      }
    }

    super.onStartNewRow(form, oldRow, newRow);
  }

  @Override
  public boolean beforeAction(Action action, Presenter presenter) {

    if (action.equals(Action.SAVE)) {
      int statusIdx = Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS);
      Long warehouse = getActiveRow().getLong(Data.getColumnIndex(VIEW_ORDERS, COL_WAREHOUSE));
      Long company = getActiveRow().getLong(Data.getColumnIndex(VIEW_ORDERS, COL_COMPANY));
      Integer status = getActiveRow().getInteger(statusIdx);

      if (Objects.equals(status, OrdersStatus.APPROVED.ordinal())) {
        if (!BeeUtils.isPositive(warehouse)) {
          getFormView().notifySevere(Localized.dictionary().warehouse() + " "
              + Localized.dictionary().valueRequired());
          return false;
        }
      }

      if (!BeeUtils.isPositive(company)) {
        getFormView().notifySevere(Localized.dictionary().client() + " "
            + Localized.dictionary().valueRequired());
        return false;
      }
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public FormInterceptor getPrintFormInterceptor() {
    return new PrintOrdersInterceptor(false, OrderForm.this);
  }

  @Override
  public String[] getReportsOld() {
    IsRow row = getActiveRow();

    if (row == null) {
      return null;
    }

    String[] reports = null;
    Integer status = row.getInteger(Data.getColumnIndex(getViewName(), COL_ORDERS_STATUS));

    if (Objects.equals(OrdersStatus.APPROVED.ordinal(), status)
        || Objects.equals(OrdersStatus.FINISH.ordinal(), status)) {
      reports = new String[] {"PrintOrder", "PrintProformaOrder"};
    } else {
      reports = new String[] {"PrintOffer", "PrintOfferApprove"};
    }

    return reports;
  }

  public void sendMail(final FileInfo fileInfo) {
    final FormView form = getFormView();

    List<FileInfo> attach = new ArrayList<>();
    attach.add(fileInfo);

    NewMailMessage.create(BeeUtils.notEmpty(form.getStringValue(ALS_CONTACT_EMAIL),
        form.getStringValue(ALS_COMPANY_EMAIL)), null, null, attach,
        new BiConsumer<Long, Boolean>() {
          @Override
          public void accept(Long messageId, Boolean saveMode) {
            if (!saveMode && !Objects.equals(form.getIntegerValue(COL_ORDERS_STATUS),
                OrdersStatus.APPROVED.ordinal())) {
              updateStatus(form, OrdersStatus.SENT);
            }
          }
        });
  }

  private static void updateStatus(FormView form, OrdersStatus status) {
    form.getActiveRow().setValue(Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS),
        status.ordinal());
    form.refreshBySource(COL_ORDERS_STATUS);
  }

  private void update() {
    FormView form = getFormView();

    BeeRowSet rowSet =
        DataUtils.getUpdated(form.getViewName(), form.getDataColumns(), form.getOldRow(),
            getActiveRow(), form.getChildrenForUpdate());

    Queries.updateRow(rowSet, new RowCallback() {

      @Override
      public void onSuccess(BeeRow result) {
        RowUpdateEvent.fire(BeeKeeper.getBus(), form.getViewName(), result);
        form.refresh();
      }
    });
  }

  private static void save(final FormView form) {
    ScheduledCommand command = new ScheduledCommand() {

      @Override
      public void execute() {
        form.getViewPresenter().handleAction(Action.SAVE);
      }
    };
    command.execute();
  }

  private void beforeSendMail() {
    IsRow row = getActiveRow();

    if (row == null) {
      return;
    }

    final String[] reports = getReportsOld();

    if (reports == null) {
      return;
    }

    final ChoiceCallback choice = new ChoiceCallback() {

      @Override
      public void onSuccess(int value) {
        RowEditor.openForm(reports[value],
            Data.getDataInfo(getFormView().getViewName()), Filter.compareId(row.getId()),
            Opener.MODAL, null, new PrintOrdersInterceptor(true, OrderForm.this));
      }
    };

    if (reports.length > 1) {
      Integer status = row.getInteger(Data.getColumnIndex(VIEW_ORDERS, COL_ORDERS_STATUS));
      final List<String> captions = new ArrayList<>();

      if (Objects.equals(OrdersStatus.APPROVED.ordinal(), status)
          || Objects.equals(OrdersStatus.FINISH.ordinal(), status)) {
        captions.add("Užsakymas");
        captions.add("Išankstinė sąskaita");
      } else {
        captions.add("Pasiūlymas");
        captions.add("Užsakymo patvirtinimas");
      }

      Global.choice(null,
          Localized.dictionary().choosePrintingForm(), captions, new ChoiceCallback() {

            @Override
            public void onSuccess(int value) {
              choice.onSuccess(value);
            }
          });
    } else {
      choice.onSuccess(0);
    }
  }

  private static void checkIsFinish(final FormView form) {

    Filter filter = Filter.equals(COL_ORDER, form.getActiveRowId());
    Queries.getRowSet(VIEW_ORDER_ITEMS, null, filter, new RowSetCallback() {

      @Override
      public void onSuccess(BeeRowSet result) {
        int qtyIdx = Data.getColumnIndex(VIEW_ORDER_ITEMS, TradeConstants.COL_TRADE_ITEM_QUANTITY);

        if (result != null) {
          for (IsRow row : result) {
            Double completed = row.getPropertyDouble(PRP_COMPLETED_INVOICES);
            Double qty = row.getDouble(qtyIdx);

            if (BeeUtils.unbox(completed) <= 0 || !Objects.equals(completed, qty)) {
              form.notifySevere(Localized.dictionary().ordEmptyInvoice());
              return;
            }
          }

          Queries.update(VIEW_ORDER_ITEMS, Filter.equals(COL_ORDER, form.getActiveRowId()),
              COL_RESERVED_REMAINDER, new IntegerValue(0), new IntCallback() {

                @Override
                public void onSuccess(Integer count) {
                  updateStatus(form, OrdersStatus.FINISH);
                  int dateIdx = Data.getColumnIndex(VIEW_ORDERS, COL_END_DATE);
                  form.getActiveRow().setValue(dateIdx, TimeUtils.nowMinutes());

                  save(form);
                }
              });
        }
      }
    });
  }
}
