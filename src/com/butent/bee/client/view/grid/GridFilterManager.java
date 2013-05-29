package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.DialogBox;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.view.grid.CellGrid.ColumnInfo;
import com.butent.bee.client.view.search.AbstractFilterSupplier;
import com.butent.bee.client.view.search.FilterConsumer;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Consumer;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterDescription;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

public class GridFilterManager {

  private static final BeeLogger logger = LogUtils.getLogger(GridFilterManager.class);

  private static final String STYLE_PREFIX = "bee-GridFilter-";
  private static final String STYLE_DIALOG = STYLE_PREFIX + "dialog";

  private static final String STYLE_CONTENT = STYLE_PREFIX + "content";

  private static final String STYLE_SUPPLIER_PREFIX = STYLE_PREFIX + "supplier-";
  private static final String STYLE_SUPPLIER_PANEL = STYLE_SUPPLIER_PREFIX + "panel";
  private static final String STYLE_SUPPLIER_TABLE = STYLE_SUPPLIER_PREFIX + "table";
  private static final String STYLE_SUPPLIER_ROW = STYLE_SUPPLIER_PREFIX + "row";
  private static final String STYLE_SUPPLIER_LABEL = STYLE_SUPPLIER_PREFIX + "label";
  private static final String STYLE_SUPPLIER_ACTION_CONTAINER = STYLE_SUPPLIER_PREFIX
      + "action-container";
  private static final String STYLE_SUPPLIER_EMPTY = STYLE_SUPPLIER_PREFIX + "empty";
  private static final String STYLE_SUPPLIER_NOT_EMPTY = STYLE_SUPPLIER_PREFIX + "not-empty";
  private static final String STYLE_SUPPLIER_BUTTON = STYLE_SUPPLIER_PREFIX + "button";
  private static final String STYLE_SUPPLIER_CLEAR = STYLE_SUPPLIER_PREFIX + "clear";

  private static final String STYLE_SAVE_PREFIX = STYLE_PREFIX + "save-";
  private static final String STYLE_SAVE_PANEL = STYLE_SAVE_PREFIX + "panel";
  private static final String STYLE_SAVE_ICON = STYLE_SAVE_PREFIX + "icon";
  private static final String STYLE_SAVE_MESSAGE = STYLE_SAVE_PREFIX + "message";

  public static Filter parseFilter(CellGrid grid, List<Map<String, String>> filterValues) {
    if (BeeUtils.isEmpty(filterValues)) {
      return null;
    }

    List<Filter> filters = Lists.newArrayList();

    for (Map<String, String> values : filterValues) {
      for (Map.Entry<String, String> entry : values.entrySet()) {
        String columnId = entry.getKey();

        ColumnInfo columnInfo = GridUtils.getColumnInfo(grid.getPredefinedColumns(), columnId);
        if (columnInfo == null) {
          logger.warning("filter column not found:", columnId);
          continue;
        }

        AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
        if (filterSupplier == null) {
          logger.warning("filter supplier not found:", columnId);
          continue;
        }

        Filter columnFilter = filterSupplier.parse(entry.getValue());
        if (columnFilter == null) {
          logger.warning(columnId, "cannot parse filter:", entry.getValue());
        } else if (!filters.contains(columnFilter)) {
          filters.add(columnFilter);
        }
      }
    }

    return Filter.and(filters);
  }

  public GridFilterManager() {
    super();
  }

