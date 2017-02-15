package com.butent.bee.client.modules.cars;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_CURRENCY;
import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.*;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.Global;
import com.butent.bee.client.communication.RpcCallback;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.event.logical.SelectorEvent;
import com.butent.bee.client.i18n.Money;
import com.butent.bee.client.modules.classifiers.ClassifierKeeper;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.DataView;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.interceptor.ParentRowRefreshGrid;
import com.butent.bee.client.widget.CustomAction;
import com.butent.bee.shared.Latch;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfoList;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class CarServiceJobsGrid extends ParentRowRefreshGrid implements SelectorEvent.Handler,
    ClickHandler {

  private final CustomAction addBundle = new CustomAction(FontAwesome.DROPBOX, this);

  @Override
  public void afterCreateEditor(String source, Editor editor, boolean embedded) {
    if (Objects.equals(source, COL_JOB) && editor instanceof DataSelector) {
      ((DataSelector) editor).addSelectorHandler(this);
    }
    super.afterCreateEditor(source, editor, embedded);
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    addBundle.setTitle(Localized.dictionary().bundle());
    presenter.getHeader().addCommandItem(addBundle);
    super.afterCreatePresenter(presenter);
  }

  @Override
  public void onDataSelector(SelectorEvent event) {
    FormView parentForm = ViewHelper.getForm(getGridView());

    if (Objects.isNull(parentForm)) {
      return;
    }
    if (event.isOpened()) {
      event.getSelector().setAdditionalFilter(Filter.or(Filter.isNull(COL_MODEL),
          Filter.equals(COL_MODEL, parentForm.getLongValue(COL_MODEL))));

    } else if (event.isChangePending()) {
      String srcView = event.getRelatedViewName();
      IsRow srcRow = event.getRelatedRow();
      if (srcRow == null) {
        return;
      }
      DataView dataView = ViewHelper.getDataView(event.getSelector());
      if (dataView == null || BeeUtils.isEmpty(dataView.getViewName()) || !dataView.isFlushable()) {
        return;
      }
      IsRow target = dataView.getActiveRow();
      if (target == null) {
        return;
      }
      Map<String, String> values = new HashMap<>();

      values.put(COL_DURATION, BeeUtils.nvl(Data.getString(srcView, srcRow,
          COL_MODEL + COL_DURATION), Data.getString(srcView, srcRow, COL_DURATION)));

      Double price = BeeUtils.nvl(Data.getDouble(srcView, srcRow, COL_MODEL + COL_PRICE),
          Data.getDouble(srcView, srcRow, COL_PRICE));

      Long currency = BeeUtils.nvl(Data.getLong(srcView, srcRow, COL_MODEL + COL_CURRENCY),
          Data.getLong(srcView, srcRow, COL_CURRENCY));

      DateTime mainDate = parentForm.getDateTimeValue(COL_DATE);
      Long mainCurrency = parentForm.getLongValue(COL_CURRENCY);

      if (BeeUtils.allNotNull(price, currency)) {
        price = BeeUtils.round(Money.exchange(currency, mainCurrency, price, mainDate), 2);
      }
      values.put(COL_PRICE, BeeUtils.toStringOrNull(price));
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, null);
      values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT, null);

      Runnable action = () -> values.forEach((col, val) -> {
        target.setValue(dataView.getDataIndex(col), val);
        dataView.refreshBySource(col);
      });
      Long item = Data.getLong(srcView, srcRow, COL_ITEM);

      if (DataUtils.isId(item)) {
        Map<String, Long> options = new HashMap<>();
        options.put(COL_DISCOUNT_COMPANY, parentForm.getLongValue(COL_CUSTOMER));
        options.put(Service.VAR_TIME, mainDate.getTime());
        options.put(COL_DISCOUNT_CURRENCY, mainCurrency);
        options.put(COL_MODEL, parentForm.getLongValue(COL_MODEL));
        options.put(COL_PRODUCTION_DATE, parentForm.getLongValue(COL_PRODUCTION_DATE));
        options.put(COL_JOB, event.getValue());

        ClassifierKeeper.getPriceAndDiscount(item, options, (prc, percent) -> {
          if (BeeUtils.isPositive(prc)) {
            values.put(COL_PRICE, BeeUtils.toString(prc, 2));
          }
          values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT, BeeUtils.toStringOrNull(percent));
          values.put(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT,
              BeeUtils.toString(BeeUtils.isPositive(percent)));
          action.run();
        });
      } else {
        action.run();
      }
    }
  }

  @Override
  public void onClick(ClickEvent clickEvent) {
    getGridView().ensureRelId(parentId -> {
      FormView parentForm = ViewHelper.getForm(getGridView());
      CompoundFilter filter = Filter.and();

      filter.add(Filter.or(Filter.isNull(COL_VALID_UNTIL),
          Filter.isMore(COL_VALID_UNTIL, Value.getValue(TimeUtils.today()))));

      Stream.of(COL_MODEL, COL_CUSTOMER).forEach(col -> {
        Filter subFilter = Filter.isNull(col);

        if (DataUtils.isId(parentForm.getLongValue(col))) {
          subFilter = Filter.or(subFilter, Filter.equals(col, parentForm.getLongValue(col)));
        }
        filter.add(subFilter);
      });
      Relation relation = Relation.create(TBL_CAR_BUNDLES,
          Arrays.asList(COL_CODE, COL_BUNDLE_NAME));
      relation.disableNewRow();
      relation.disableEdit();
      relation.setFilter(filter);
      UnboundSelector selector = UnboundSelector.create(relation);

      selector.addSelectorHandler(event -> {
        Long id = event.getValue();

        if (event.isChanged() && DataUtils.isId(id)) {
          UiHelper.getParentPopup(selector).close();
          addBundle.running();

          Map<String, Filter> views = new HashMap<>();
          views.put(TBL_CAR_BUNDLE_JOBS, Filter.equals(COL_BUNDLE, id));
          views.put(TBL_CAR_BUNDLE_ITEMS, Filter.equals(COL_BUNDLE, id));

          Queries.getData(views.keySet(), views, null, new Queries.DataCallback() {
            @Override
            public void onSuccess(Collection<BeeRowSet> result) {
              RpcCallback<RowInfoList> insertCallback = new RpcCallback<RowInfoList>() {
                long cnt = result.stream().filter(rs -> !rs.isEmpty()).count();

                @Override
                public void onSuccess(RowInfoList res) {
                  if (--cnt == 0) {
                    addBundle.idle();
                    Queries.getRow(parentForm.getViewName(), parentId,
                        RowCallback.refreshRow(parentForm.getViewName(), true));
                  }
                }
              };
              result.stream().filter(rs -> !rs.isEmpty()).forEach(rs -> {
                DataInfo info = Data.getDataInfo(Objects.equals(rs.getViewName(),
                    TBL_CAR_BUNDLE_JOBS) ? TBL_SERVICE_ORDER_JOBS : TBL_SERVICE_ORDER_ITEMS);

                List<BeeColumn> cols = new ArrayList<>();
                cols.add(info.getColumn(COL_SERVICE_ORDER));
                cols.add(info.getColumn(COL_TRADE_DOCUMENT_ITEM_DISCOUNT));
                cols.add(info.getColumn(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT));

                rs.getColumns().stream()
                    .filter(BeeColumn::isEditable)
                    .map(BeeColumn::getId)
                    .filter(info::containsColumn)
                    .forEach(col -> cols.add(info.getColumn(col)));

                BeeRowSet newRs = new BeeRowSet(info.getViewName(), cols);
                Latch latch = new Latch(rs.getNumberOfRows());

                Runnable action = () -> {
                  latch.decrement();
                  if (latch.isOpen()) {
                    Queries.insertRows(newRs, insertCallback);
                  }
                };
                rs.forEach(beeRow -> {
                  BeeRow newRow = newRs.addEmptyRow();

                  for (int i = 0; i < cols.size(); i++) {
                    String col = cols.get(i).getId();

                    switch (col) {
                      case COL_SERVICE_ORDER:
                        newRow.setValue(i, parentId);
                        break;

                      case COL_PRICE:
                        Double price = beeRow.getDouble(rs.getColumnIndex(COL_PRICE));
                        Long currency = beeRow.getLong(rs.getColumnIndex(COL_CURRENCY));

                        if (BeeUtils.allNotNull(price, currency)) {
                          newRow.setValue(i, BeeUtils.round(Money.exchange(currency,
                              parentForm.getLongValue(COL_CURRENCY), price,
                              parentForm.getDateTimeValue(COL_DATE)), 2));
                        }
                        break;

                      case COL_TRADE_DOCUMENT_ITEM_DISCOUNT:
                      case COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT:
                        break;

                      default:
                        newRow.setValue(i, beeRow.getString(rs.getColumnIndex(col)));
                        break;
                    }
                  }
                  Long item = beeRow.getLong(rs.getColumnIndex(COL_ITEM));

                  if (DataUtils.isId(item)) {
                    Map<String, Long> options = new HashMap<>();
                    options.put(COL_DISCOUNT_COMPANY, parentForm.getLongValue(COL_CUSTOMER));
                    options.put(Service.VAR_TIME, parentForm.getLongValue(COL_DATE));
                    options.put(COL_DISCOUNT_CURRENCY, parentForm.getLongValue(COL_CURRENCY));
                    options.put(COL_MODEL, parentForm.getLongValue(COL_MODEL));
                    options.put(COL_PRODUCTION_DATE, parentForm.getLongValue(COL_PRODUCTION_DATE));

                    if (rs.containsColumn(COL_JOB)) {
                      options.put(COL_JOB, beeRow.getLong(rs.getColumnIndex(COL_JOB)));
                    }
                    ClassifierKeeper.getPriceAndDiscount(item, options, (price, percent) -> {
                      if (BeeUtils.isPositive(price)) {
                        newRow.setValue(newRs.getColumnIndex(COL_PRICE), BeeUtils.round(price, 2));
                      }
                      newRow.setValue(newRs.getColumnIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT),
                          percent);
                      newRow.setValue(newRs
                              .getColumnIndex(COL_TRADE_DOCUMENT_ITEM_DISCOUNT_IS_PERCENT),
                          BeeUtils.isPositive(percent));
                      action.run();
                    });
                  } else {
                    action.run();
                  }
                });
              });
            }
          });
        }
      });
      Global.showModalWidget(Localized.dictionary().bundle(), selector);
    });
  }
}
