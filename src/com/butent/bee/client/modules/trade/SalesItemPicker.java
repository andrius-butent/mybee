package com.butent.bee.client.modules.trade;

import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.modules.classifiers.ItemsPicker;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Map;

public class SalesItemPicker extends ItemsPicker {

  private static final String STYLE_STOCK_PREFIX = STYLE_PREFIX + "stock-";
  private static final String STYLE_STOCK_POSITIVE = STYLE_STOCK_PREFIX + "positive";

  @Override
  public void getItems(Filter filter, Queries.RowSetCallback callback) {
    ParameterList params = TradeKeeper.createArgs(SVC_GET_ITEMS_FOR_SELECTION);

    if (DataUtils.hasId(getLastRow())) {
      params.addDataItem(COL_SALE, getLastRow().getId());
    }

    if (DataUtils.isId(getWarehouseFrom())) {
      params.addDataItem(ClassifierConstants.COL_WAREHOUSE, getWarehouseFrom());
    }

    if (filter != null) {
      params.addDataItem(Service.VAR_VIEW_WHERE, filter.serialize());
    }

    params.addDataItem(Service.VAR_TABLE, getSource());

    BeeKeeper.getRpc().makeRequest(params, response -> {
      if (response.hasResponse(BeeRowSet.class)) {
        callback.onSuccess(BeeRowSet.restore(response.getResponseAsString()));
      } else {
        getSpinner().setStyleName(STYLE_SEARCH_SPINNER_LOADING, false);
        BeeKeeper.getScreen().notifyWarning(Localized.dictionary().nothingFound());
      }
    });
  }

  @Override
  public Long getWarehouseFrom(IsRow row) {
    int warehouseIdx = Data.getColumnIndex(VIEW_SALES, COL_TRADE_WAREHOUSE_FROM);
    if (row == null || BeeConst.isUndef(warehouseIdx)) {
      return null;
    }

    return row.getLong(warehouseIdx);
  }

  @Override
  public boolean setIsOrder(IsRow row) {
    return false;
  }

  @Override
  protected String getCaption() {
    return Localized.dictionary().goods();
  }

  @Override
  protected void renderItems(Map<Long, Double> quantities, Map<Long, String> warehouses,
      Flow panel, BeeRowSet itemList) {
    super.renderItems(quantities, warehouses, panel, itemList);

    HtmlTable table = null;

    for (int i = 0; i < panel.getWidgetCount(); i++) {
      Widget w = panel.getWidget(i);
      if (w instanceof HtmlTable && StyleUtils.hasClassName(w.getElement(), STYLE_ITEM_TABLE)) {
        table = (HtmlTable) w;
        break;
      }
    }

    if (table == null) {
      return;
    }

    int r = 0;
    int c = table.getCellCount(r);

    if (table.getWidget(r, c - 1) != null) {
      table.setWidget(r, c, table.getWidget(r, c - 1));
    } else {
      table.setHtml(r, c, table.getCellFormatter().getElement(r, c - 1).getInnerHTML());
    }

    getVisibleTableCols().add(c - 1, ClassifierConstants.COL_EXTERNAL_STOCK);

    table.setText(r, c - 1, Localized.maybeTranslate(itemList.getColumnLabel(
        ClassifierConstants.COL_EXTERNAL_STOCK)));

    r++;
    for (int i = r; i < table.getRowCount(); i++) {
      c = table.getCellCount(i);

      Long itemId = BeeUtils.toLongOrNull(table.getCellFormatter().getElement(i,
          getVisibleTableCols().indexOf(DataUtils.ID_TAG)).getInnerText());

      if (!DataUtils.isId(itemId)) {
        continue;
      }

      if (!itemList.getRowIds().contains(itemId)) {
        continue;
      }

      table.setWidget(i, c, table.getWidget(i, c - 1));

      table.setText(i, c - 1, itemList.getString(itemList.getRowIndex(itemId),
          ClassifierConstants.COL_EXTERNAL_STOCK), STYLE_STOCK_POSITIVE);
    }
  }

  protected String getSource() {
    return TBL_SALE_ITEMS;
  }
}
