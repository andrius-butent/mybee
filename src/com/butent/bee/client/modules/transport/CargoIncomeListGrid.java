package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.Queries.RowSetCallback;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.AbstractFormInterceptor;
import com.butent.bee.client.ui.FormFactory.FormInterceptor;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridInterceptor;
import com.butent.bee.client.view.grid.GridView.SelectedRows;
import com.butent.bee.client.widget.Button;
import com.butent.bee.server.modules.commons.ExchangeUtils;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CargoIncomeListGrid extends AbstractGridInterceptor implements ClickHandler {

  private UnboundSelector mainItem;

  @Override
  public GridInterceptor getInstance() {
    return new CargoIncomeListGrid();
  }

  @Override
  public void onClick(ClickEvent event) {
    final GridPresenter presenter = getGridPresenter();
    CompoundFilter flt = CompoundFilter.or();
    final Set<Long> ids = Sets.newHashSet();

    for (RowInfo row : presenter.getGridView().getSelectedRows(SelectedRows.ALL)) {
      flt.add(ComparisonFilter.compareId(row.getId()));
      ids.add(row.getId());
    }
    if (flt.isEmpty()) {
      presenter.getGridView().notifyWarning(Localized.getConstants().selectAtLeastOneRow());
      return;
    }
    Queries.getRowSet(VIEW_CARGO_INCOME_LIST, null, flt, new RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        Set<String> orders = Sets.newHashSet();
        Set<String> vehicles = Sets.newHashSet();
        Set<String> drivers = Sets.newHashSet();

        Map<Long, Pair<String, Integer>> payers = Maps.newHashMap();
        Map<Long, String> customers = Maps.newHashMap();
        Map<Long, String> currencies = Maps.newHashMap();

        boolean itemEmpty = false;

        int item = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, CommonsConstants.COL_ITEM);
        int order = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_ORDER_NO);
        int vehicle = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_VEHICLE);
        int trailer = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_TRAILER);
        int driver = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_DRIVER);
        int custId = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_CUSTOMER);
        int custName = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, COL_CUSTOMER_NAME);
        int currId = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, ExchangeUtils.COL_CURRENCY);
        int currName = Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, ExchangeUtils.COL_CURRENCY
            + ExchangeUtils.COL_CURRENCY_NAME);

        for (BeeRow row : result.getRows()) {
          if (!itemEmpty) {
            itemEmpty = row.getLong(item) == null;
          }
          orders.add(row.getString(order));
          vehicles.add(BeeUtils.join("/", row.getString(vehicle), row.getString(trailer)));
          drivers.add(row.getString(driver));

          String name = null;
          Long id = null;

          for (String fld : new String[] {"Company", "Payer", "Customer"}) {
            name = fld;
            id = row.getLong(Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, name));

            if (DataUtils.isId(id)) {
              break;
            }
          }
          if (DataUtils.isId(id)) {
            payers.put(id,
                Pair.of(row.getString(Data.getColumnIndex(VIEW_CARGO_INCOME_LIST, name + "Name")),
                    row.getInteger(Data.getColumnIndex(VIEW_CARGO_INCOME_LIST,
                        name + "CreditDays"))));
          }
          customers.put(row.getLong(custId), row.getString(custName));

          id = row.getLong(currId);

          if (DataUtils.isId(id)) {
            currencies.put(id, row.getString(currName));
          }
        }
        final boolean mainRequired = itemEmpty;
        final DataInfo turnoversInfo = Data.getDataInfo(VIEW_CARGO_INVOICES);

        BeeRow newRow = RowFactory.createEmptyRow(turnoversInfo, true);

        newRow.setValue(turnoversInfo.getColumnIndex(COL_NUMBER), BeeUtils.joinItems(orders));
        newRow.setValue(turnoversInfo.getColumnIndex(COL_VEHICLE), BeeUtils.joinItems(vehicles));
        newRow.setValue(turnoversInfo.getColumnIndex(COL_DRIVER), BeeUtils.joinItems(drivers));
        newRow.setValue(turnoversInfo.getColumnIndex(TradeConstants.COL_SALE_VAT_INCL), true);

        if (customers.size() == 1) {
          for (Entry<Long, String> entry : customers.entrySet()) {
            newRow.setValue(turnoversInfo.getColumnIndex(COL_CUSTOMER), entry.getKey());
            newRow.setValue(turnoversInfo.getColumnIndex(COL_CUSTOMER_NAME), entry.getValue());
          }
        }
        if (payers.size() == 1) {
          for (Entry<Long, Pair<String, Integer>> entry : payers.entrySet()) {
            if (!Objects.equal(entry.getKey(),
                newRow.getLong(turnoversInfo.getColumnIndex(COL_CUSTOMER)))) {

              newRow.setValue(turnoversInfo.getColumnIndex(COL_PAYER), entry.getKey());
              newRow.setValue(turnoversInfo.getColumnIndex(COL_PAYER_NAME),
                  entry.getValue().getA());
            }
            Integer days = entry.getValue().getB();

            if (BeeUtils.isPositive(days)) {
              newRow.setValue(turnoversInfo.getColumnIndex(TradeConstants.COL_SALE_TERM),
                  TimeUtils.nextDay(newRow.getDateTime(turnoversInfo.getColumnIndex(COL_DATE)),
                      days));
            }
          }
        }
        if (currencies.size() == 1) {
          for (Entry<Long, String> entry : currencies.entrySet()) {
            newRow.setValue(turnoversInfo.getColumnIndex(ExchangeUtils.COL_CURRENCY),
                entry.getKey());
            newRow.setValue(turnoversInfo.getColumnIndex(ExchangeUtils.COL_CURRENCY
                + ExchangeUtils.COL_CURRENCY_NAME), entry.getValue());
          }
        }
        RowFactory.createRow("NewCargoInvoice", null, turnoversInfo, newRow, null,
            new AbstractFormInterceptor() {
              @Override
              public FormInterceptor getInstance() {
                return this;
              }

              @Override
              public void onStart(FormView form) {
                Widget w = form.getWidgetByName("MainItem");

                if (w != null && w instanceof UnboundSelector) {
                  mainItem = (UnboundSelector) w;

                  if (mainRequired) {
                    mainItem.setNullable(false);
                    w = form.getWidgetByName("MainItemCaption");

                    if (w != null) {
                      w.addStyleName(StyleUtils.NAME_REQUIRED);
                    }
                  }
                }
              }
            },
            new RowCallback() {
              @Override
              public void onCancel() {
                mainItem = null;
              }

              @Override
              public void onSuccess(final BeeRow row) {
                ParameterList args = TransportHandler.createArgs(SVC_CREATE_INVOICE_ITEMS);
                args.addDataItem(TradeConstants.COL_SALE, row.getId());
                args.addDataItem(ExchangeUtils.COL_CURRENCY,
                    row.getLong(turnoversInfo.getColumnIndex(ExchangeUtils.COL_CURRENCY)));
                args.addDataItem("IdList", DataUtils.buildIdList(ids));

                if (mainItem != null && DataUtils.isId(mainItem.getRelatedId())) {
                  args.addDataItem(CommonsConstants.COL_ITEM, mainItem.getRelatedId());
                }
                BeeKeeper.getRpc().makePostRequest(args, new ResponseCallback() {
                  @Override
                  public void onResponse(ResponseObject response) {
                    response.notify(presenter.getGridView());

                    if (response.hasErrors()) {
                      return;
                    }
                    CellGrid grid = presenter.getGridView().getGrid();
                    Popup popup = UiHelper.getParentPopup(grid);

                    if (popup != null) {
                      popup.close();
                    } else {
                      grid.reset();
                      presenter.refresh(true);
                    }
                    RowEditor.openRow("CargoInvoice", turnoversInfo, row.getId());
                  }
                });
                mainItem = null;
              }
            });
      }
    });
  }

  @Override
  public void onShow(final GridPresenter presenter) {
    presenter.getHeader().clearCommandPanel();
    presenter.getHeader()
        .addCommandItem(new Button(Localized.getConstants().createInvoice(), this));
  }
}
