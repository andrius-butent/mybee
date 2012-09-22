package com.butent.bee.client.presenter;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.data.AsyncProvider;
import com.butent.bee.client.data.CachedProvider;
import com.butent.bee.client.data.HasActiveRow;
import com.butent.bee.client.data.HasDataProvider;
import com.butent.bee.client.data.LocalProvider;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.dialog.DialogCallback;
import com.butent.bee.client.dialog.DialogConstants;
import com.butent.bee.client.dialog.NotificationListener;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.ui.WidgetInitializer;
import com.butent.bee.client.utils.Command;
import com.butent.bee.client.view.GridContainerImpl;
import com.butent.bee.client.view.GridContainerView;
import com.butent.bee.client.view.HasGridView;
import com.butent.bee.client.view.HasSearch;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.ReadyForUpdateEvent;
import com.butent.bee.client.view.edit.SaveChangesEvent;
import com.butent.bee.client.view.grid.AbstractGridCallback;
import com.butent.bee.client.view.grid.CellGrid;
import com.butent.bee.client.view.grid.GridCallback;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.cache.CachingPolicy;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowInsertEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GridPresenter extends AbstractPresenter implements ReadyForInsertEvent.Handler,
    ReadyForUpdateEvent.Handler, SaveChangesEvent.Handler, HasSearch, HasDataProvider,
    HasActiveRow,
    HasGridView {

  private class DeleteCallback extends Command {
    private final IsRow activeRow;
    private final Collection<RowInfo> rows;

    private DeleteCallback(IsRow activeRow, Collection<RowInfo> rows) {
      this.activeRow = activeRow;
      this.rows = rows;
    }

    @Override
    public void execute() {
      Assert.state(!BeeUtils.allEmpty(activeRow, rows));

      int count = (rows == null) ? 0 : rows.size();
      GridCallback gcb = getGridCallback();

      if (gcb != null) {
        if ((count == 0
            ? gcb.beforeDeleteRow(GridPresenter.this, activeRow)
            : gcb.beforeDeleteRows(GridPresenter.this, activeRow, rows))
          == GridCallback.DELETE_CANCEL) {
          return;
        }
      }
      setLoadingState(LoadingStateChangeEvent.LoadingState.LOADING);

      if (count == 0) {
        final long rowId = activeRow.getId();
        long version = activeRow.getVersion();

        if (BeeUtils.isEmpty(getViewName())) {
          getDataProvider().onRowDelete(new RowDeleteEvent(getViewName(), rowId));
          afterDelete(rowId);
        } else {
          Queries.deleteRow(getViewName(), rowId, version, new Queries.IntCallback() {
            @Override
            public void onFailure(String... reason) {
              setLoadingState(LoadingStateChangeEvent.LoadingState.LOADED);
              showFailure("Error deleting row", reason);
            }

            @Override
            public void onSuccess(Integer result) {
              BeeKeeper.getBus().fireEvent(new RowDeleteEvent(getViewName(), rowId));
              afterDelete(rowId);
            }
          });
        }
      } else {
        final long[] rowIds = new long[count];
        int i = 0;
        for (RowInfo rowInfo : rows) {
          rowIds[i] = rowInfo.getId();
          i++;
        }
        if (BeeUtils.isEmpty(getViewName())) {
          getDataProvider().onMultiDelete(new MultiDeleteEvent(getViewName(), rows));
          afterMulti(rowIds);
        } else {
          Queries.deleteRows(getViewName(), rows, new Queries.IntCallback() {
            @Override
            public void onFailure(String... reason) {
              showFailure("Error deleting rows", reason);
              setLoadingState(LoadingStateChangeEvent.LoadingState.LOADED);
            }

            @Override
            public void onSuccess(Integer result) {
              BeeKeeper.getBus().fireEvent(new MultiDeleteEvent(getViewName(), rows));
              afterMulti(rowIds);
              showInfo("Išmesta " + result + " eil.");
            }
          });
        }
      }
    }

    private void afterMulti(long[] rowIds) {
      for (long rowId : rowIds) {
        afterDelete(rowId);
      }
    }
  }

  private final GridContainerView gridContainer;
  private final Provider dataProvider;

  private final Set<HandlerRegistration> filterChangeHandlers = Sets.newHashSet();
  private Filter lastFilter = null;

  public GridPresenter(GridDescription gridDescription, int rowCount, BeeRowSet rowSet,
      Provider.Type providerType, CachingPolicy cachingPolicy, Collection<UiOption> uiOptions) {
    this(gridDescription, rowCount, rowSet, providerType, cachingPolicy, uiOptions,
        null, null, null, null, null);
  }

  public GridPresenter(GridDescription gridDescription, int rowCount, BeeRowSet rowSet,
      Provider.Type providerType, CachingPolicy cachingPolicy, Collection<UiOption> uiOptions,
      GridCallback gridCallback, Filter immutableFilter, Map<String, Filter> initialFilters,
      Order order, GridFactory.GridOptions gridOptions) {

    if (gridCallback != null) {
      gridCallback.setGridPresenter(this);
    }

    this.gridContainer = createView(gridDescription, rowSet.getColumns(), rowCount, rowSet,
        order, gridCallback, uiOptions, gridOptions);

    this.dataProvider = createProvider(gridContainer, gridDescription.getViewName(),
        rowSet.getColumns(), gridDescription.getIdName(), gridDescription.getVersionName(),
        immutableFilter, initialFilters, order, rowSet, providerType, cachingPolicy);

    bind();
  }

  public void addRow() {
    if (getGridCallback() != null && !getGridCallback().beforeAddRow(this)) {
      return;
    }
    getGridView().startNewRow();
  }

  public void close() {
    if (getGridCallback() != null && !getGridCallback().onClose(this)) {
      return;
    }
    BeeKeeper.getScreen().closeWidget(getView().asWidget());
  }

  public void deleteRow(IsRow row, boolean confirm) {
    Assert.notNull(row);

    String message = (getGridCallback() != null)
        ? getGridCallback().getDeleteRowMessage()
        : AbstractGridCallback.DELETE_ROW_MESSAGE;

    int mode = BeeUtils.isEmpty(message) ? GridCallback.DELETE_SILENT : GridCallback.DELETE_DEFAULT;

    DeleteCallback deleteCallback = new DeleteCallback(row, null);

    if (mode == GridCallback.DELETE_SILENT || mode == GridCallback.DELETE_DEFAULT && !confirm) {
      deleteCallback.execute();
    } else {
      Global.getMsgBoxen().confirm(message, deleteCallback, StyleUtils.NAME_SCARY);
    }
  }

  @Override
  public IsRow getActiveRow() {
    return getGridView().getActiveRow();
  }

  @Override
  public String getCaption() {
    return getView().getCaption();
  }
  
  public List<BeeColumn> getDataColumns() {
    return getDataProvider().getColumns();
  }

  @Override
  public Provider getDataProvider() {
    return dataProvider;
  }

  @Override
  public GridView getGridView() {
    return getView().getGridView();
  }

  public Filter getLastFilter() {
    return lastFilter;
  }

  @Override
  public Collection<SearchView> getSearchers() {
    Collection<SearchView> searchers;

    if (getView() instanceof HasSearch) {
      searchers = ((HasSearch) getView()).getSearchers();
    } else {
      searchers = null;
    }
    return searchers;
  }

  public GridContainerView getView() {
    return gridContainer;
  }

  public String getViewName() {
    return getDataProvider().getViewName();
  }

  @Override
  public Widget getWidget() {
    return getView().asWidget();
  }

  @Override
  public void handleAction(Action action) {
    Assert.notNull(action);
    if (getGridCallback() != null && !getGridCallback().beforeAction(action, this)) {
      return;
    }

    switch (action) {
      case ADD:
        if (getView().isEnabled()) {
          addRow();
        }
        break;

      case CLOSE:
        close();
        break;

      case CONFIGURE:
        Global.inputString("Options", new DialogCallback<String>() {
          @Override
          public void onSuccess(String value) {
            getGridView().applyOptions(value);
          }
        });
        break;

      case DELETE:
        if (getView().isEnabled()) {
          IsRow row = getActiveRow();
          if (row != null && getGridView().isRowEditable(row, true)) {
            Collection<RowInfo> selectedRows = getGridView().getSelectedRows();
            boolean isActiveRowSelected = getGridView().isRowSelected(row.getId());

            if (selectedRows.isEmpty() || isActiveRowSelected && selectedRows.size() == 1) {
              deleteRow(row, true);
            } else {
              deleteRows(row, selectedRows);
            }
          }
        }
        break;

      case REFRESH:
        refresh(true);
        break;

      case BOOKMARK:
        Global.getFavorites().bookmark(getViewName(), getActiveRow(), getDataColumns(),
            getView().getFavorite());
        break;

      case PRINT:
        if (getGridView().getGrid().getRowCount() > 0) {
          Printer.print(getView());
        }
        break;

      default:
        BeeKeeper.getLog().info(action, "not implemented");
    }

    if (getGridCallback() != null) {
      getGridCallback().afterAction(action, this);
    }
  }

  @Override
  public void onReadyForInsert(ReadyForInsertEvent event) {
    setLoadingState(LoadingStateChangeEvent.LoadingState.LOADING);

    Queries.insert(getViewName(), event.getColumns(), event.getValues(), new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        setLoadingState(LoadingStateChangeEvent.LoadingState.LOADED);
        showFailure("Insert Row", reason);
        getGridView().finishNewRow(null);
      }

      @Override
      public void onSuccess(BeeRow result) {
        BeeKeeper.getBus().fireEvent(new RowInsertEvent(getViewName(), result));
        getGridView().finishNewRow(result);
      }
    });
  }

  @Override
  public void onReadyForUpdate(ReadyForUpdateEvent event) {
    final long rowId = event.getRowValue().getId();
    final long version = event.getRowValue().getVersion();
    final String columnId = event.getColumn().getId();
    final String newValue = event.getNewValue();

    if (BeeUtils.isEmpty(getViewName())) {
      getDataProvider().onCellUpdate(new CellUpdateEvent(getViewName(), rowId, version, columnId,
          getDataProvider().getColumnIndex(columnId), newValue));
      return;
    }

    BeeRowSet rs = new BeeRowSet(new BeeColumn(event.getColumn().getType(), columnId));
    rs.setViewName(getViewName());
    rs.addRow(rowId, version, new String[] {event.getOldValue()});
    rs.getRow(0).preliminaryUpdate(0, newValue);

    final boolean rowMode = event.isRowMode();

    Queries.update(rs, rowMode, new RowCallback() {
      @Override
      public void onFailure(String... reason) {
        getGridView().refreshCellContent(rowId, columnId);
        showFailure("Update Cell", reason);
      }

      @Override
      public void onSuccess(BeeRow row) {
        BeeKeeper.getLog().info("cell updated:", getViewName(), rowId, columnId, newValue);
        if (rowMode) {
          BeeKeeper.getBus().fireEvent(new RowUpdateEvent(getViewName(), row));
        } else {
          BeeKeeper.getBus().fireEvent(
              new CellUpdateEvent(getViewName(), rowId, row.getVersion(), columnId,
                  getDataProvider().getColumnIndex(columnId), newValue));
        }
      }
    });
  }

  @Override
  public void onSaveChanges(SaveChangesEvent event) {
    final long rowId = event.getRowId();

    Queries.update(getViewName(), rowId, event.getVersion(), event.getColumns(),
        event.getOldValues(), event.getNewValues(), new RowCallback() {
          @Override
          public void onFailure(String... reason) {
            showFailure("Save Changes", reason);
          }

          @Override
          public void onSuccess(BeeRow row) {
            BeeKeeper.getLog().info("changes saved", getViewName(), rowId);
            BeeKeeper.getBus().fireEvent(new RowUpdateEvent(getViewName(), row));
          }
        });
  }

  @Override
  public void onViewUnload() {
    getView().setViewPresenter(null);

    for (HandlerRegistration hr : filterChangeHandlers) {
      hr.removeHandler();
    }
    filterChangeHandlers.clear();

    getDataProvider().onUnload();
  }

  public void refresh(boolean updateActiveRow) {
    if (getGridCallback() != null) {
      getGridCallback().beforeRefresh(this);
    }

    Filter filter = ViewHelper.getFilter(this, getDataProvider());
    if (filter != null && getGridView().getGrid().getRowCount() <= 0) {
      setLastFilter(null);
      getDataProvider().onFilterChange(null, updateActiveRow);
    } else if (Objects.equal(filter, getLastFilter())) {
      getDataProvider().refresh(updateActiveRow);
    } else {
      getDataProvider().onFilterChange(filter, updateActiveRow);
    }
  }

  private void afterDelete(long rowId) {
    if (getGridCallback() != null) {
      getGridCallback().afterDeleteRow(rowId);
    }
  }

  private void bind() {
    GridContainerView view = getView();
    view.setViewPresenter(this);
    view.bind();

    Collection<SearchView> searchers = getSearchers();
    if (searchers != null) {
      for (SearchView search : searchers) {
        filterChangeHandlers.add(search.addValueChangeHandler(new ValueChangeHandler<String>() {
          @Override
          public void onValueChange(ValueChangeEvent<String> event) {
            updateFilter();
          }
        }));
      }
    }

    view.getGridView().addReadyForUpdateHandler(this);
    view.getGridView().addReadyForInsertHandler(this);

    view.getGridView().addSaveChangesHandler(this);
  }

  private Provider createProvider(GridContainerView view, String viewName, List<BeeColumn> columns,
      String idColumnName, String versionColumnName, Filter immutableFilter,
      Map<String, Filter> initialFilters, Order order, BeeRowSet rowSet,
      Provider.Type providerType, CachingPolicy cachingPolicy) {

    if (providerType == null) {
      return null;
    }

    Provider provider;
    CellGrid display = view.getGridView().getGrid();
    NotificationListener notificationListener = view.getGridView();

    switch (providerType) {
      case ASYNC:
        provider = new AsyncProvider(display, notificationListener, viewName, columns,
            idColumnName, versionColumnName, immutableFilter, cachingPolicy);
        break;

      case CACHED:
        provider = new CachedProvider(display, notificationListener, viewName, columns,
            idColumnName, versionColumnName, immutableFilter, rowSet);
        break;

      case LOCAL:
        provider = new LocalProvider(display, notificationListener, viewName, columns,
            immutableFilter, rowSet);
        break;

      default:
        Assert.untouchable();
        provider = null;
    }

    if (initialFilters != null) {
      for (Map.Entry<String, Filter> entry : initialFilters.entrySet()) {
        String key = entry.getKey();
        Filter value = entry.getValue();
        if (!BeeUtils.isEmpty(key) && value != null) {
          provider.setParentFilter(key, value);
        }
      }
    }

    if (order != null) {
      provider.setOrder(order);
    }
    return provider;
  }

  private GridContainerView createView(GridDescription gridDescription, List<BeeColumn> columns,
      int rowCount, BeeRowSet rowSet, Order order, GridCallback gridCallback,
      Collection<UiOption> uiOptions, GridFactory.GridOptions gridOptions) {

    GridContainerView view = new GridContainerImpl();
    view.create(gridDescription, columns, rowCount, rowSet, order, gridCallback, uiOptions,
        gridOptions);

    return view;
  }

  private void deleteRows(final IsRow activeRow, final Collection<RowInfo> selectedRows) {
    int size = selectedRows.size();
    List<String> options = Lists.newArrayList();
    Pair<String, String> defMsg = AbstractGridCallback.deleteRowsMessage(size);
    Pair<String, String> message =
        (getGridCallback() != null) ? getGridCallback().getDeleteRowsMessage(size) : defMsg;

    if (message != null) {
      options.add(BeeUtils.notEmpty(message.getA(), defMsg.getA()));
      options.add(BeeUtils.notEmpty(message.getB(), defMsg.getB()));
    }
    if (options.isEmpty()) {
      DeleteCallback deleteCallback = new DeleteCallback(activeRow, selectedRows);
      deleteCallback.execute();

    } else {
      Global.choice("Išmesti", null, options, new DialogCallback<Integer>() {
        @Override
        public void onSuccess(Integer value) {
          if (value == 0) {
            deleteRow(activeRow, false);

          } else if (value == 1) {
            DeleteCallback deleteCallback = new DeleteCallback(activeRow, selectedRows);
            deleteCallback.execute();
          }
        }
      }, 2, BeeConst.UNDEF, DialogConstants.CANCEL, new WidgetInitializer() {
        @Override
        public Widget initialize(Widget widget, String name) {
          if (BeeUtils.same(name, DialogConstants.WIDGET_DIALOG)) {
            widget.addStyleName(StyleUtils.NAME_SUPER_SCARY);
          }
          return widget;
        }
      });
    }
  }

  private GridCallback getGridCallback() {
    return getGridView().getGridCallback();
  }

  private void setLastFilter(Filter lastFilter) {
    this.lastFilter = lastFilter;
  }

  private void setLoadingState(LoadingStateChangeEvent.LoadingState loadingState) {
    if (loadingState != null) {
      getGridView().getGrid().fireLoadingStateChange(loadingState);
    }
  }

  private void showFailure(String activity, String... reasons) {
    List<String> messages = Lists.newArrayList(activity);
    if (reasons != null) {
      messages.addAll(Lists.newArrayList(reasons));
    }
    getGridView().notifySevere(messages.toArray(new String[0]));
  }

  private void showInfo(String... messages) {
    getGridView().notifyInfo(messages);
  }

  private void updateFilter() {
    Filter filter = ViewHelper.getFilter(this, getDataProvider());
    if (Objects.equal(filter, getLastFilter())) {
      showInfo("filtras nepasikeitė", BeeUtils.transform(filter));
    } else {
      setLastFilter(filter);
      getDataProvider().onFilterChange(filter, true);
    }
  }
}
