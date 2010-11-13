package com.butent.bee.egg.client.grid;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasScrollHandlers;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import com.butent.bee.egg.client.BeeGlobal;
import com.butent.bee.egg.client.BeeKeeper;
import com.butent.bee.egg.client.dom.DomUtils;
import com.butent.bee.egg.client.grid.FlexTable.FlexCellFormatter;
import com.butent.bee.egg.client.grid.HtmlTable.Cell;
import com.butent.bee.egg.client.grid.HtmlTable.CellFormatter;
import com.butent.bee.egg.client.grid.SelectionGrid.SelectionPolicy;
import com.butent.bee.egg.client.grid.SortableGrid.ColumnSorter;
import com.butent.bee.egg.client.grid.SortableGrid.ColumnSorterCallback;
import com.butent.bee.egg.client.grid.edit.CellEditor;
import com.butent.bee.egg.client.grid.edit.CellEditor.CellEditInfo;
import com.butent.bee.egg.client.grid.event.ColumnSortEvent;
import com.butent.bee.egg.client.grid.event.ColumnSortHandler;
import com.butent.bee.egg.client.grid.event.HasPageChangeHandlers;
import com.butent.bee.egg.client.grid.event.HasPageCountChangeHandlers;
import com.butent.bee.egg.client.grid.event.HasPageLoadHandlers;
import com.butent.bee.egg.client.grid.event.HasPagingFailureHandlers;
import com.butent.bee.egg.client.grid.event.HasRowInsertionHandlers;
import com.butent.bee.egg.client.grid.event.HasRowRemovalHandlers;
import com.butent.bee.egg.client.grid.event.HasRowValueChangeHandlers;
import com.butent.bee.egg.client.grid.event.PageChangeEvent;
import com.butent.bee.egg.client.grid.event.PageChangeHandler;
import com.butent.bee.egg.client.grid.event.PageCountChangeEvent;
import com.butent.bee.egg.client.grid.event.PageCountChangeHandler;
import com.butent.bee.egg.client.grid.event.PageLoadEvent;
import com.butent.bee.egg.client.grid.event.PageLoadHandler;
import com.butent.bee.egg.client.grid.event.PagingFailureEvent;
import com.butent.bee.egg.client.grid.event.PagingFailureHandler;
import com.butent.bee.egg.client.grid.event.RowCountChangeEvent;
import com.butent.bee.egg.client.grid.event.RowCountChangeHandler;
import com.butent.bee.egg.client.grid.event.RowInsertionEvent;
import com.butent.bee.egg.client.grid.event.RowInsertionHandler;
import com.butent.bee.egg.client.grid.event.RowRemovalEvent;
import com.butent.bee.egg.client.grid.event.RowRemovalHandler;
import com.butent.bee.egg.client.grid.event.RowSelectionEvent;
import com.butent.bee.egg.client.grid.event.RowSelectionHandler;
import com.butent.bee.egg.client.grid.event.RowValueChangeEvent;
import com.butent.bee.egg.client.grid.event.RowValueChangeHandler;
import com.butent.bee.egg.client.grid.event.TableEvent.Row;
import com.butent.bee.egg.client.grid.model.MutableTableModel;
import com.butent.bee.egg.client.grid.model.TableModel;
import com.butent.bee.egg.client.grid.model.TableModel.Callback;
import com.butent.bee.egg.client.grid.model.TableModelHelper.ColumnSortList;
import com.butent.bee.egg.client.grid.model.TableModelHelper.Request;
import com.butent.bee.egg.client.grid.model.TableModelHelper.Response;
import com.butent.bee.egg.client.grid.property.FooterProperty;
import com.butent.bee.egg.client.grid.property.HeaderProperty;
import com.butent.bee.egg.client.grid.property.MaximumWidthProperty;
import com.butent.bee.egg.client.grid.property.MinimumWidthProperty;
import com.butent.bee.egg.client.grid.property.PreferredWidthProperty;
import com.butent.bee.egg.client.grid.property.SortableProperty;
import com.butent.bee.egg.client.grid.property.TruncationProperty;
import com.butent.bee.egg.client.grid.render.FixedWidthGridBulkRenderer;
import com.butent.bee.egg.client.grid.render.RendererCallback;
import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.HasId;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ScrollTable<RowType> extends ComplexPanel implements
    HasId, HasScrollHandlers, HasTableDefinition<RowType>,
    HasPageCountChangeHandlers,
    HasPageLoadHandlers, HasPageChangeHandlers, HasPagingFailureHandlers,
    RequiresResize {

  public static enum ColumnResizePolicy {
    DISABLED, SINGLE_CELL, MULTI_CELL
  }

  public static enum ResizePolicy {
    UNCONSTRAINED(false, false), FLOW(false, true), FIXED_WIDTH(true, true), FILL_WIDTH(
        true, true);

    private boolean isSacrificial;
    private boolean isFixedWidth;

    private ResizePolicy(boolean isFixedWidth, boolean isSacrificial) {
      this.isFixedWidth = isFixedWidth;
      this.isSacrificial = isSacrificial;
    }

    private boolean isFixedWidth() {
      return isFixedWidth;
    }

    private boolean isSacrificial() {
      return isSacrificial;
    }
  }

  public static enum ScrollPolicy {
    HORIZONTAL, BOTH, DISABLED
  }

  public static enum SortPolicy {
    DISABLED, SINGLE_CELL, MULTI_CELL
  }

  protected static class ScrollTableCellView<RowType> extends
      AbstractCellView<RowType> {
    private ScrollTable<RowType> table;

    public ScrollTableCellView(ScrollTable<RowType> table) {
      super(table);
      this.table = table;
    }

    @Override
    public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
      table.getDataTable().getCellFormatter().setHorizontalAlignment(
          getRowIndex(), getCellIndex(), align);
    }

    @Override
    public void setHTML(String html) {
      table.getDataTable().setHTML(getRowIndex(), getCellIndex(), html);
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      table.getDataTable().getFixedWidthGridCellFormatter().getRawElement(
          getRowIndex(), getCellIndex()).getStyle().setProperty(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      table.getDataTable().getCellFormatter().setStyleName(getRowIndex(),
          getCellIndex(), stylename);
    }

    @Override
    public void setText(String text) {
      table.getDataTable().setText(getRowIndex(), getCellIndex(), text);
    }

    @Override
    public void setVerticalAlignment(VerticalAlignmentConstant align) {
      table.getDataTable().getCellFormatter().setVerticalAlignment(
          getRowIndex(), getCellIndex(), align);
    }

    @Override
    public void setWidget(Widget widget) {
      table.getDataTable().setWidget(getRowIndex(), getCellIndex(), widget);
    }
  }

  protected static class ScrollTableRowView<RowType> extends
      AbstractRowView<RowType> {
    private ScrollTable<RowType> table;

    public ScrollTableRowView(ScrollTable<RowType> table) {
      super(new ScrollTableCellView<RowType>(table));
      this.table = table;
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      table.getDataTable().getFixedWidthGridRowFormatter().getRawElement(
          getRowIndex()).getStyle().setProperty(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      if (table.getDataTable().isRowSelected(getRowIndex())) {
        stylename += " selected";
      }
      table.getDataTable().getRowFormatter().setStyleName(getRowIndex(),
          stylename);
    }
  }

  private static class ColumnHeaderInfo {
    private int rowSpan = 1;
    private Object header;

    public ColumnHeaderInfo(Object header) {
      this.header = (header == null) ? "" : header;
    }

    public ColumnHeaderInfo(Object header, int rowSpan) {
      this.header = (header == null) ? BeeConst.HTML_NBSP : header;
      this.rowSpan = rowSpan;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (o instanceof ColumnHeaderInfo) {
        ColumnHeaderInfo info = (ColumnHeaderInfo) o;
        return (rowSpan == info.rowSpan) && header.equals(info.header);
      }
      return false;
    }

    public Object getHeader() {
      return header;
    }

    public int getRowSpan() {
      return rowSpan;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    public void incrementRowSpan() {
      rowSpan++;
    }
  }

  private static class MouseResizeWorker {
    private class ResizeCommand implements Scheduler.ScheduledCommand {
      @Override
      public void execute() {
        resize();
      }
    }

    private static final int RESIZE_CURSOR_WIDTH = 15;

    private Element curCell = null;
    private List<ColumnWidthInfo> curCells = new ArrayList<ColumnWidthInfo>();
    private int curCellIndex = 0;

    private int mouseXCurrent = 0;
    private int mouseXLast = 0;
    private int mouseXStart = 0;

    private boolean resizing = false;

    private int sacrificeCellIndex = -1;
    private List<ColumnWidthInfo> sacrificeCells = new ArrayList<ColumnWidthInfo>();

    private ScrollTable<?> table = null;

    private ResizeCommand command = new ResizeCommand();

    public Element getCurrentCell() {
      return curCell;
    }

    public boolean isResizing() {
      return resizing;
    }

    public void resizeColumn(Event event) {
      mouseXCurrent = DOM.eventGetClientX(event);
      Scheduler.get().scheduleDeferred(command);
    }

    public boolean setCurrentCell(Event event) {
      Element cell = null;
      if (table.columnResizePolicy == ColumnResizePolicy.MULTI_CELL) {
        cell = table.headerTable.getEventTargetCell(event);
      } else if (table.columnResizePolicy == ColumnResizePolicy.SINGLE_CELL) {
        cell = table.headerTable.getEventTargetCell(event);
        if (cell != null && DomUtils.getColSpan(cell) > 1) {
          cell = null;
        }
      }

      int clientX = event.getClientX();
      if (cell != null) {
        int absLeft = cell.getAbsoluteLeft() - Window.getScrollLeft();
        int absRight = absLeft + cell.getOffsetWidth();
        if (clientX < absRight - RESIZE_CURSOR_WIDTH || clientX > absRight) {
          cell = null;
        }
      }

      if (cell != curCell) {
        if (curCell != null) {
          curCell.getStyle().clearCursor();
        }

        curCell = cell;
        if (curCell != null) {
          curCellIndex = getCellIndex(curCell);
          if (curCellIndex < 0) {
            curCell = null;
            return false;
          }

          boolean resizable = false;
          int colSpan = DomUtils.getColSpan(cell);
          curCells = table.getColumnWidthInfo(curCellIndex, colSpan);
          for (ColumnWidthInfo info : curCells) {
            if (!info.hasMaximumWidth() || !info.hasMinimumWidth()
                || info.getMaximumWidth() != info.getMinimumWidth()) {
              resizable = true;
            }
          }
          if (!resizable) {
            curCell = null;
            curCells = null;
            return false;
          }

          curCell.getStyle().setCursor(Cursor.E_RESIZE);
        }
        return true;
      }

      return false;
    }

    public void setScrollTable(ScrollTable<?> table) {
      this.table = table;
    }

    public void startResizing(Event event) {
      if (curCell != null) {
        resizing = true;
        mouseXStart = event.getClientX();
        mouseXLast = mouseXStart;
        mouseXCurrent = mouseXStart;

        int numColumns = table.getDataTable().getColumnCount();
        int colSpan = DomUtils.getColSpan(curCell);
        sacrificeCellIndex = curCellIndex + colSpan;
        sacrificeCells = table.getColumnWidthInfo(sacrificeCellIndex, numColumns
            - sacrificeCellIndex);

        DOM.setCapture(table.headerWrapper);
      }
    }

    public void stopResizing() {
      if (curCell != null && resizing) {
        resizing = false;

        DOM.releaseCapture(table.headerWrapper);
        curCell.getStyle().clearCursor();

        curCell = null;
        curCells = null;
        sacrificeCells = null;

        curCellIndex = 0;
        sacrificeCellIndex = -1;
      }
    }

    private int getCellIndex(Element cell) {
      int row = TableRowElement.as(DOM.getParent(cell)).getRowIndex() - 1;
      int column = TableCellElement.as(cell).getCellIndex();

      return table.headerTable.getColumnIndex(row, column)
          - table.getHeaderOffset();
    }

    private void resize() {
      if (mouseXLast != mouseXCurrent && resizing) {
        mouseXLast = mouseXCurrent;

        int totalDelta = mouseXCurrent - mouseXStart;
        totalDelta -= table.columnResizer.distributeWidth(curCells, totalDelta);

        if (table.resizePolicy.isSacrificial()) {
          int remaining = table.columnResizer.distributeWidth(sacrificeCells, -totalDelta);

          if (remaining != 0 && table.resizePolicy.isFixedWidth()) {
            totalDelta += remaining;
            table.columnResizer.distributeWidth(curCells, totalDelta);
          }

          table.applyNewColumnWidths(sacrificeCellIndex, sacrificeCells, true);
        }

        table.applyNewColumnWidths(curCellIndex, curCells, true);
        table.scrollTables(false);
      }
    }
  }

  private class SortHandler implements ColumnSortHandler {
    public void onColumnSorted(ColumnSortEvent event) {
      int column = -1;
      boolean ascending = true;
      ColumnSortList sortList = event.getColumnSortList();
      if (sortList != null) {
        column = sortList.getPrimaryColumn();
        ascending = sortList.isPrimaryAscending();
      }

      if (isColumnSortable(column)) {
        Element parent = DOM.getParent(sortedColumnWrapper);
        if (parent != null) {
          parent.removeChild(sortedColumnWrapper);
        }

        if (column < 0) {
          sortedCellIndex = -1;
          sortedRowIndex = -1;
        } else if (sortedCellIndex >= 0 && sortedRowIndex >= 0
            && headerTable.getRowCount() > sortedRowIndex
            && headerTable.getCellCount(sortedRowIndex) > sortedCellIndex) {
          CellFormatter formatter = headerTable.getCellFormatter();
          Element td = formatter.getElement(sortedRowIndex, sortedCellIndex);
          applySortedColumnIndicator(td, ascending);
        }
      }
    }
  }

  private class TableHeightInfo {
    private int headerTableHeight;
    private int dataTableHeight;
    private int footerTableHeight;

    public TableHeightInfo() {
      Element elem = getElement();
      int totalHeight = elem.getClientHeight();
      while (totalHeight <= 0 && elem.getParentElement() != null) {
        elem = elem.getParentElement().cast();
        totalHeight = elem.getClientHeight();
      }

      headerTableHeight = headerTable.getOffsetHeight();
      if (footerTable != null) {
        footerTableHeight = footerTable.getOffsetHeight();
      }
      dataTableHeight = totalHeight - headerTableHeight - footerTableHeight;
    }
  }

  private class TableWidthInfo {
    private int headerTableWidth;
    private int dataTableWidth;
    private int footerTableWidth;
    private int availableWidth;

    public TableWidthInfo(boolean includeSpacer) {
      availableWidth = getAvailableWidth();
      headerTableWidth = getTableWidth(headerTable, includeSpacer);
      dataTableWidth = dataTable.getElement().getScrollWidth();
      if (dataTableWidth <= 0) {
        dataTableWidth = availableWidth;
      }
      if (footerTable != null) {
        footerTableWidth = getTableWidth(footerTable, includeSpacer);
      }
    }
  }

  private class VisibleRowsIterator implements Iterator<RowType> {
    private Iterator<RowType> rows;
    private int curRow;
    private int lastVisibleRow;

    public VisibleRowsIterator(Iterator<RowType> rows, int firstRow,
        int firstVisibleRow, int lastVisibleRow) {
      this.curRow = firstRow;
      this.lastVisibleRow = lastVisibleRow;

      while (curRow < firstVisibleRow && rows.hasNext()) {
        rows.next();
        curRow++;
      }
      this.rows = rows;
    }

    public boolean hasNext() {
      return (curRow <= lastVisibleRow && rows.hasNext());
    }

    public RowType next() {
      if (!hasNext()) {
        Assert.untouchable("no such element");
      }
      return rows.next();
    }

    public void remove() {
      Assert.unsupported("Remove not supported");
    }
  }

  public static final String DEFAULT_STYLE_NAME = "bee-ScrollTable";

  private FixedWidthGridBulkRenderer<RowType> bulkRenderer = null;

  private SimplePanel emptyTableWidgetWrapper = new SimplePanel();

  private TableDefinition<RowType> tableDefinition = null;
  private int currentPage = -1;

  private Request lastRequest = null;

  private boolean isCrossPageSelectionEnabled;

  private Set<RowType> selectedRowValues = new HashSet<RowType>();

  private boolean isFooterGenerated;

  private boolean isHeaderGenerated;

  private boolean isPageLoading;

  private int oldPageCount;

  private int pageSize = 0;

  private Callback<RowType> pagingCallback = new Callback<RowType>() {
    public void onFailure(Throwable caught) {
      isPageLoading = false;
      fireEvent(new PagingFailureEvent(caught));
    }

    public void onRowsReady(Request request, Response<RowType> response) {
      if (lastRequest == request) {
        setData(request.getStartRow(), response.getRowValues());
        lastRequest = null;
      }
    }
  };

  private List<RowType> rowValues = new ArrayList<RowType>();

  private AbstractRowView<RowType> rowView = new ScrollTableRowView<RowType>(
      this);

  private Widget selectAllWidget;

  private TableModel<RowType> tableModel;

  private RendererCallback tableRendererCallback = new RendererCallback() {
    public void onRendered() {
      onDataTableRendered();
    }
  };

  private List<ColumnDefinition<RowType, ?>> visibleColumns = new ArrayList<ColumnDefinition<RowType, ?>>();

  private boolean headersObsolete;

  private Element absoluteElem;

  private ColumnResizer columnResizer = new ColumnResizer();

  private ColumnResizePolicy columnResizePolicy = ColumnResizePolicy.MULTI_CELL;

  private FixedWidthGrid dataTable;

  private Element dataWrapper;

  private FixedWidthFlexTable footerTable = null;

  private Element footerWrapper = null;

  private Element headerSpacer;

  private FixedWidthFlexTable headerTable = null;

  private Element headerWrapper;

  private String lastHeight = null;

  private int lastScrollLeft;

  private MouseResizeWorker resizeWorker = GWT.create(MouseResizeWorker.class);

  private ResizePolicy resizePolicy = ResizePolicy.FILL_WIDTH;

  private ScrollPolicy scrollPolicy = ScrollPolicy.BOTH;

  private SortPolicy sortPolicy = SortPolicy.MULTI_CELL;

  private int sortedCellIndex = -1;

  private int sortedRowIndex = -1;

  private Element sortedColumnWrapper = null;

  @SuppressWarnings("unchecked")
  public ScrollTable(TableModel<RowType> tableModel,
      FixedWidthGrid dataTable, FixedWidthFlexTable headerTable,
      TableDefinition<RowType> tableDefinition) {
    super();
    this.dataTable = dataTable;
    this.headerTable = headerTable;
    resizeWorker.setScrollTable(this);

    prepareTable(dataTable, "dataTable");
    prepareTable(headerTable, "headerTable");
    if (dataTable.getSelectionPolicy().hasInputColumn()) {
      headerTable.setColumnWidth(0, dataTable.getInputColumnWidth());
    }

    Element mainElem = DOM.createDiv();
    setElement(mainElem);
    setStylePrimaryName(DEFAULT_STYLE_NAME);
    mainElem.getStyle().setPadding(0, Unit.PX);
    mainElem.getStyle().setOverflow(Overflow.HIDDEN);
    mainElem.getStyle().setPosition(Position.RELATIVE);
    createId();

    absoluteElem = DOM.createDiv();
    absoluteElem.getStyle().setPosition(Position.ABSOLUTE);
    absoluteElem.getStyle().setTop(0, Unit.PX);
    absoluteElem.getStyle().setLeft(0, Unit.PX);
    absoluteElem.getStyle().setWidth(100, Unit.PCT);

    absoluteElem.getStyle().setPadding(0, Unit.PX);
    absoluteElem.getStyle().setMargin(0, Unit.PX);
    absoluteElem.getStyle().setBorderWidth(0, Unit.PX);
    absoluteElem.getStyle().setOverflow(Overflow.HIDDEN);

    setStyleName(absoluteElem, "absolute");
    DomUtils.createId(absoluteElem, "st-absolute");
    mainElem.appendChild(absoluteElem);

    headerWrapper = createWrapper("headerWrapper", "st-header");
    headerSpacer = createSpacer(headerTable);
    dataWrapper = createWrapper("dataWrapper", "st-data");

    setCellSpacing(0);
    setCellPadding(2);

    adoptTable(headerTable, headerWrapper, 0);
    adoptTable(dataTable, dataWrapper, 1);

    sortedColumnWrapper = DOM.createSpan();

    sinkEvents(Event.ONMOUSEOUT);
    DOM.setEventListener(dataWrapper, this);
    DOM.sinkEvents(dataWrapper, Event.ONSCROLL);
    DOM.setEventListener(headerWrapper, this);
    DOM.sinkEvents(headerWrapper, Event.ONMOUSEMOVE | Event.ONMOUSEDOWN | Event.ONMOUSEUP);

    dataTable.addColumnSortHandler(new SortHandler());

    this.tableModel = tableModel;
    setTableDefinition(tableDefinition);
    refreshVisibleColumnDefinitions();
    oldPageCount = getPageCount();

    emptyTableWidgetWrapper.getElement().getStyle().setWidth(100, Unit.PCT);
    emptyTableWidgetWrapper.getElement().getStyle().setOverflow(Overflow.HIDDEN);
    emptyTableWidgetWrapper.getElement().getStyle().setBorderWidth(0, Unit.PX);
    emptyTableWidgetWrapper.getElement().getStyle().setMargin(0, Unit.PX);
    emptyTableWidgetWrapper.getElement().getStyle().setPadding(0, Unit.PX);
    insert(emptyTableWidgetWrapper, getAbsoluteElement(), 2, true);
    setEmptyTableWidgetVisible(false);

    tableModel.addRowCountChangeHandler(new RowCountChangeHandler() {
      public void onRowCountChange(RowCountChangeEvent event) {
        int pageCount = getPageCount();
        if (pageCount != oldPageCount) {
          fireEvent(new PageCountChangeEvent(oldPageCount, pageCount));
          oldPageCount = pageCount;
        }
      }
    });

    if (tableModel instanceof HasRowInsertionHandlers) {
      ((HasRowInsertionHandlers) tableModel).addRowInsertionHandler(new RowInsertionHandler() {
        public void onRowInsertion(RowInsertionEvent event) {
          insertAbsoluteRow(event.getRowIndex());
        }
      });
    }

    if (tableModel instanceof HasRowRemovalHandlers) {
      ((HasRowRemovalHandlers) tableModel).addRowRemovalHandler(new RowRemovalHandler() {
        public void onRowRemoval(RowRemovalEvent event) {
          removeAbsoluteRow(event.getRowIndex());
        }
      });
    }

    if (tableModel instanceof HasRowValueChangeHandlers) {
      ((HasRowValueChangeHandlers<RowType>) tableModel).addRowValueChangeHandler(new RowValueChangeHandler<RowType>() {
        public void onRowValueChange(RowValueChangeEvent<RowType> event) {
          int rowIndex = event.getRowIndex();
          if (rowIndex < getAbsoluteFirstRowIndex()
              || rowIndex > getAbsoluteLastRowIndex()) {
            return;
          }
          setRowValue(rowIndex - getAbsoluteFirstRowIndex(),
              event.getRowValue());
        }
      });
    }

    dataTable.addDomHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (event.getSource() instanceof HtmlTable) {
          Cell cell = ((HtmlTable) event.getSource()).getCellForEvent(event);
          if (cell != null) {
            editCell(cell.getRowIndex(), cell.getCellIndex());
          }
        }
      }
    }, ClickEvent.getType());

    if (dataTable.getColumnSorter() == null) {
      ColumnSorter sorter = new ColumnSorter() {
        @Override
        public void onSortColumn(SortableGrid grid, ColumnSortList sortList,
            ColumnSorterCallback callback) {
          reloadPage();
          callback.onSortingComplete();
        }
      };
      dataTable.setColumnSorter(sorter);
    }

    dataTable.addRowSelectionHandler(new RowSelectionHandler() {
      public void onRowSelection(RowSelectionEvent event) {
        if (isPageLoading) {
          return;
        }
        Set<Row> deselected = event.getDeselectedRows();
        for (Row row : deselected) {
          selectedRowValues.remove(getRowValue(row.getRowIndex()));
        }
        Set<Row> selected = event.getSelectedRows();
        for (Row row : selected) {
          selectedRowValues.add(getRowValue(row.getRowIndex()));
        }
      }
    });
  }

  public ScrollTable(TableModel<RowType> tableModel, TableDefinition<RowType> tableDefinition) {
    this(tableModel, new FixedWidthGrid(), new FixedWidthFlexTable(), tableDefinition);
    isHeaderGenerated = true;
    isFooterGenerated = true;
  }

  public HandlerRegistration addPageChangeHandler(PageChangeHandler handler) {
    return addHandler(handler, PageChangeEvent.getType());
  }

  public HandlerRegistration addPageCountChangeHandler(PageCountChangeHandler handler) {
    return addHandler(handler, PageCountChangeEvent.getType());
  }

  public HandlerRegistration addPageLoadHandler(PageLoadHandler handler) {
    return addHandler(handler, PageLoadEvent.getType());
  }

  public HandlerRegistration addPagingFailureHandler(PagingFailureHandler handler) {
    return addHandler(handler, PagingFailureEvent.getType());
  }

  public HandlerRegistration addScrollHandler(ScrollHandler handler) {
    return addDomHandler(handler, ScrollEvent.getType());
  }

  public void createId() {
    DomUtils.createId(this, "scroll-table");
  }

  public Element createSpacer(FixedWidthFlexTable table) {
    resizeSpacer(table, 15);
    return null;
  }

  public void fillWidth() {
    List<ColumnWidthInfo> colWidths = getFillColumnWidths(null);
    applyNewColumnWidths(0, colWidths, false);
    scrollTables(false);
  }

  public int getAbsoluteFirstRowIndex() {
    return currentPage * pageSize;
  }

  public int getAbsoluteLastRowIndex() {
    if (tableModel.getRowCount() < 0) {
      return (currentPage + 1) * pageSize - 1;
    } else if (pageSize == 0) {
      return tableModel.getRowCount() - 1;
    }
    return Math.min(tableModel.getRowCount(), (currentPage + 1) * pageSize) - 1;
  }

  public int getAvailableWidth() {
    Element elem = absoluteElem;
    int clientWidth = absoluteElem.getClientWidth();

    while (clientWidth <= 0 && elem.getParentElement() != null) {
      elem = elem.getParentElement().cast();
      clientWidth = elem.getClientWidth();
    }

    if (scrollPolicy == ScrollPolicy.BOTH) {
      int scrollbarWidth = DomUtils.getScrollbarWidth();
      clientWidth -= scrollbarWidth + 1;
    }
    return Math.max(clientWidth, -1);
  }

  public int getCellPadding() {
    return dataTable.getCellPadding();
  }

  public int getCellSpacing() {
    return dataTable.getCellSpacing();
  }

  public ColumnResizePolicy getColumnResizePolicy() {
    return columnResizePolicy;
  }

  public int getColumnWidth(int column) {
    return dataTable.getColumnWidth(column);
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public FixedWidthGrid getDataTable() {
    return dataTable;
  }

  public Widget getEmptyTableWidget() {
    return emptyTableWidgetWrapper.getWidget();
  }

  public FixedWidthFlexTable getFooterTable() {
    return footerTable;
  }

  public Element getHeaderSpacer() {
    return headerSpacer;
  }

  public FixedWidthFlexTable getHeaderTable() {
    return headerTable;
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  public int getMaximumColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return -1;
    }
    return colDef.getColumnProperty(MaximumWidthProperty.TYPE).getMaximumColumnWidth();
  }

  public int getMinimumColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return FixedWidthGrid.MIN_COLUMN_WIDTH;
    }
    int minWidth = colDef.getColumnProperty(MinimumWidthProperty.TYPE).getMinimumColumnWidth();
    return Math.max(FixedWidthGrid.MIN_COLUMN_WIDTH, minWidth);
  }

  public int getPageCount() {
    if (pageSize < 1) {
      return 1;
    } else {
      int numDataRows = tableModel.getRowCount();
      if (numDataRows < 0) {
        return -1;
      }
      return (int) Math.ceil(numDataRows / (pageSize + 0.0));
    }
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPreferredColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return FixedWidthGrid.DEFAULT_COLUMN_WIDTH;
    }
    return colDef.getColumnProperty(PreferredWidthProperty.TYPE).getPreferredColumnWidth();
  }

  public ResizePolicy getResizePolicy() {
    return resizePolicy;
  }

  public RowType getRowValue(int row) {
    if (rowValues.size() <= row) {
      return null;
    }
    return rowValues.get(row);
  }

  public ScrollPolicy getScrollPolicy() {
    return scrollPolicy;
  }

  public Set<RowType> getSelectedRowValues() {
    return selectedRowValues;
  }

  public SortPolicy getSortPolicy() {
    return sortPolicy;
  }

  public TableDefinition<RowType> getTableDefinition() {
    return tableDefinition;
  }

  public TableModel<RowType> getTableModel() {
    return tableModel;
  }

  public int getTableWidth(FixedWidthFlexTable table, boolean includeSpacer) {
    int scrollWidth = table.getElement().getScrollWidth();
    if (!includeSpacer) {
      int spacerWidth = getSpacerWidth(table);
      if (spacerWidth > 0) {
        scrollWidth -= spacerWidth;
      }
    }
    return scrollWidth;
  }

  public void gotoFirstPage() {
    gotoPage(0, false);
  }

  public void gotoLastPage() {
    if (getPageCount() >= 0) {
      gotoPage(getPageCount(), false);
    }
  }

  public void gotoNextPage() {
    gotoPage(currentPage + 1, false);
  }

  public void gotoPage(int page, boolean forced) {
    int oldPage = currentPage;
    int numPages = getPageCount();
    if (numPages >= 0) {
      currentPage = Math.max(0, Math.min(page, numPages - 1));
    } else {
      currentPage = page;
    }

    if (currentPage != oldPage || forced) {
      isPageLoading = true;

      FixedWidthGrid data = getDataTable();
      data.deselectAllRows();
      if (!isCrossPageSelectionEnabled) {
        selectedRowValues = new HashSet<RowType>();
      }

      fireEvent(new PageChangeEvent(oldPage, currentPage));

      if (bulkRenderer == null) {
        int rowCount = getAbsoluteLastRowIndex() - getAbsoluteFirstRowIndex() + 1;
        if (rowCount != data.getRowCount()) {
          data.resizeRows(rowCount);
        }
        data.clear(true);
      }

      int firstRow = getAbsoluteFirstRowIndex();
      int lastRow = pageSize == 0 ? tableModel.getRowCount() : pageSize;
      lastRequest = new Request(firstRow, lastRow, data.getColumnSortList());
      tableModel.requestRows(lastRequest, pagingCallback);
    }
  }

  public void gotoPreviousPage() {
    gotoPage(currentPage - 1, false);
  }

  public boolean isColumnSortable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    if (getSortPolicy() == SortPolicy.DISABLED) {
      return false;
    }
    return colDef.getColumnProperty(SortableProperty.TYPE).isColumnSortable();
  }

  public boolean isColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isColumnTruncatable();
  }

  public boolean isCrossPageSelectionEnabled() {
    return isCrossPageSelectionEnabled;
  }

  public boolean isFooterColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isFooterTruncatable();
  }

  public boolean isFooterGenerated() {
    return isFooterGenerated;
  }

  public boolean isHeaderColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isHeaderTruncatable();
  }

  public boolean isHeaderGenerated() {
    return isHeaderGenerated;
  }

  public boolean isPageLoading() {
    return isPageLoading;
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);
    Element target = DOM.eventGetTarget(event);

    switch (DOM.eventGetType(event)) {
      case Event.ONSCROLL:
        lastScrollLeft = dataWrapper.getScrollLeft();
        scrollTables(false);
        if (dataWrapper.isOrHasChild(target)) {
          DomEvent.fireNativeEvent(event, this);
        }
        break;

      case Event.ONMOUSEDOWN:
        if (DOM.eventGetButton(event) != Event.BUTTON_LEFT) {
          return;
        }
        if (resizeWorker.getCurrentCell() != null) {
          event.preventDefault();
          event.stopPropagation();
          resizeWorker.startResizing(event);
        }
        break;

      case Event.ONMOUSEUP:
        if (DOM.eventGetButton(event) != Event.BUTTON_LEFT) {
          return;
        }
        if (resizeWorker.isResizing()) {
          resizeWorker.stopResizing();
          return;
        }

        Element cellElem = headerTable.getEventTargetCell(event);
        int column = -1;
        if (cellElem != null) {
          int rowIdx = TableRowElement.as(cellElem.getParentElement()).getRowIndex() - 1;
          int cellIdx = TableCellElement.as(cellElem).getCellIndex();
          column = headerTable.getColumnIndex(rowIdx, cellIdx)
              - getHeaderOffset();
        }

        if (BeeUtils.betweenExclusive(column, 0, dataTable.getColumnCount())) {

        } else {
          ScrollTableConfig config = new ScrollTableConfig(this);
          config.show();
        }

        break;

      case Event.ONMOUSEMOVE:
        if (resizeWorker.isResizing()) {
          resizeWorker.resizeColumn(event);
        } else {
          resizeWorker.setCurrentCell(event);
        }
        break;

      case Event.ONMOUSEOUT:
        Element toElem = DOM.eventGetToElement(event);
        if (toElem == null || !dataWrapper.isOrHasChild(toElem)) {
          int clientX = event.getClientX() + Window.getScrollLeft();
          int clientY = event.getClientY() + Window.getScrollTop();
          int tableLeft = dataWrapper.getAbsoluteLeft();
          int tableTop = dataWrapper.getAbsoluteTop();
          int tableWidth = dataWrapper.getOffsetWidth();
          int tableHeight = dataWrapper.getOffsetHeight();
          int tableBottom = tableTop + tableHeight;
          int tableRight = tableLeft + tableWidth;
          if (clientX > tableLeft && clientX < tableRight && clientY > tableTop
              && clientY < tableBottom) {
            return;
          }

          dataTable.highlightCell(null);
        }
        break;
    }
  }

  public void onResize() {
    redraw();
  }

  public void recalculateIdealColumnWidths(ScrollTable<?> scrollTable) {
    FixedWidthFlexTable ht = scrollTable.getHeaderTable();
    FixedWidthFlexTable ft = scrollTable.getFooterTable();
    FixedWidthGrid dt = scrollTable.getDataTable();

    dt.recalculateIdealColumnWidthsSetup();
    ht.recalculateIdealColumnWidthsSetup();
    if (ft != null) {
      ft.recalculateIdealColumnWidthsSetup();
    }

    dt.recalculateIdealColumnWidthsImpl();
    ht.recalculateIdealColumnWidthsImpl();
    if (ft != null) {
      ft.recalculateIdealColumnWidthsImpl();
    }

    dt.recalculateIdealColumnWidthsTeardown();
    ht.recalculateIdealColumnWidthsTeardown();
    if (ft != null) {
      ft.recalculateIdealColumnWidthsTeardown();
    }
  }

  public void redraw() {
    if (!isAttached()) {
      return;
    }

    TableWidthInfo redrawInfo = new TableWidthInfo(false);

    maybeRecalculateIdealColumnWidths();

    List<ColumnWidthInfo> colWidths = null;
    if (resizePolicy == ResizePolicy.FILL_WIDTH) {
      colWidths = getFillColumnWidths(redrawInfo);
    } else {
      colWidths = getBoundedColumnWidths(true);
    }
    applyNewColumnWidths(0, colWidths, true);

    resizeTablesVertically();

    scrollTables(false);
  }

  public void reloadPage() {
    if (currentPage >= 0) {
      gotoPage(currentPage, true);
    } else {
      gotoPage(0, true);
    }
  }

  @Override
  public boolean remove(Widget child) {
    Assert.unsupported("This panel does not support remove()");
    return false;
  }

  public void repositionSpacer(ScrollTable<?> scrollTable, boolean force) {
    if (!force && scrollTable.scrollPolicy != ScrollPolicy.BOTH) {
      return;
    }

    Element wrapper = scrollTable.dataWrapper;
    int spacerWidth = wrapper.getOffsetWidth() - dataWrapper.getClientWidth();
    resizeSpacer(scrollTable.headerTable, spacerWidth);

    if (scrollTable.footerTable != null) {
      resizeSpacer(scrollTable.footerTable, spacerWidth);
    }
  }

  public void resetColumnWidths() {
    applyNewColumnWidths(0, getBoundedColumnWidths(false), false);
    scrollTables(false);
  }

  public void setBulkRenderer(FixedWidthGridBulkRenderer<RowType> bulkRenderer) {
    this.bulkRenderer = bulkRenderer;
  }

  public void setCellPadding(int padding) {
    headerTable.setCellPadding(padding);
    dataTable.setCellPadding(padding);
    if (footerTable != null) {
      footerTable.setCellPadding(padding);
    }
    redraw();
  }

  public void setCellSpacing(int spacing) {
    headerTable.setCellSpacing(spacing);
    dataTable.setCellSpacing(spacing);
    if (footerTable != null) {
      footerTable.setCellSpacing(spacing);
    }
    redraw();
  }

  public void setColumnResizePolicy(ColumnResizePolicy columnResizePolicy) {
    this.columnResizePolicy = columnResizePolicy;
  }

  public int setColumnWidth(int column, int width) {
    ColumnWidthInfo info = getColumnWidthInfo(column);
    if (info.hasMaximumWidth()) {
      width = Math.min(width, info.getMaximumWidth());
    }
    if (info.hasMinimumWidth()) {
      width = Math.max(width, info.getMinimumWidth());
    }

    if (resizePolicy.isSacrificial()) {
      int sacrificeColumn = column + 1;
      int numColumns = dataTable.getColumnCount();
      int remainingColumns = numColumns - sacrificeColumn;
      List<ColumnWidthInfo> infos = getColumnWidthInfo(sacrificeColumn,
          remainingColumns);

      int diff = width - getColumnWidth(column);
      int undistributed = columnResizer.distributeWidth(infos, -diff);

      applyNewColumnWidths(sacrificeColumn, infos, false);

      if (resizePolicy.isFixedWidth()) {
        width += undistributed;
      }
    }

    int offset = getHeaderOffset();
    dataTable.setColumnWidth(column, width);
    headerTable.setColumnWidth(column + offset, width);
    if (footerTable != null) {
      footerTable.setColumnWidth(column + offset, width);
    }

    repositionSpacer(this, false);
    resizeTablesVertically();
    scrollTables(false);
    return width;
  }

  public void setCrossPageSelectionEnabled(boolean enabled) {
    if (isCrossPageSelectionEnabled != enabled) {
      this.isCrossPageSelectionEnabled = enabled;

      if (!enabled) {
        selectedRowValues = new HashSet<RowType>();
        Set<Integer> selectedRows = getDataTable().getSelectedRows();
        for (Integer selectedRow : selectedRows) {
          selectedRowValues.add(getRowValue(selectedRow));
        }
      }
    }
  }

  public void setEmptyTableWidget(Widget emptyTableWidget) {
    emptyTableWidgetWrapper.setWidget(emptyTableWidget);
  }

  public void setFooterGenerated(boolean isGenerated) {
    this.isFooterGenerated = isGenerated;
    if (isGenerated) {
      refreshFooterTable();
    }
  }

  public void setFooterTable(FixedWidthFlexTable footerTable) {
    if (this.footerTable != null) {
      super.remove(this.footerTable);
      DOM.removeChild(absoluteElem, footerWrapper);
    }

    this.footerTable = footerTable;
    if (footerTable != null) {
      footerTable.setCellSpacing(getCellSpacing());
      footerTable.setCellPadding(getCellPadding());
      prepareTable(footerTable, "footerTable");
      if (dataTable.getSelectionPolicy().hasInputColumn()) {
        footerTable.setColumnWidth(0, dataTable.getInputColumnWidth());
      }

      if (footerWrapper == null) {
        footerWrapper = createWrapper("footerWrapper", "st-footer");
        DOM.setEventListener(footerWrapper, this);
        DOM.sinkEvents(footerWrapper, Event.ONMOUSEUP);
      }

      adoptTable(footerTable, footerWrapper,
          absoluteElem.getChildNodes().getLength());
    }
    redraw();
  }

  public void setHeaderGenerated(boolean isGenerated) {
    this.isHeaderGenerated = isGenerated;
    if (isGenerated) {
      refreshHeaderTable();
    }
  }

  @Override
  public void setHeight(String height) {
    this.lastHeight = height;
    super.setHeight(height);
    resizeTablesVertically();
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setPageSize(int pageSize) {
    pageSize = Math.max(0, pageSize);
    this.pageSize = pageSize;

    int pageCount = getPageCount();
    if (pageCount != oldPageCount) {
      fireEvent(new PageCountChangeEvent(oldPageCount, pageCount));
      oldPageCount = pageCount;
    }

    if (currentPage >= 0) {
      gotoPage(currentPage, true);
    }
  }

  public void setResizePolicy(ResizePolicy resizePolicy) {
    this.resizePolicy = resizePolicy;
    redraw();
  }

  public void setRowValue(int row, RowType value) {
    for (int i = rowValues.size(); i <= row; i++) {
      rowValues.add(null);
    }

    rowValues.set(row, value);
    refreshRow(row);
  }

  public void setScrollPolicy(ScrollPolicy scrollPolicy) {
    if (scrollPolicy == this.scrollPolicy) {
      return;
    }
    this.scrollPolicy = scrollPolicy;

    headerWrapper.getStyle().clearHeight();
    dataWrapper.getStyle().clearHeight();
    if (footerWrapper != null) {
      footerWrapper.getStyle().clearHeight();
    }

    if (scrollPolicy == ScrollPolicy.DISABLED) {
      BeeKeeper.getStyle().autoHeight(dataWrapper);
      dataWrapper.getStyle().clearOverflow();
    } else if (scrollPolicy == ScrollPolicy.HORIZONTAL) {
      BeeKeeper.getStyle().autoHeight(dataWrapper);
      dataWrapper.getStyle().setOverflow(Overflow.AUTO);
    } else if (scrollPolicy == ScrollPolicy.BOTH) {
      if (lastHeight != null) {
        super.setHeight(lastHeight);
      } else {
        getElement().getStyle().clearHeight();
      }
      dataWrapper.getStyle().setOverflow(Overflow.AUTO);
    }

    repositionSpacer(this, true);
    redraw();
  }

  public void setSortPolicy(SortPolicy sortPolicy) {
    this.sortPolicy = sortPolicy;
    applySortedColumnIndicator(null, true);
  }

  public void setTableDefinition(TableDefinition<RowType> tableDefinition) {
    Assert.notNull(tableDefinition, "tableDefinition cannot be null");
    this.tableDefinition = tableDefinition;
  }

  protected void applySortedColumnIndicator(Element tdElem, boolean ascending) {
    if (tdElem == null) {
      Element parent = DOM.getParent(sortedColumnWrapper);
      if (parent != null) {
        parent.removeChild(sortedColumnWrapper);
        headerTable.clearIdealWidths();
      }
      return;
    }

    tdElem.appendChild(sortedColumnWrapper);
    if (ascending) {
      sortedColumnWrapper.setInnerHTML(BeeConst.HTML_NBSP
          + AbstractImagePrototype.create(BeeGlobal.getImages().ascending()).getHTML());
    } else {
      sortedColumnWrapper.setInnerHTML(BeeConst.HTML_NBSP
          + AbstractImagePrototype.create(BeeGlobal.getImages().descending()).getHTML());
    }
    sortedRowIndex = -1;
    sortedCellIndex = -1;

    headerTable.clearIdealWidths();
    redraw();
  }

  protected Element createWrapper(String cssName, String idPrefix) {
    Element wrapper = DOM.createDiv();

    wrapper.getStyle().setWidth(100, Unit.PCT);
    wrapper.getStyle().setOverflow(Overflow.HIDDEN);
    wrapper.getStyle().setPadding(0, Unit.PX);
    wrapper.getStyle().setMargin(0, Unit.PX);
    wrapper.getStyle().setBorderWidth(0, Unit.PX);

    if (cssName != null) {
      setStyleName(wrapper, cssName);
    }
    DomUtils.createId(wrapper, idPrefix);

    return wrapper;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void editCell(int row, int column) {
    final ColumnDefinition colDef = getColumnDefinition(column);
    if (colDef == null) {
      return;
    }
    CellEditor cellEditor = colDef.getCellEditor();
    if (cellEditor == null) {
      return;
    }

    final RowType rowValue = getRowValue(row);
    CellEditInfo editInfo = new CellEditInfo(getDataTable(), row, column);
    cellEditor.editCell(editInfo, colDef.getCellValue(rowValue),
        new CellEditor.Callback() {
          public void onCancel(CellEditInfo cellEditInfo) {
          }

          public void onComplete(CellEditInfo cellEditInfo, Object cellValue) {
            colDef.setCellValue(rowValue, cellValue);
            if (tableModel instanceof MutableTableModel) {
              int row = getAbsoluteFirstRowIndex() + cellEditInfo.getRowIndex();
              ((MutableTableModel<RowType>) tableModel).setRowValue(row, rowValue);
            } else {
              refreshRow(cellEditInfo.getRowIndex());
            }
          }
        });
  }

  protected ColumnDefinition<RowType, ?> getColumnDefinition(int colIndex) {
    if (colIndex < visibleColumns.size()) {
      return visibleColumns.get(colIndex);
    }
    return null;
  }

  protected Element getDataWrapper() {
    return dataWrapper;
  }

  protected List<RowType> getRowValues() {
    return rowValues;
  }

  protected Widget getSelectAllWidget() {
    if (selectAllWidget == null) {
      final CheckBox box = new CheckBox();
      selectAllWidget = box;
      box.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          if (box.getValue()) {
            getDataTable().selectAllRows();
          } else {
            getDataTable().deselectAllRows();
          }
        }
      });
    }
    return selectAllWidget;
  }

  protected List<ColumnDefinition<RowType, ?>> getVisibleColumnDefinitions() {
    return visibleColumns;
  }

  protected void insertAbsoluteRow(int beforeRow) {
    int lastRow = getAbsoluteLastRowIndex() + 1;
    if (beforeRow <= lastRow) {
      int firstRow = getAbsoluteFirstRowIndex();
      if (beforeRow >= firstRow) {
        getDataTable().insertRow(beforeRow - firstRow);
      } else {
        getDataTable().insertRow(0);
      }
      if (getDataTable().getRowCount() > pageSize) {
        getDataTable().removeRow(pageSize);
      }
    }
  }

  protected void onDataTableRendered() {
    if (headersObsolete) {
      refreshHeaderTable();
      refreshFooterTable();
      headersObsolete = false;
    }

    FixedWidthGrid data = getDataTable();
    int rowCount = data.getRowCount();
    for (int i = 0; i < rowCount; i++) {
      if (selectedRowValues.contains(getRowValue(i))) {
        data.selectRow(i, false);
      }
    }

    data.clearIdealWidths();
    redraw();
    isPageLoading = false;
    fireEvent(new PageLoadEvent(currentPage));
  }

  protected void onLoad() {
    gotoFirstPage();
    redraw();
  }

  protected void refreshFooterTable() {
    if (!isFooterGenerated) {
      return;
    }

    List<List<ColumnHeaderInfo>> allInfos = new ArrayList<List<ColumnHeaderInfo>>();
    int columnCount = visibleColumns.size();
    int footerCounts[] = new int[columnCount];
    int maxFooterCount = 0;

    for (int col = 0; col < columnCount; col++) {
      ColumnDefinition<RowType, ?> colDef = visibleColumns.get(col);
      FooterProperty prop = colDef.getColumnProperty(FooterProperty.TYPE);
      int footerCount = prop.getFooterCount();
      footerCounts[col] = footerCount;
      maxFooterCount = Math.max(maxFooterCount, footerCount);

      List<ColumnHeaderInfo> infos = new ArrayList<ColumnHeaderInfo>();
      ColumnHeaderInfo prev = null;

      for (int row = 0; row < footerCount; row++) {
        Object footer = prop.getFooter(row);
        if (prev != null && prev.header.equals(footer)) {
          prev.incrementRowSpan();
        } else {
          prev = new ColumnHeaderInfo(footer);
          infos.add(prev);
        }
      }
      allInfos.add(infos);
    }

    if (maxFooterCount == 0) {
      return;
    }

    for (int col = 0; col < columnCount; col++) {
      int footerCount = footerCounts[col];
      if (footerCount < maxFooterCount) {
        allInfos.get(col).add(
            new ColumnHeaderInfo(null, maxFooterCount - footerCount));
      }
    }

    if (getFooterTable() == null) {
      setFooterTable(new FixedWidthFlexTable());
    }

    refreshHeaderTable(getFooterTable(), allInfos, false);
  }

  protected void refreshHeaderTable() {
    if (!isHeaderGenerated) {
      return;
    }

    List<List<ColumnHeaderInfo>> allInfos = new ArrayList<List<ColumnHeaderInfo>>();
    int columnCount = visibleColumns.size();
    int headerCounts[] = new int[columnCount];
    int maxHeaderCount = 0;

    for (int col = 0; col < columnCount; col++) {
      ColumnDefinition<RowType, ?> colDef = visibleColumns.get(col);
      HeaderProperty prop = colDef.getColumnProperty(HeaderProperty.TYPE);
      int headerCount = prop.getHeaderCount();
      headerCounts[col] = headerCount;
      maxHeaderCount = Math.max(maxHeaderCount, headerCount);

      List<ColumnHeaderInfo> infos = new ArrayList<ColumnHeaderInfo>();
      ColumnHeaderInfo prev = null;
      for (int row = 0; row < headerCount; row++) {
        Object header = prop.getHeader(row);
        if (prev != null && prev.header.equals(header)) {
          prev.incrementRowSpan();
        } else {
          prev = new ColumnHeaderInfo(header);
          infos.add(0, prev);
        }
      }
      allInfos.add(infos);
    }

    if (maxHeaderCount == 0) {
      return;
    }

    for (int col = 0; col < columnCount; col++) {
      int headerCount = headerCounts[col];
      if (headerCount < maxHeaderCount) {
        allInfos.get(col).add(0,
            new ColumnHeaderInfo(null, maxHeaderCount - headerCount));
      }
    }

    refreshHeaderTable(getHeaderTable(), allInfos, true);
  }

  protected void refreshVisibleColumnDefinitions() {
    List<ColumnDefinition<RowType, ?>> colDefs = new ArrayList<ColumnDefinition<RowType, ?>>(
        tableDefinition.getVisibleColumnDefinitions());
    if (!colDefs.equals(visibleColumns)) {
      visibleColumns = colDefs;
      headersObsolete = true;
    } else {
      for (ColumnDefinition<RowType, ?> colDef : colDefs) {
        if (colDef.getColumnProperty(HeaderProperty.TYPE).isDynamic()
            || colDef.getColumnProperty(FooterProperty.TYPE).isDynamic()) {
          headersObsolete = true;
          return;
        }
      }
    }
  }

  protected void removeAbsoluteRow(int row) {
    int firstRow = getAbsoluteFirstRowIndex();
    int lastRow = getAbsoluteLastRowIndex();
    if (row <= lastRow && row >= firstRow) {
      FixedWidthGrid data = getDataTable();
      int relativeRow = row - firstRow;
      if (relativeRow < data.getRowCount()) {
        data.removeRow(relativeRow);
      }
    }
  }

  protected void resizeTablesVertically() {
    if (scrollPolicy == ScrollPolicy.DISABLED) {
      dataWrapper.getStyle().setOverflow(Overflow.AUTO);
      dataWrapper.getStyle().clearOverflow();
      int height = Math.max(1, absoluteElem.getOffsetHeight());
      getElement().getStyle().setHeight(height, Unit.PX);

    } else if (scrollPolicy == ScrollPolicy.HORIZONTAL) {
      dataWrapper.getStyle().setOverflow(Overflow.HIDDEN);
      dataWrapper.getStyle().setOverflow(Overflow.AUTO);
      int height = Math.max(1, absoluteElem.getOffsetHeight());
      getElement().getStyle().setHeight(height, Unit.PX);

    } else {
      applyTableWrapperSizes(getTableWrapperSizes());
      dataWrapper.getStyle().setWidth(100, Unit.PCT);
    }
  }

  protected void scrollTables(boolean baseHeader) {
    if (scrollPolicy == ScrollPolicy.DISABLED) {
      return;
    }

    if (lastScrollLeft >= 0) {
      headerWrapper.setScrollLeft(lastScrollLeft);
      if (baseHeader) {
        dataWrapper.setScrollLeft(lastScrollLeft);
      }
      if (footerWrapper != null) {
        footerWrapper.setScrollLeft(lastScrollLeft);
      }
    }
  }

  protected void setData(int firstRow, Iterator<RowType> rows) {
    getDataTable().deselectAllRows();
    rowValues = new ArrayList<RowType>();
    if (rows != null && rows.hasNext()) {
      setEmptyTableWidgetVisible(false);

      int firstVisibleRow = getAbsoluteFirstRowIndex();
      int lastVisibleRow = getAbsoluteLastRowIndex();
      Iterator<RowType> visibleIter = new VisibleRowsIterator(rows, firstRow,
          firstVisibleRow, lastVisibleRow);

      while (visibleIter.hasNext()) {
        rowValues.add(visibleIter.next());
      }

      refreshVisibleColumnDefinitions();

      if (bulkRenderer != null) {
        bulkRenderer.renderRows(rowValues.iterator(), tableRendererCallback);
        return;
      }

      int rowCount = rowValues.size();
      int colCount = visibleColumns.size();
      getDataTable().resize(rowCount, colCount);

      tableDefinition.renderRows(0, rowValues.iterator(), rowView);
    } else {
      setEmptyTableWidgetVisible(true);
    }

    onDataTableRendered();
  }

  protected void setEmptyTableWidgetVisible(boolean visible) {
    emptyTableWidgetWrapper.setVisible(visible);
    if (visible) {
      getDataWrapper().getStyle().setDisplay(Display.NONE);
    } else {
      getDataWrapper().getStyle().clearDisplay();
    }
  }

  Element getAbsoluteElement() {
    return absoluteElem;
  }

  void resizeSpacer(FixedWidthFlexTable table, int spacerWidth) {
    if (spacerWidth == getSpacerWidth(table)) {
      return;
    }

    table.getElement().getStyle().setPaddingRight(spacerWidth, Unit.PX);
  }

  private void adoptTable(Widget table, Element wrapper, int index) {
    DOM.insertChild(absoluteElem, wrapper, index);
    add(table, wrapper);
  }

  private void applyNewColumnWidths(int startIndex,
      List<ColumnWidthInfo> infos, boolean forced) {
    if (infos == null) {
      return;
    }

    int offset = getHeaderOffset();
    int numColumns = infos.size();

    for (int i = 0; i < numColumns; i++) {
      ColumnWidthInfo info = infos.get(i);
      int newWidth = info.getNewWidth();

      if (forced || info.getCurrentWidth() != newWidth) {
        dataTable.setColumnWidth(startIndex + i, newWidth);
        headerTable.setColumnWidth(startIndex + i + offset, newWidth);
        if (footerTable != null) {
          footerTable.setColumnWidth(startIndex + i + offset, newWidth);
        }
      }
    }
    repositionSpacer(this, false);
  }

  private void applyTableWrapperSizes(TableHeightInfo sizes) {
    if (sizes == null) {
      return;
    }

    headerWrapper.getStyle().setHeight(sizes.headerTableHeight, Unit.PX);
    if (footerWrapper != null) {
      footerWrapper.getStyle().setHeight(sizes.footerTableHeight, Unit.PX);
    }
    dataWrapper.getStyle().setHeight(Math.max(sizes.dataTableHeight, 0), Unit.PX);
    dataWrapper.getStyle().setOverflow(Overflow.HIDDEN);
    dataWrapper.getStyle().setOverflow(Overflow.AUTO);
  }

  private List<ColumnWidthInfo> getBoundedColumnWidths(boolean boundsOnly) {
    if (!isAttached()) {
      return null;
    }

    int numColumns = dataTable.getColumnCount();
    int totalWidth = 0;
    List<ColumnWidthInfo> colWidthInfos = getColumnWidthInfo(0, numColumns);

    if (!boundsOnly) {
      for (ColumnWidthInfo info : colWidthInfos) {
        totalWidth += info.getCurrentWidth();
        info.setCurrentWidth(0);
      }
    }

    columnResizer.distributeWidth(colWidthInfos, totalWidth);

    return colWidthInfos;
  }

  private ColumnWidthInfo getColumnWidthInfo(int column) {
    int minWidth = getMinimumColumnWidth(column);
    int maxWidth = getMaximumColumnWidth(column);
    int preferredWidth = getPreferredColumnWidth(column);
    int curWidth = getColumnWidth(column);

    if (!isColumnTruncatable(column)) {
      maybeRecalculateIdealColumnWidths();
      int idealWidth = getDataTable().getIdealColumnWidth(column);
      if (maxWidth != MaximumWidthProperty.NO_MAXIMUM_WIDTH) {
        idealWidth = Math.min(idealWidth, maxWidth);
      }
      minWidth = Math.max(minWidth, idealWidth);
    }
    if (!isHeaderColumnTruncatable(column)) {
      maybeRecalculateIdealColumnWidths();
      int idealWidth = getHeaderTable().getIdealColumnWidth(
          column + getHeaderOffset());
      if (maxWidth != MaximumWidthProperty.NO_MAXIMUM_WIDTH) {
        idealWidth = Math.min(idealWidth, maxWidth);
      }
      minWidth = Math.max(minWidth, idealWidth);
    }
    if (footerTable != null && !isFooterColumnTruncatable(column)) {
      maybeRecalculateIdealColumnWidths();
      int idealWidth = getFooterTable().getIdealColumnWidth(
          column + getHeaderOffset());
      if (maxWidth != MaximumWidthProperty.NO_MAXIMUM_WIDTH) {
        idealWidth = Math.min(idealWidth, maxWidth);
      }
      minWidth = Math.max(minWidth, idealWidth);
    }

    return new ColumnWidthInfo(minWidth, maxWidth, preferredWidth, curWidth);
  }

  private List<ColumnWidthInfo> getColumnWidthInfo(int column, int numColumns) {
    List<ColumnWidthInfo> infos = new ArrayList<ColumnWidthInfo>();
    for (int i = 0; i < numColumns; i++) {
      infos.add(getColumnWidthInfo(column + i));
    }
    return infos;
  }

  private List<ColumnWidthInfo> getFillColumnWidths(TableWidthInfo info) {
    if (!isAttached()) {
      return null;
    }

    if (info == null) {
      info = new TableWidthInfo(false);
    }

    int clientWidth = info.availableWidth;
    if (clientWidth <= 0) {
      return null;
    }

    int diff = 0;
    int numColumns = 0;
    {
      int numHeaderCols = 0;
      int numDataCols = 0;
      int numFooterCols = 0;
      if (info.headerTableWidth > 0) {
        numHeaderCols = headerTable.getColumnCount() - getHeaderOffset();
      }
      if (info.dataTableWidth > 0) {
        numDataCols = dataTable.getColumnCount();
      }
      if (footerTable != null && info.footerTableWidth > 0) {
        numFooterCols = footerTable.getColumnCount() - getHeaderOffset();
      }

      if (numHeaderCols >= numDataCols && numHeaderCols >= numFooterCols) {
        numColumns = numHeaderCols;
        diff = clientWidth - info.headerTableWidth;
      } else if (numFooterCols >= numDataCols && numFooterCols >= numHeaderCols) {
        numColumns = numFooterCols;
        diff = clientWidth - info.footerTableWidth;
      } else if (numDataCols > 0) {
        numColumns = numDataCols;
        diff = clientWidth - info.dataTableWidth;
      }
    }
    if (numColumns <= 0) {
      return null;
    }

    List<ColumnWidthInfo> colWidthInfos = getColumnWidthInfo(0, numColumns);
    columnResizer.distributeWidth(colWidthInfos, diff);

    return colWidthInfos;
  }

  private int getHeaderOffset() {
    if (dataTable.getSelectionPolicy().hasInputColumn()) {
      return 1;
    }
    return 0;
  }

  private int getSpacerWidth(FixedWidthFlexTable table) {
    String paddingStr = table.getElement().getStyle().getPaddingRight();

    if (paddingStr == null || paddingStr.length() < 3) {
      return -1;
    }

    try {
      return Integer.parseInt(paddingStr.substring(0, paddingStr.length() - 2));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private TableHeightInfo getTableWrapperSizes() {
    if (!isAttached()) {
      return null;
    }

    if (scrollPolicy == ScrollPolicy.DISABLED
        || scrollPolicy == ScrollPolicy.HORIZONTAL) {
      return null;
    }

    return new TableHeightInfo();
  }

  private void maybeRecalculateIdealColumnWidths() {
    if (!isAttached()) {
      return;
    }

    if (headerTable.isIdealColumnWidthsCalculated()
        && dataTable.isIdealColumnWidthsCalculated()
        && (footerTable == null || footerTable.isIdealColumnWidthsCalculated())) {
      return;
    }

    recalculateIdealColumnWidths(this);
  }

  private void prepareTable(Widget table, String cssName) {
    Element tableElem = table.getElement();
    tableElem.getStyle().setMargin(0, Unit.PX);
    tableElem.getStyle().setBorderWidth(0, Unit.PX);
    table.addStyleName(cssName);
  }

  private void refreshHeaderTable(FixedWidthFlexTable table,
      List<List<ColumnHeaderInfo>> allInfos, boolean isHeader) {
    if (visibleColumns == null) {
      return;
    }

    int rowCount = table.getRowCount();
    for (int i = 0; i < rowCount; i++) {
      table.removeRow(0);
    }

    int columnCount = allInfos.size();
    FlexCellFormatter formatter = table.getFlexCellFormatter();
    List<ColumnHeaderInfo> prevInfos = null;

    for (int col = 0; col < columnCount; col++) {
      List<ColumnHeaderInfo> infos = allInfos.get(col);
      int row = 0;
      for (ColumnHeaderInfo info : infos) {
        int rowSpan = info.getRowSpan();
        int cell = 0;
        if (table.getRowCount() > row) {
          cell = table.getCellCount(row);
        }

        if (prevInfos != null) {
          boolean headerAdded = false;
          int prevRow = 0;
          for (ColumnHeaderInfo prevInfo : prevInfos) {
            if (prevRow == row && info.equals(prevInfo)) {
              int colSpan = formatter.getColSpan(row, cell - 1);
              formatter.setColSpan(row, cell - 1, colSpan + 1);
              headerAdded = true;
              break;
            }
            prevRow += prevInfo.getRowSpan();
          }

          if (headerAdded) {
            row += rowSpan;
            continue;
          }
        }

        Object header = info.getHeader();
        if (header instanceof Widget) {
          table.setWidget(row, cell, (Widget) header);
        } else {
          table.setHTML(row, cell, header.toString());
        }

        if (rowSpan > 1) {
          formatter.setRowSpan(row, cell, rowSpan);
        }

        row += rowSpan;
      }

      prevInfos = infos;
    }

    SelectionPolicy selectionPolicy = getDataTable().getSelectionPolicy();
    if (selectionPolicy.hasInputColumn()) {
      Widget box = null;
      if (isHeader
          && getDataTable().getSelectionPolicy() == SelectionPolicy.CHECKBOX) {
        box = getSelectAllWidget();
      }

      table.insertCell(0, 0);
      if (box != null) {
        table.setWidget(0, 0, box);
      } else {
        table.setHTML(0, 0, BeeConst.HTML_NBSP);
      }
      formatter.setRowSpan(0, 0, table.getRowCount());
      formatter.setHorizontalAlignment(0, 0,
          HasHorizontalAlignment.ALIGN_CENTER);
      table.setColumnWidth(0, getDataTable().getInputColumnWidth());
    }
  }

  private void refreshRow(int rowIndex) {
    final RowType rowValue = getRowValue(rowIndex);
    Iterator<RowType> singleIterator = new Iterator<RowType>() {
      private boolean nextCalled = false;

      public boolean hasNext() {
        return !nextCalled;
      }

      public RowType next() {
        if (!hasNext()) {
          Assert.untouchable("no such element");
        }
        nextCalled = true;
        return rowValue;
      }

      public void remove() {
        Assert.untouchable();
      }
    };
    tableDefinition.renderRows(rowIndex, singleIterator, rowView);
  }
}