  public void clearFilter(CellGrid grid) {
    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
      if (filterSupplier != null && !filterSupplier.isEmpty()) {
        filterSupplier.setValue(null);
      }
    }
  }

  public void handleFilter(String gridKey, final CellGrid grid, Element target,
      final FilterConsumer filterConsumer) {

    List<ColumnInfo> predefinedColumns = grid.getPredefinedColumns();
    List<Integer> visibleColumns = grid.getVisibleColumns();

    final DialogBox dialog = DialogBox.create(Localized.constants.filter(), STYLE_DIALOG);

    final Scheduler.ScheduledCommand onChange = new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        final Filter filter = getFilter(grid, null);
        logger.debug("accept", filter);

        filterConsumer.tryFilter(filter, new Consumer<Boolean>() {
          @Override
          public void accept(Boolean input) {
            if (BeeUtils.isTrue(input)) {
              dialog.close();
            } else if (filter != null) {
              BeeKeeper.getScreen().notifyWarning(Localized.constants.nothingFound());
            }
          }
        }, false);
      }
    };

    HtmlTable table = new HtmlTable(STYLE_SUPPLIER_TABLE);
    int row = 0;

    for (int index : visibleColumns) {
      ColumnInfo columnInfo = predefinedColumns.get(index);
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
      if (filterSupplier != null) {
        createSupplierRow(table, row, grid, columnInfo, filterSupplier, onChange);
        row++;
      }
    }

    if (predefinedColumns.size() > visibleColumns.size()) {
      for (int i = 0; i < predefinedColumns.size(); i++) {
        if (!visibleColumns.contains(i)) {
          ColumnInfo columnInfo = predefinedColumns.get(i);
          AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();
          if (filterSupplier != null && !filterSupplier.isEmpty()) {
            createSupplierRow(table, row, grid, columnInfo, filterSupplier, onChange);
            row++;
          }
        }
      }
    }

    Flow content = new Flow();
    content.addStyleName(STYLE_CONTENT);

    Simple supplierPanel = new Simple(table);
    supplierPanel.addStyleName(STYLE_SUPPLIER_PANEL);
    content.add(supplierPanel);

    Widget saveWidget = maybeCreateSaveWidget(gridKey, grid, dialog);
    if (saveWidget != null) {
      content.add(saveWidget);
    }

    if (Global.getFilters().containsKey(gridKey)) {
      Widget widget = Global.getFilters().createWidget(gridKey, new Consumer<FilterDescription>() {
        @Override
        public void accept(FilterDescription input) {
          List<Map<String, String>> values = Lists.newArrayList();
          values.add(input.getValues());

          setFilter(grid, values);
          onChange.execute();
        }
      });

      if (widget != null) {
        content.add(widget);
      }
    }

    dialog.setWidget(content);
    dialog.setHideOnEscape(true);

    dialog.setAnimationEnabled(true);
    dialog.showRelativeTo(target);
  }

  public void setFilter(CellGrid grid, List<Map<String, String>> filterValues) {
    if (BeeUtils.isEmpty(filterValues)) {
      clearFilter(grid);
      return;
    }

    logger.debug("set", filterValues);

    Map<String, String> columnFilters = Maps.newHashMap();
    for (Map<String, String> values : filterValues) {
      for (Map.Entry<String, String> entry : values.entrySet()) {
        String columnId = entry.getKey();

        if (columnFilters.containsKey(columnId)) {
          logger.warning(columnId, "duplicate column filter:", columnFilters.get(columnId),
              entry.getValue());
        } else {
          columnFilters.put(columnId, entry.getValue());
        }
      }
    }

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        String columnId = columnInfo.getColumnId();

        if (columnFilters.containsKey(columnId)) {
          filterSupplier.setValue(columnFilters.get(columnId));
        } else if (!filterSupplier.isEmpty()) {
          filterSupplier.setValue(null);
        }
      }
    }
  }

  private void createSupplierRow(HtmlTable table, int row, final CellGrid grid,
      final ColumnInfo columnInfo, final AbstractFilterSupplier filterSupplier,
      final Scheduler.ScheduledCommand onChange) {

    CustomDiv label = new CustomDiv();
    label.setHTML(columnInfo.getLabel());

    table.setWidgetAndStyle(row, 0, label, STYLE_SUPPLIER_LABEL);

    final BeeButton button = new BeeButton();
    button.addStyleName(STYLE_SUPPLIER_BUTTON);

    if (!filterSupplier.isEmpty()) {
      button.setHTML(filterSupplier.getLabel());
      button.setTitle(filterSupplier.getTitle());
    }

    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        filterSupplier.setEffectiveFilter(getFilter(grid, columnInfo.getColumnId()));
        filterSupplier.onRequest(button.getElement(), onChange);
      }
    };
    
    label.addClickHandler(clickHandler);
    button.addClickHandler(clickHandler);

    BeeImage clear = new BeeImage(Global.getImages().closeSmall());
    clear.addStyleName(STYLE_SUPPLIER_CLEAR);

    clear.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        filterSupplier.setValue(null);
        onChange.execute();
      }
    });

    Flow actionContainer = new Flow();
    actionContainer.add(button);
    actionContainer.add(clear);

    table.setWidgetAndStyle(row, 1, actionContainer, STYLE_SUPPLIER_ACTION_CONTAINER);

    actionContainer.addStyleName(filterSupplier.isEmpty()
        ? STYLE_SUPPLIER_EMPTY : STYLE_SUPPLIER_NOT_EMPTY);
    table.getRowFormatter().addStyleName(row, STYLE_SUPPLIER_ROW);
  }

  private Filter getFilter(CellGrid grid, String excludeColumn) {
    List<Filter> filters = Lists.newArrayList();

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        if (!BeeUtils.isEmpty(excludeColumn) && columnInfo.is(excludeColumn)) {
          continue;
        }

        Filter columnFilter = filterSupplier.getFilter();
        if (columnFilter != null) {
          filters.add(columnFilter);
        }
      }
    }
    return Filter.and(filters);
  }

  private String getFilterLabel(CellGrid grid) {
    List<String> labels = Lists.newArrayList();

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        String label = filterSupplier.getLabel();
        if (!BeeUtils.isEmpty(label)) {
          labels.add(label);
        }
      }
    }

    if (labels.isEmpty()) {
      return null;
    } else {
      return BeeUtils.join(BeeConst.STRING_SPACE, labels);
    }
  }

  private Map<String, String> getFilterValues(CellGrid grid) {
    Map<String, String> values = Maps.newHashMap();

    List<ColumnInfo> columns = grid.getPredefinedColumns();
    for (ColumnInfo columnInfo : columns) {
      AbstractFilterSupplier filterSupplier = columnInfo.getFilterSupplier();

      if (filterSupplier != null) {
        String value = filterSupplier.getValue();
        if (!BeeUtils.isEmpty(value)) {
          values.put(columnInfo.getColumnId(), value);
        }
      }
    }
    return values;
  }

  private Widget maybeCreateSaveWidget(final String gridKey, final CellGrid grid,
      final DialogBox dialog) {

    final Map<String, String> values = getFilterValues(grid);
    if (values.isEmpty() || Global.getFilters().contains(gridKey, values)) {
      return null;
    }

    Flow panel = new Flow();
    panel.addStyleName(STYLE_SAVE_PANEL);

    ClickHandler clickHandler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialog.close();
        String label = getFilterLabel(grid);

        if (BeeUtils.isEmpty(label)) {
          logger.severe("filter has no label:", values);
        } else {
          Global.getFilters().addCustomFilter(gridKey, label, values);
        }
      }
    };

    BeeImage icon = new BeeImage(Global.getImages().silverPlus());
    icon.addStyleName(STYLE_SAVE_ICON);
    icon.addClickHandler(clickHandler);
    panel.add(icon);

    CustomDiv message = new CustomDiv(STYLE_SAVE_MESSAGE);
    message.setText(Localized.constants.saveFilter());
    message.addClickHandler(clickHandler);
    panel.add(message);

    return panel;
  }
}
