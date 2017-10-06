package com.butent.bee.client.modules.trade;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;

import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.DataChangeCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.trade.acts.TradeActKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.classifiers.ItemPrice;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public class TradeItemsGrid extends ParentRowRefreshGrid implements
    SelectionHandler<BeeRowSet> {

  private final ScheduledCommand refresher;
  private SalesItemPicker picker;

  public TradeItemsGrid() {
    this.refresher = createRefresher();
  }

  @Override
  public void afterDeleteRow(long rowId) {
    refresher.execute();
  }

  @Override
  public void afterInsertRow(IsRow result) {
    refresher.execute();
  }

  @Override
  public void afterUpdateCell(IsColumn column, String oldValue, String newValue, IsRow result,
      boolean rowMode) {
    if (BeeUtils.inListSame(column.getId(), COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE,
        COL_TRADE_VAT_PLUS, COL_TRADE_VAT, COL_TRADE_VAT_PERC)) {
      refresher.execute();
    }
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    final IsRow parentRow = ViewHelper.getFormRow(presenter.getMainView());

    if (parentRow != null) {
      ensurePicker().show(parentRow, presenter.getMainView().getElement());
    }

    return false;
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeItemsGrid();
  }

  private ScheduledCommand createRefresher() {
    return new ScheduledCommand() {
      @Override
      public void execute() {
        FormView form = ViewHelper.getForm(getGridView());

        final String viewName = (form == null) ? null : form.getViewName();
        final Long rowId = (form == null) ? null : form.getActiveRowId();

        if (DataUtils.isId(rowId)) {
          Queries.getRow(viewName, rowId, new RowCallback() {
            @Override
            public void onSuccess(BeeRow result) {
              if (result != null) {
                RowUpdateEvent.fire(BeeKeeper.getBus(), viewName, result);
              }
            }
          });
        }
      }
    };
  }

  private SalesItemPicker ensurePicker() {
    if (picker == null) {
      picker = new SalesItemPicker();
      picker.addSelectionHandler(this);
    }

    return picker;
  }

  private Double getDefaultDiscount() {
    IsRow row = getGridView().getActiveRow();
    if (row == null) {
      row = BeeUtils.getLast(getGridView().getRowData());
    }

    return (row == null) ? null : row.getDouble(getDataIndex(COL_TRADE_DISCOUNT));
  }

  @Override
  public void onSelection(SelectionEvent<BeeRowSet> event) {
    addItems(event.getSelectedItem());
  }

  private void addItems(final BeeRowSet rowSet) {
    if (!DataUtils.isEmpty(rowSet) && VIEW_ITEMS.equals(rowSet.getViewName())) {
      getGridView().ensureRelId(result -> {
        IsRow parentRow = ViewHelper.getFormRow(getGridView());

        if (DataUtils.idEquals(parentRow, result)) {
          ItemPrice itemPrice = TradeActKeeper.getItemPrice(VIEW_SALES, parentRow);

          String ip = rowSet.getTableProperty(PRP_ITEM_PRICE);
          if (BeeUtils.isDigit(ip)) {
            itemPrice = EnumUtils.getEnumByIndex(ItemPrice.class, ip);
          }

          DateTime date = Data.getDateTime(VIEW_SALES, parentRow, COL_TA_DATE);
          Long currency = Data.getLong(VIEW_SALES, parentRow, COL_TA_CURRENCY);

          addItems(parentRow, date, currency, itemPrice, getDefaultDiscount(), rowSet);
        }
      });
    }
  }

  private void addItems(IsRow parentRow, DateTime date, Long currency, ItemPrice defPrice,
      Double discount, BeeRowSet items) {

    List<String> colNames = Lists.newArrayList(COL_SALE, COL_ITEM, COL_ITEM_ARTICLE,
        COL_TRADE_ITEM_QUANTITY, COL_TRADE_ITEM_PRICE, COL_TRADE_DISCOUNT, COL_TRADE_VAT,
        COL_TRADE_VAT_PERC);

    BeeRowSet rowSet = new BeeRowSet(getViewName(), Data.getColumns(getViewName(), colNames));

    int saleIndex = rowSet.getColumnIndex(COL_SALE);
    int itemIndex = rowSet.getColumnIndex(COL_TA_ITEM);
    int articleIndex = rowSet.getColumnIndex(COL_ITEM_ARTICLE);
    int qtyIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_QUANTITY);
    int priceIndex = rowSet.getColumnIndex(COL_TRADE_ITEM_PRICE);
    int discountIndex = rowSet.getColumnIndex(COL_TRADE_DISCOUNT);
    int vatIndex = rowSet.getColumnIndex(COL_TRADE_VAT);
    int vatPercIndex = rowSet.getColumnIndex(COL_TRADE_VAT_PERC);

    for (BeeRow item : items) {
      Double qty = BeeUtils.toDoubleOrNull(item.getProperty(PRP_QUANTITY));

      if (BeeUtils.isDouble(qty)) {
        BeeRow row = DataUtils.createEmptyRow(rowSet.getNumberOfColumns());

        row.setValue(saleIndex, parentRow.getId());
        row.setValue(itemIndex, item.getId());
        row.setValue(articleIndex, item.getValue(DataUtils.getColumnIndex(COL_ITEM_ARTICLE,
            items.getColumns())));
        row.setValue(qtyIndex, qty);
        row.setValue(vatIndex, item.getValue(Data.getColumnIndex(VIEW_ITEMS,
            ClassifierConstants.COL_ITEM_VAT_PERCENT)));
        row.setValue(vatPercIndex, item.getBoolean(Data.getColumnIndex(VIEW_ITEMS, COL_ITEM_VAT)));

        ItemPrice itemPrice = defPrice;

        String ip = item.getProperty(PRP_ITEM_PRICE);
        if (BeeUtils.isDigit(ip)) {
          itemPrice = EnumUtils.getEnumByIndex(ItemPrice.class, ip);
        }

        if (itemPrice != null) {
          Double price = item.getDouble(items.getColumnIndex(itemPrice.getPriceColumn()));

          if (BeeUtils.isDouble(price)) {
            if (DataUtils.isId(currency)) {
              Long ic = item.getLong(items.getColumnIndex(itemPrice.getCurrencyColumn()));
              if (DataUtils.isId(ic) && !currency.equals(ic)) {
                price = Money.exchange(ic, currency, price, date);
              }
            }

            row.setValue(priceIndex, Data.round(getViewName(), COL_TRADE_ITEM_PRICE, price));
          }
        }

        if (BeeUtils.nonZero(discount)) {
          row.setValue(discountIndex, discount);
        }

        rowSet.addRow(row);
      }
    }

    if (!rowSet.isEmpty()) {
      Queries.insertRows(rowSet, new DataChangeCallback(rowSet.getViewName()) {
        @Override
        public void onSuccess(RowInfoList result) {
          previewModify(null);
          super.onSuccess(result);
        }
      });
    }
  }
}
