package com.butent.bee.client.modules.trade.acts;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.GwtEvent;

import static com.butent.bee.shared.modules.trade.acts.TradeActConstants.*;
import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.dialog.Icon;
import com.butent.bee.client.dialog.MessageBoxes;
import com.butent.bee.client.dialog.Modality;
import com.butent.bee.client.dialog.StringCallback;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.ActiveRowChangeEvent;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.client.view.search.ListFilterSupplier;
import com.butent.bee.client.widget.Button;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterComponent;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.modules.trade.acts.TradeActKind;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.StringList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TradeActGrid extends AbstractGridInterceptor {

  private static final BeeLogger logger = LogUtils.getLogger(TradeActGrid.class);

  private final TradeActKind kind;

  private Button supplementCommand;
  private Button returnCommand;
  private Button alterCommand;
  private Button copyCommand;
  private Button templateCommand;

  private TradeActKind newActKind;

  TradeActGrid(TradeActKind kind) {
    this.kind = kind;
  }

  @Override
  public void afterCreatePresenter(GridPresenter presenter) {
    GridView gridView = presenter.getGridView();

    if (gridView != null && !gridView.isReadOnly()) {
      boolean canCreateActs = BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACTS);

      if (canCreateActs) {
        presenter.getHeader().addCommandItem(ensureSupplementCommand());
        presenter.getHeader().addCommandItem(ensureReturnCommand());
      }

      presenter.getHeader().addCommandItem(ensureAlterCommand());

      if (canCreateActs) {
        presenter.getHeader().addCommandItem(ensureCopyCommand());
      }
      if (BeeKeeper.getUser().canCreateData(VIEW_TRADE_ACT_TEMPLATES)) {
        presenter.getHeader().addCommandItem(ensureTemplateCommand());
      }
    }

    super.afterCreatePresenter(presenter);
  }

  @Override
  public void onReadyForInsert(final GridView gridView, final ReadyForInsertEvent event) {
    IsRow row = gridView.getActiveForm().getActiveRow();
    checkContract(row, event, gridView);
  }

  @Override
  public void onSaveChanges(GridView gridView, SaveChangesEvent event) {
    if (event.isEmpty()) {
      return;
    }

    IsRow row = event.getNewRow();
    checkContract(row, event, gridView);
  }

  @Override
  public boolean beforeAction(Action action, GridPresenter presenter) {
    if (TradeActKeeper.isClientArea() && Action.AUDIT == action) {
      return false;
    }
    return super.beforeAction(action, presenter);
  }

  @Override
  public boolean beforeAddRow(GridPresenter presenter, boolean copy) {
    newActKind = kind;

    if (kind == null) {
      final List<TradeActKind> kinds = Lists.newArrayList(TradeActKind.SALE, TradeActKind.TENDER,
          TradeActKind.PURCHASE, TradeActKind.WRITE_OFF, TradeActKind.RESERVE);

      List<String> options = new ArrayList<>();
      for (TradeActKind k : kinds) {
        options.add(k.getCaption());
      }

      Global.choice(Localized.dictionary().tradeActNew(), null, options, value -> {
        if (BeeUtils.isIndex(kinds, value)) {
          newActKind = kinds.get(value);
          getGridView().startNewRow(false);
        }
      });

      return false;

    } else {
      return super.beforeAddRow(presenter, copy);
    }
  }

  @Override
  public DeleteMode beforeDeleteRow(GridPresenter presenter, IsRow row) {
    revertStatuses(row.getId());
    return DeleteMode.CANCEL;
  }

  @Override
  public DeleteMode beforeDeleteRows(GridPresenter presenter, IsRow activeRow,
      Collection<RowInfo> selectedRows) {
    List<Long> ids = new ArrayList<>(selectedRows.size());

    selectedRows.forEach(info -> ids.add(info.getId()));

    revertStatuses(ids.toArray(new Long[ids.size()]));
    return DeleteMode.CANCEL;
  }

  @Override
  public List<FilterComponent> getInitialUserFilters(List<FilterComponent> defaultFilters) {
    if (TradeActKeeper.isClientArea()) {
      return super.getInitialUserFilters(defaultFilters);
    }

    FilterComponent seriesFilter = getInitialSeriesFilter();
    FilterComponent statusFilter = getInitialStatusFilter();

    if (seriesFilter == null && statusFilter == null) {
      return super.getInitialUserFilters(defaultFilters);
    }

    List<FilterComponent> result = new ArrayList<>();
    if (!BeeUtils.isEmpty(defaultFilters)) {
      for (FilterComponent component : defaultFilters) {
        if (component != null) {
          result.add(component);

          if (BeeUtils.same(component.getName(), COL_TA_SERIES)) {
            seriesFilter = null;
          } else if (BeeUtils.same(component.getName(), COL_TA_STATUS)) {
            statusFilter = null;
          }
        }
      }
    }

    if (seriesFilter != null) {
      result.add(seriesFilter);
    }
    if (statusFilter != null) {
      result.add(statusFilter);
    }

    return result;
  }

  private static FilterComponent getInitialSeriesFilter() {
    BeeRowSet series = TradeActKeeper.getUserSeries(false);

    if (DataUtils.isEmpty(series)) {
      return null;

    } else {
      List<String> items = new ArrayList<>();
      for (BeeRow row : series) {
        items.add(BeeUtils.toString(row.getId()));
      }

      return new FilterComponent(COL_TA_SERIES, ListFilterSupplier.buildValue(items));
    }
  }

  private FilterComponent getInitialStatusFilter() {
    if (kind != null && !kind.enableReturn()) {
      return null;
    }

    Long returnedActStatus = TradeActKeeper.getReturnedActStatus();
    if (!DataUtils.isId(returnedActStatus)) {
      return null;
    }

    BeeRowSet statuses = kind == null ? null : TradeActKeeper.getStatuses();
    if (DataUtils.isEmpty(statuses)) {
      return null;
    }

    List<String> items = new ArrayList<>();
    for (BeeRow row : statuses) {
      if (!returnedActStatus.equals(row.getId())) {
        items.add(BeeUtils.toString(row.getId()));
      }
    }

    items.add(null);
    return new FilterComponent(COL_TA_STATUS, ListFilterSupplier.buildValue(items));
  }

  @Override
  public GridInterceptor getInstance() {
    return new TradeActGrid(kind);
  }

  @Override
  public void onActiveRowChange(ActiveRowChangeEvent event) {
    refreshCommands(event.getRowValue());
    super.onActiveRowChange(event);
  }

  @Override
  public boolean onStartNewRow(GridView gridView, IsRow oldRow, IsRow newRow, boolean copy) {
    TradeActKeeper.prepareNewTradeAct(newRow, newActKind);

    return super.onStartNewRow(gridView, oldRow, newRow, copy);
  }

  private Button ensureCopyCommand() {
    if (copyCommand == null) {
      copyCommand = new Button(Localized.dictionary().actionCopy(),
          event -> {
            final IsRow row = getGridView().getActiveRow();
            TradeActKind tak = TradeActKeeper.getKind(getViewName(), row);

            if (tak != null) {
              List<String> messages = new StringList();

              messages.add(row.getString(getDataIndex(COL_TRADE_ACT_NAME)));

              String number = row.getString(getDataIndex(COL_TA_NUMBER));
              if (!BeeUtils.isEmpty(number)) {
                messages.add(BeeUtils.joinWords(
                    row.getString(getDataIndex(TradeConstants.COL_SERIES_NAME)), number));
              }

              messages.add(Localized.dictionary().tradeActCopyQuestion());

              Global.confirm(tak.getCaption(), Icon.QUESTION, messages,
                  Localized.dictionary().actionCopy(), Localized.dictionary().actionCancel(),
                  () -> {
                    if (DataUtils.sameId(row, getGridView().getActiveRow())) {
                      doCopy(row.getId());
                      Data.resetLocal(VIEW_TRADE_ACTS);
                    }
                  });
            }
          });

      TradeActKeeper.addCommandStyle(copyCommand, "copy");
      TradeActKeeper.setCommandEnabled(copyCommand, false);
    }
    return copyCommand;
  }

  private void doCopy(long id) {
    ParameterList params = TradeActKeeper.createArgs(SVC_COPY_ACT);
    params.addQueryItem(COL_TRADE_ACT, id);

    BeeKeeper.getRpc().makeGetRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (Queries.checkRowResponse(SVC_COPY_ACT, getViewName(), response)) {
          BeeRow row = BeeRow.restore(response.getResponseAsString());
          BeeRow newRow = DataUtils.cloneRow(row);
          TradeActKeeper.setDefaultOperation(newRow, TradeActKeeper.getKind(VIEW_TRADE_ACTS, row));

          DataInfo tradeActsView = Data.getDataInfo(VIEW_TRADE_ACTS);

          Queries.update(VIEW_TRADE_ACTS, tradeActsView.getColumns(), row, newRow, null,
              new RowCallback() {
                @Override
                public void onSuccess(BeeRow updatedRow) {
                  RowUpdateEvent.fire(BeeKeeper.getBus(), VIEW_TRADE_ACTS, updatedRow);

                  GridView gridView = getGridView();

                  if (gridView != null && gridView.asWidget().isAttached()) {
                    gridView.ensureRow(updatedRow, true);
                    maybeOpenAct(gridView, updatedRow);
                  }
                }
              });
        }
      }
    });
  }

  private Button ensureReturnCommand() {
    if (returnCommand == null) {
      returnCommand = new Button(Localized.dictionary().taKindReturn(),
          event -> {
            final List<IsRow> rows = new ArrayList<>();
            int idxObject = getGridView().getDataIndex(COL_TA_OBJECT);
            int idxSeries = getGridView().getDataIndex(COL_TA_SERIES);
            int idxCompany = getGridView().getDataIndex(COL_TA_COMPANY);

            IsRow activeRow = null;
            TradeActKind rowKind = null;
//            TradeActKind rowKind = TradeActKeeper.getKind(getViewName(), activeRow);
            boolean mixedReturn = false;

            for (IsRow row : getGridView().getRowData()) {
              if (activeRow == null && getGridView().isRowSelected(row.getId())) {
                activeRow = row;
                rowKind = TradeActKeeper.getKind(getViewName(), activeRow);

                if (rowKind == null || !rowKind.enableReturn()) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }
              } else if (activeRow == null) {
                continue;
              }

              if (getGridView().isRowSelected(row.getId()) || DataUtils.sameId(row, activeRow)) {
                if (BeeUtils.compare(activeRow.getLong(idxObject), row.getLong(idxObject),
                    null) != BeeConst.COMPARE_EQUAL) {
                  getGridView().notifyWarning(Localized.dictionary().taObjectsIsDifferent());
                  return;
                }

                if (BeeUtils.compare(activeRow.getLong(idxSeries), row.getLong(idxSeries),
                    null) != BeeConst.COMPARE_EQUAL
                    || BeeUtils.compare(activeRow.getLong(idxCompany), row.getLong(idxCompany),
                    null) != BeeConst.COMPARE_EQUAL) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }

                TradeActKind taKind = TradeActKeeper.getKind(getViewName(), row);


                if (!rowKind.enableMultiReturn(taKind)) {
                  getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
                  return;
                }

                if (taKind != null && taKind.enableReturn()) {
                  mixedReturn |= taKind != rowKind;
                  rows.add(row);
                }
              }
            }

            if (rowKind == TradeActKind.CONTINUOUS && !mixedReturn) {
              multiReturnContinuousAct(rows);
            } else if (rows.size() == 1) {
              checkItemsBeforeReturn(rows.get(0));
            } else if (rows.size() > 1 && !mixedReturn) {
              multiReturn(rows);
            } else if (mixedReturn) {
              multiMixedReturn(rows);
            } else {
              getGridView().notifyWarning(Localized.dictionary().selectAtLeastOneRow());
            }

            Data.resetLocal(VIEW_TRADE_ACTS);
          });

      TradeActKeeper.addCommandStyle(returnCommand, "return");
      TradeActKeeper.setCommandEnabled(returnCommand, false);
    }
    return returnCommand;
  }

  private Button ensureAlterCommand() {
    if (alterCommand == null) {
      alterCommand = new Button(Localized.dictionary().taAlterKind(),
          event -> {
            IsRow row = getGridView().getActiveRow();
            if (row != null) {
              alterKind(row);
            }
          });

      TradeActKeeper.addCommandStyle(alterCommand, "alter");
      TradeActKeeper.setCommandEnabled(alterCommand, false);
    }
    return alterCommand;
  }

  private void alterKind(IsRow row) {
    TradeActKind tak = TradeActKeeper.getKind(getViewName(), row);

    if (tak != null && tak.enableAlter()) {
      final List<TradeActKind> targets = new ArrayList<>();
      List<String> choices = new ArrayList<>();

      for (TradeActKind target : TradeActKind.values()) {
        if (target.isAlterTarget() && target != tak) {
          targets.add(target);
          choices.add(target.getCaption());
        }
      }

      final long actId = row.getId();

      MessageBoxes.choice(Localized.dictionary().taAlterKind(),
          BeeUtils.joinWords(tak.getCaption(), row.getString(getDataIndex(COL_TRADE_ACT_NAME))),
          choices, value -> {
            if (DataUtils.idEquals(getGridView().getActiveRow(), actId)
                && BeeUtils.isIndex(targets, value)) {

              ParameterList params = TradeActKeeper.createArgs(SVC_ALTER_ACT_KIND);
              params.addQueryItem(COL_TRADE_ACT, actId);
              params.addQueryItem(COL_TA_KIND, targets.get(value).ordinal());

              BeeKeeper.getRpc().makeRequest(params, response -> {
                if (response.hasResponse(BeeRow.class)) {
                  BeeRow updated = BeeRow.restore(response.getResponseAsString());
                  RowUpdateEvent.fire(BeeKeeper.getBus(), getViewName(), updated);

                  GridView gridView = getGridView();

                  if (gridView != null && gridView.asWidget().isAttached()) {
                    if (DataUtils.idEquals(gridView.getActiveRow(), actId)) {
                      refreshCommands(updated);
                    }
                    maybeOpenAct(gridView, updated);
                  }
                }
                Data.resetLocal(VIEW_TRADE_ACTS);
              });
            }
          }, BeeConst.UNDEF, BeeConst.UNDEF, Localized.dictionary().cancel(), null);
    }
  }

  private void checkItemsBeforeReturn(final IsRow parent) {
    final TradeActKind parentKind = TradeActKeeper.getKind(VIEW_TRADE_ACTS, parent);

    if (parentKind == null || !parentKind.enableReturn()) {
      getGridView().notifyWarning(Localized.dictionary().taIsDifferent());
      return;
    }

    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_RETURN);
    params.addQueryItem(COL_TRADE_ACT, parent.getId());
    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          createReturn(parent);
        } else {
          getGridView().notifyWarning(Localized.dictionary().noData());
        }
      }
    });

  }

  private void createReturn(IsRow parent) {
    createReturn(parent, null, null);
  }

  private void createReturn(BeeRowSet parentActs, BeeRowSet parentItems) {
    createReturn(null, parentActs, parentItems);
  }

  private void createReturn(final IsRow parent, BeeRowSet parentActs, BeeRowSet parentItems) {

    if (!DataUtils.isEmpty(parentActs) && !DataUtils.isEmpty(parentItems)) {
      createReturnActForm(parentActs, parentItems);
      return;
    } else if (parent == null) {
      Assert.untouchable();
    }

    ParameterList prm = TradeActKeeper.createArgs(SVC_GET_NEXT_RETURN_ACT_NUMBER);

    if (DataUtils.isId(parent.getLong(getDataIndex(COL_TA_SERIES)))) {
      prm.addDataItem(COL_TA_SERIES, parent.getLong(getDataIndex(COL_TA_SERIES)));
    }

    prm.addDataItem(TradeConstants.VAR_VIEW_NAME, getViewName());
    prm.addDataItem(Service.VAR_COLUMN, COL_TA_NUMBER);
    prm.addDataItem(COL_TA_PARENT, parent.getId());

    BeeKeeper.getRpc().makePostRequest(prm, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        createReturnActForm(parent, response.getResponseAsString());
      }
    });
  }

  private void createReturnActForm(BeeRowSet parentActs,  BeeRowSet parentItems) {
    createReturnActForm(null, null, parentActs, parentItems);
  }

  private void createReturnActForm(IsRow parent, String number) {
    createReturnActForm(parent, number, null, null);
  }

  private void createReturnActForm(IsRow parent, String number, BeeRowSet parentActs,
      BeeRowSet parentItems) {
    DataInfo viewTradeActs = Data.getDataInfo(getViewName());
    BeeRow newRow = RowFactory.createEmptyRow(viewTradeActs, true);

    for (int i = 0; i < getDataColumns().size(); i++) {
      String colId = getDataColumns().get(i).getId();

      switch (colId) {
        case COL_TA_KIND:
          newRow.setValue(i, TradeActKind.RETURN.ordinal());
          break;

        case COL_TA_DATE:
          newRow.setValue(i, (DateTime) null);
          // ID 29613
//          if (parent == null) {
//            newRow.setValue(i, (DateTime) null);
//            break;
//          }
//          newRow.setValue(i, TimeUtils.nowMinutes());
          break;

        case COL_TA_PARENT:
          if (parent == null) {
            break;
          }
          newRow.setValue(i, parent.getId());
          break;

        case COL_TA_NUMBER:
          if (parent == null || BeeUtils.isEmpty(number)) {
            break;
          }
          newRow.setValue(i, parent.getString(i) + "-"
              + number);
          break;

        case COL_TA_SERIES:
        case TradeConstants.COL_SERIES_NAME:
          if (parentActs != null) {
            newRow.setValue(i, parentActs.getString(0, i));
          } else if (parent != null) {
            newRow.setValue(i, parent.getString(i));
          }
          break;

        case COL_TA_COMPANY:
        case COL_TA_CONTRACT:
        case COL_TA_CONTACT:
        case COL_TA_OBJECT:
          if (parentActs != null) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                parentActs.getRow(0), false);
            }
            newRow.setValue(i, parentActs.getString(0, i));
          } else if (parent != null) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                parent, false);
            }
            newRow.setValue(i, parent.getString(i));
          }
          break;
        case COL_TA_UNTIL:
        case COL_TA_NOTES:
        case COL_TA_RETURN:
        case COL_TA_CONTINUOUS:
        case ALS_RETURNED_COUNT:
        case COL_TA_INPUT_DRIVER:
        case COL_TA_INPUT_VEHICLE:
        case COL_TA_MANAGER:
        case "ManagerPerson":
          break;

        default:
          if (parent != null && !parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)
              && !colId.startsWith(COL_TA_OPERATION)) {
            if (viewTradeActs.hasRelation(colId)) {
              RelationUtils.updateRow(viewTradeActs, colId, newRow, viewTradeActs,
                parent, false);
            }

            if (viewTradeActs.getColumn(colId).getLevel() == 0) {
              newRow.setValue(i, parent.getString(i));
            }
          }
      }
    }

    if (!DataUtils.isEmpty(parentActs) && !DataUtils.isEmpty(parentItems)) {
      newRow.setProperty(PRP_MULTI_RETURN_DATA, Codec.beeSerialize(Pair.of(parentActs,
          parentItems)));
    }

    TradeActKeeper.setDefaultOperation(newRow, TradeActKind.RETURN);

    RowFactory.createRow(viewTradeActs, newRow, Modality.ENABLED, new RowCallback() {
      @Override
      public void onSuccess(BeeRow result) {
        getGridView().ensureRow(result, true);
        if (parent != null) {
          Queries.getRow(VIEW_TRADE_ACTS, parent.getId(), new RowCallback() {
            @Override
            public void onSuccess(BeeRow updatedParent) {
              DataInfo info = Data.getDataInfo(VIEW_TRADE_ACTS);
              RowEditor.openForm(info.getEditForm(), info, Filter.compareId(updatedParent.getId()),
                  Opener.modeless());
            }
          });
        }
      }
    });
  }

  private void multiReturnContinuousAct(final Collection<IsRow> ctActs) {
    DataInfo taInfo = Data.getDataInfo(VIEW_TRADE_ACTS);

    Queries.getRowSet(VIEW_TRADE_ACTS, taInfo.getColumnNames(false),
        Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TA_PARENT,
            Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(ctActs))),
        new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            List<IsRow> rows = new ArrayList<>();
            rows.addAll(result.getRows());
            multiReturn(rows);
          }
        });
  }

  private void multiMixedReturn(final Collection<IsRow> mixedActs) {
    DataInfo taInfo = Data.getDataInfo(VIEW_TRADE_ACTS);

    Queries.getRowSet(VIEW_TRADE_ACTS, taInfo.getColumnNames(false),
        Filter.or(
            Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TA_PARENT,
                Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(mixedActs))),
            Filter.in(Data.getIdColumn(VIEW_TRADE_ACTS), VIEW_TRADE_ACT_ITEMS, COL_TRADE_ACT,
                Filter.and(
                    Filter.any(COL_TRADE_ACT, DataUtils.getRowIds(mixedActs)),
                    Filter.isNull(COL_TA_PARENT)
                )
            )
        ), new Queries.RowSetCallback() {
          @Override
          public void onSuccess(BeeRowSet result) {
            List<IsRow> rows = new ArrayList<>();
            rows.addAll(result.getRows());
            multiReturn(rows);
          }
        });

  }

  private void multiReturn(final Collection<IsRow> parents) {
    ParameterList params = TradeActKeeper.createArgs(SVC_GET_ITEMS_FOR_MULTI_RETURN);
    params.addQueryItem(Service.VAR_LIST, DataUtils.buildIdList(DataUtils.getRowIds(parents)));

    BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasResponse(BeeRowSet.class)) {
          BeeRowSet parentItems = BeeRowSet.restore(response.getResponseAsString());

          BeeRowSet parentActs = Data.createRowSet(getViewName());
          for (IsRow parent : parents) {
            parentActs.addRow(DataUtils.cloneRow(parent));
          }
          createReturn(parentActs, parentItems);
        } else {
          getGridView().notifyWarning(Localized.dictionary().noData());
        }
      }
    });
  }

  private Button ensureSupplementCommand() {
    if (supplementCommand == null) {
      supplementCommand = new Button(Localized.dictionary().taKindSupplement(),
          event -> {
            IsRow row = getGridView().getActiveRow();
            if (row != null) {
              createSupplement(row);
              Data.resetLocal(VIEW_TRADE_ACTS);
            }
          });

      TradeActKeeper.addCommandStyle(supplementCommand, "supplement");
      TradeActKeeper.setCommandEnabled(supplementCommand, false);
    }
    return supplementCommand;
  }

  private void createSupplement(IsRow parent) {
    ParameterList prm = TradeActKeeper.createArgs(SVC_GET_NEXT_CHILD_ACT_NUMBER);

    if (DataUtils.isId(parent.getLong(getDataIndex(COL_TA_SERIES)))) {
      prm.addDataItem(COL_TA_SERIES, parent.getLong(getDataIndex(COL_TA_SERIES)));
    }

    prm.addDataItem(TradeConstants.VAR_VIEW_NAME, getViewName());
    prm.addDataItem(Service.VAR_COLUMN, COL_TA_NUMBER);
    prm.addDataItem(COL_TA_PARENT, parent.getId());
    prm.addDataItem(COL_TA_KIND, TradeActKind.SUPPLEMENT.ordinal());

    BeeKeeper.getRpc().makeRequest(prm, new ResponseCallback() {

      @Override
      public void onResponse(ResponseObject response) {
        DataInfo dataInfo = Data.getDataInfo(getViewName());
        BeeRow newRow = RowFactory.createEmptyRow(dataInfo, false);

        for (int i = 0; i < getDataColumns().size(); i++) {
          String colId = getDataColumns().get(i).getId();

          switch (colId) {
            case COL_TA_KIND:
              newRow.setValue(i, TradeActKind.SUPPLEMENT.ordinal());
              break;

            case COL_TA_DATE:
              newRow.setValue(i, TimeUtils.nowMinutes());
              break;

            case COL_TA_PARENT:
              newRow.setValue(i, parent.getId());
              break;

            case COL_TA_NUMBER:
              if (!parent.isNull(i)) {
                newRow.setValue(i, BeeUtils.trim(parent.getString(i)) + " P-"
                    + response.getResponseAsString());
              }
              break;

            case COL_TA_CONTINUOUS:
            case COL_TA_RETURN:
            case COL_TA_UNTIL:
            case COL_TA_NOTES:
            case ALS_RETURNED_COUNT:
              break;

            default:
              if (!parent.isNull(i) && !colId.startsWith(COL_TA_STATUS)) {
                newRow.setValue(i, parent.getString(i));
              }
          }
        }

        TradeActKeeper.setDefaultOperation(newRow, TradeActKind.SUPPLEMENT);

        RowFactory.createRow(dataInfo, newRow, Modality.ENABLED, new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            getGridView().ensureRow(result, true);
          }
        });
      }
    });
  }

  private Button ensureTemplateCommand() {
    if (templateCommand == null) {
      templateCommand = new Button(Localized.dictionary().tradeActSaveAsTemplate(),
          event -> {
            int maxLen = Data.getColumnPrecision(VIEW_TRADE_ACT_TEMPLATES, COL_TA_TEMPLATE_NAME);

            Global.inputString(Localized.dictionary().tradeActNewTemplate(),
                Localized.dictionary().name(), new StringCallback() {
                  @Override
                  public void onSuccess(String value) {
                    if (!BeeUtils.isEmpty(value)) {
                      saveAsTemplate(value.trim());
                    }
                  }
                }, null, null, maxLen);
          });

      TradeActKeeper.addCommandStyle(templateCommand, "template");
      TradeActKeeper.setCommandEnabled(templateCommand, false);
    }
    return templateCommand;
  }

  private static void maybeOpenAct(GridView gridView, IsRow row) {
    if (DomUtils.isVisible(gridView.getGrid())) {
      gridView.onEditStart(new EditStartEvent(row, gridView.isReadOnly()));
    }
  }

  private void refreshCommands(IsRow row) {
    TradeActKind k = null;

    if (row != null) {
      k = TradeActKeeper.getKind(row, getDataIndex(COL_TA_KIND));
    }
    TradeActKeeper.setCommandEnabled(supplementCommand, k != null && k.enableSupplement());
    TradeActKeeper.setCommandEnabled(returnCommand, k != null && k.enableReturn()
      && !DataUtils.isId(Data.getLong(VIEW_TRADE_ACTS, row, COL_TA_CONTINUOUS)));
    TradeActKeeper.setCommandEnabled(alterCommand, k != null && k.enableAlter());
    TradeActKeeper.setCommandEnabled(copyCommand, k != null && k.enableCopy());
    TradeActKeeper.setCommandEnabled(templateCommand, k != null && k.enableTemplate());
  }

  private void saveAsTemplate(String name) {
    IsRow row = getGridView().getActiveRow();

    if (row == null) {
      logger.severe(SVC_SAVE_ACT_AS_TEMPLATE, "act row not available");

    } else {
      ParameterList params = TradeActKeeper.createArgs(SVC_SAVE_ACT_AS_TEMPLATE);
      params.addDataItem(COL_TRADE_ACT, row.getId());
      params.addDataItem(COL_TA_TEMPLATE_NAME, name);

      BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
        @Override
        public void onResponse(ResponseObject response) {
          response.notify(getGridView());

          if (response.hasResponse(BeeRow.class)) {
            BeeRow template = BeeRow.restore(response.getResponseAsString());
            DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACT_TEMPLATES);

            Data.resetLocal(VIEW_TRADE_ACTS);
            RowEditor.open(VIEW_TRADE_ACT_TEMPLATES, template, Opener.MODAL);
          }
        }
      });
    }
  }

  private static void checkContract(final IsRow row, final GwtEvent<?> event,
      final GridView gridView) {
    int idxKind = Data.getColumnIndex(VIEW_TRADE_ACTS, COL_TA_KIND);
    if (TradeActKind.SALE.ordinal() == BeeUtils.unbox(row.getInteger(idxKind))
        || TradeActKind.SUPPLEMENT.ordinal() == BeeUtils.unbox(row.getInteger(idxKind))) {

      int contractIdx = Data.getColumnIndex(VIEW_TRADE_ACTS, WIDGET_TA_CONTRACT);
      if (!BeeUtils.isPositive(row.getInteger(contractIdx))) {

        if (event instanceof ReadyForInsertEvent) {
          ((ReadyForInsertEvent) event).consume();
        }
        if (event instanceof SaveChangesEvent) {
          ((SaveChangesEvent) event).consume();
        }

        Global.confirm(Localized.dictionary().taEmptyContract()
            + Localized.dictionary().saveChanges(), () -> gridView.fireEvent(event));
      }
    }
  }

  private void revertStatuses(Long ... acts) {
    ParameterList prm = TradeActKeeper.createArgs(SVC_REVERT_ACTS_STATUS_BEFORE_DELETE);
    prm.addDataItem(TradeConstants.VAR_ID_LIST, DataUtils.buildIdList(acts));

    BeeKeeper.getRpc().makePostRequest(prm, new ResponseCallback() {
      @Override
      public void onResponse(ResponseObject response) {
        if (response.hasErrors()) {
          getGridView().notifySevere(response.getErrors());
          return;
        }

        if (getGridView() != null && getGridView().getGrid() != null) {
          getGridView().getGrid().clearSelection();
        }

        Queries.delete(VIEW_TRADE_ACTS, Filter.idIn(Lists.newArrayList(acts)),
            new Queries.IntCallback() {
              @Override
              public void onSuccess(Integer result) {
                DataChangeEvent.fireRefresh(BeeKeeper.getBus(), VIEW_TRADE_ACTS);
              }
            });
      }
    });
  }
}
