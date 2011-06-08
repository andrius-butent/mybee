package com.butent.bee.client.view.grid;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RowCountChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.HasDataTable;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.Font;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.dom.Rulers;
import com.butent.bee.client.dom.Selectors;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.Modifiers;
import com.butent.bee.client.grid.AbstractColumn;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.ui.ConditionalStyle;
import com.butent.bee.client.ui.StyleDescriptor;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditorFactory;
import com.butent.bee.client.view.edit.HasEditStartHandlers;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.HasId;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.ActiveRowChangeEvent;
import com.butent.bee.shared.data.event.CellUpdateEvent;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.SelectionCountChangeEvent;
import com.butent.bee.shared.data.event.SortEvent;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.GridComponentDescription;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the structure and behavior of a cell grid user interface component.
 */

public class CellGrid extends Widget implements HasId, HasDataTable, HasEditStartHandlers {

  /**
   * Contains a list of possible resizing modes (horizontal, vertical).
   */

  public enum ResizerMode {
    HORIZONTAL(10, 4), VERTICAL(10, 4);

    int handlePx;
    int barPx;

    private ResizerMode(int handlePx, int barPx) {
      this.handlePx = handlePx;
      this.barPx = barPx;
    }

    public int getBarPx() {
      return barPx;
    }

    public int getHandlePx() {
      return handlePx;
    }

    public void setBarPx(int barPx) {
      this.barPx = barPx;
    }

    public void setHandlePx(int handlePx) {
      this.handlePx = handlePx;
    }
  }

  /**
   * Contains templates which facilitates compile-time binding of HTML templates to generate
   * SafeHtml strings.
   */

  public interface Template extends SafeHtmlTemplates {
    @Template("<div data-row=\"{0}\" data-col=\"{1}\" class=\"{2}\" style=\"{3}\">{4}</div>")
    SafeHtml cell(String rowIdx, int colIdx, String classes, SafeStyles styles, SafeHtml contents);

    @Template("<div data-row=\"{0}\" data-col=\"{1}\" class=\"{2}\" style=\"{3}\" tabindex=\"{4}\">{5}</div>")
    SafeHtml cellFocusable(String rowIdx, int colIdx, String classes, SafeStyles styles,
        int tabIndex, SafeHtml contents);

    @Template("<div class=\"{0}\">{1}</div>")
    SafeHtml emptiness(String classes, String text);

    @Template("<div id=\"{0}\" style=\"position:absolute; top:-64px; left:-64px;\">{1}{2}</div>")
    SafeHtml resizer(String id, SafeHtml handle, SafeHtml bar);

    @Template("<div id=\"{0}\" style=\"position:absolute;\"></div>")
    SafeHtml resizerBar(String id);

    @Template("<div id=\"{0}\" style=\"position:absolute;\"></div>")
    SafeHtml resizerHandle(String id);
  }
  
  private class CellInfo {
    private int width;
    private int height;

    private Edges nextRowPadding = null;
    private Edges nextRowBorders = null;

    private Edges nextColumnPadding = null;
    private Edges nextColumnBorders = null;

    private CellInfo(int width, int height) {
      this.width = width;
      this.height = height;
    }

    private int getHeight() {
      return height;
    }

    private Edges getNextColumnBorders() {
      return nextColumnBorders;
    }

    private Edges getNextColumnPadding() {
      return nextColumnPadding;
    }

    private Edges getNextRowBorders() {
      return nextRowBorders;
    }

    private Edges getNextRowPadding() {
      return nextRowPadding;
    }

    private int getWidth() {
      return width;
    }

    private void setHeight(int height) {
      this.height = height;
    }

    private void setNextColumnBorders(Edges nextColumnBorders) {
      this.nextColumnBorders = nextColumnBorders;
    }

    private void setNextColumnPadding(Edges nextColumnPadding) {
      this.nextColumnPadding = nextColumnPadding;
    }

    private void setNextRowBorders(Edges nextRowBorders) {
      this.nextRowBorders = nextRowBorders;
    }

    private void setNextRowPadding(Edges nextRowPadding) {
      this.nextRowPadding = nextRowPadding;
    }

    private void setSize(int width, int height) {
      setWidth(width);
      setHeight(height);
    }

    private void setWidth(int width) {
      this.width = width;
    }
  }

  private class ColumnInfo {
    private final String columnId;
    private final int dataIndex;

    private final AbstractColumn<?> column;
    private final ColumnHeader header;
    private final ColumnFooter footer;

    private int width = BeeConst.UNDEF;
    private int minWidth = BeeConst.UNDEF;
    private int maxWidth = BeeConst.UNDEF;

    private int headerWidth = BeeConst.UNDEF;
    private int bodyWidth = BeeConst.UNDEF;
    private int footerWidth = BeeConst.UNDEF;

    private StyleDescriptor headerStyle = null;
    private StyleDescriptor bodyStyle = null;
    private StyleDescriptor footerStyle = null;

    private ConditionalStyle dynStyles = null;

    private boolean colReadOnly = false;

    private ColumnInfo(String columnId, int dataIndex,
        AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer) {
      this.columnId = columnId;
      this.dataIndex = dataIndex;

      this.column = column;
      this.header = header;
      this.footer = footer;
    }

    private void buildSafeStyles(SafeStylesBuilder stylesBuilder, ComponentType componentType) {
      StyleDescriptor sd = getStyleDescriptor(componentType);
      if (sd == null) {
        return;
      }
      sd.buildSafeStyles(stylesBuilder);
    }

    private void ensureBodyWidth(int w) {
      if (w > 0) {
        setBodyWidth(Math.max(getBodyWidth(), w));
      }
    }

    private void ensureFooterWidth(int w) {
      if (w > 0) {
        setFooterWidth(Math.max(getFooterWidth(), w));
      }
    }

    private void ensureHeaderWidth(int w) {
      if (w > 0) {
        setHeaderWidth(Math.max(getHeaderWidth(), w));
      }
    }

    private Font getBodyFont() {
      if (getBodyStyle() == null) {
        return null;
      }
      return getBodyStyle().getFont();
    }

    private StyleDescriptor getBodyStyle() {
      return bodyStyle;
    }

    private int getBodyWidth() {
      return bodyWidth;
    }

    private String getClassName(ComponentType componentType) {
      StyleDescriptor sd = getStyleDescriptor(componentType);
      if (sd == null) {
        return null;
      }
      return sd.getClassName();
    }

    private AbstractColumn<?> getColumn() {
      return column;
    }

    private String getColumnId() {
      return columnId;
    }

    private int getColumnWidth() {
      if (getWidth() > 0) {
        return getWidth();
      }

      int w;
      if (getBodyWidth() > 0) {
        w = Math.max(getBodyWidth(), getHeaderWidth());
      } else if (getHeaderWidth() > 0) {
        w = getHeaderWidth();
      } else {
        w = getFooterWidth();
      }

      int minW = getMinWidth();
      if (minW <= 0) {
        minW = getMinCellWidth();
      }
      int maxW = getMaxWidth();
      if (maxW <= 0) {
        maxW = getMaxCellWidth();
      }
      return BeeUtils.limit(w, minW, maxW);
    }

    private int getDataIndex() {
      return dataIndex;
    }

    private ConditionalStyle getDynStyles() {
      return dynStyles;
    }

    private ColumnFooter getFooter() {
      return footer;
    }

    private Font getFooterFont() {
      if (getFooterStyle() == null) {
        return null;
      }
      return getFooterStyle().getFont();
    }

    private StyleDescriptor getFooterStyle() {
      return footerStyle;
    }

    private int getFooterWidth() {
      return footerWidth;
    }

    private ColumnHeader getHeader() {
      return header;
    }

    private Font getHeaderFont() {
      if (getHeaderStyle() == null) {
        return null;
      }
      return getHeaderStyle().getFont();
    }

    private StyleDescriptor getHeaderStyle() {
      return headerStyle;
    }

    private int getHeaderWidth() {
      return headerWidth;
    }

    private int getMaxWidth() {
      return maxWidth;
    }

    private int getMinWidth() {
      return minWidth;
    }

    private StyleDescriptor getStyleDescriptor(ComponentType componentType) {
      switch (componentType) {
        case HEADER:
          return getHeaderStyle();
        case BODY:
          return getBodyStyle();
        case FOOTER:
          return getFooterStyle();
      }
      return null;
    }

    private int getWidth() {
      return width;
    }

    private boolean is(String id) {
      return BeeUtils.same(getColumnId(), id);
    }
    
    private boolean isCalculated() {
      return ColType.CALCULATED.equals(getColumn().getColType());
    }

    private boolean isColReadOnly() {
      return colReadOnly;
    }

    private void setBodyFont(String fontDeclaration) {
      if (getBodyStyle() == null) {
        if (!BeeUtils.isEmpty(fontDeclaration)) {
          setBodyStyle(new StyleDescriptor(null, null, fontDeclaration));
        }
      } else {
        getBodyStyle().setFontDeclaration(fontDeclaration);
      }
    }

    private void setBodyStyle(StyleDescriptor bodyStyle) {
      this.bodyStyle = bodyStyle;
    }

    private void setBodyWidth(int bodyWidth) {
      this.bodyWidth = bodyWidth;
    }

    private void setColReadOnly(boolean colReadOnly) {
      this.colReadOnly = colReadOnly;
    }

    private void setDynStyles(ConditionalStyle dynStyles) {
      this.dynStyles = dynStyles;
    }

    private void setFooterFont(String fontDeclaration) {
      if (getFooterStyle() == null) {
        if (!BeeUtils.isEmpty(fontDeclaration)) {
          setFooterStyle(new StyleDescriptor(null, null, fontDeclaration));
        }
      } else {
        getFooterStyle().setFontDeclaration(fontDeclaration);
      }
    }

    private void setFooterStyle(StyleDescriptor footerStyle) {
      this.footerStyle = footerStyle;
    }

    private void setFooterWidth(int footerWidth) {
      this.footerWidth = footerWidth;
    }

    private void setHeaderFont(String fontDeclaration) {
      if (getHeaderStyle() == null) {
        if (!BeeUtils.isEmpty(fontDeclaration)) {
          setHeaderStyle(new StyleDescriptor(null, null, fontDeclaration));
        }
      } else {
        getHeaderStyle().setFontDeclaration(fontDeclaration);
      }
    }

    private void setHeaderStyle(StyleDescriptor headerStyle) {
      this.headerStyle = headerStyle;
    }

    private void setHeaderWidth(int headerWidth) {
      this.headerWidth = headerWidth;
    }

    private void setMaxWidth(int maxWidth) {
      this.maxWidth = maxWidth;
    }

    private void setMinWidth(int minWidth) {
      this.minWidth = minWidth;
    }

    private void setWidth(int width) {
      this.width = width;
    }
  }

  private class Component {
    private StyleDescriptor style = null;

    private int cellHeight = BeeConst.UNDEF;
    private int minHeight = BeeConst.UNDEF;
    private int maxHeight = BeeConst.UNDEF;

    private Edges padding = null;
    private Edges borderWidth = null;
    private Edges margin = null;

    private Component(int cellHeight, int minHeight, int maxHeight,
        Edges padding, Edges borderWidth, Edges margin) {
      this.cellHeight = cellHeight;
      this.minHeight = minHeight;
      this.maxHeight = maxHeight;
      this.padding = padding;
      this.borderWidth = borderWidth;
      this.margin = margin;
    }

    private void buildSafeStyles(SafeStylesBuilder stylesBuilder) {
      if (getSafeStyles() != null) {
        stylesBuilder.append(getSafeStyles());
      }
      if (getFont() != null) {
        stylesBuilder.append(getFont().buildCss());
      }
    }

    private Edges getBorderWidth() {
      return borderWidth;
    }

    private int getCellHeight() {
      return cellHeight;
    }

    private String getClassName() {
      if (getStyle() == null) {
        return null;
      }
      return getStyle().getClassName();
    }

    private Font getFont() {
      if (getStyle() == null) {
        return null;
      }
      return getStyle().getFont();
    }

    private Edges getMargin() {
      return margin;
    }

    private int getMaxHeight() {
      return maxHeight;
    }

    private int getMinHeight() {
      return minHeight;
    }

    private Edges getPadding() {
      return padding;
    }

    private SafeStyles getSafeStyles() {
      if (getStyle() == null) {
        return null;
      }
      return getStyle().getSafeStyles();
    }

    private StyleDescriptor getStyle() {
      return style;
    }

    private void setBorderWidth(Edges borderWidth) {
      this.borderWidth = borderWidth;
    }

    private void setCellHeight(int cellHeight) {
      this.cellHeight = cellHeight;
    }

    private void setFont(String fontDeclaration) {
      if (getStyle() == null) {
        if (!BeeUtils.isEmpty(fontDeclaration)) {
          setStyle(new StyleDescriptor(null, null, fontDeclaration));
        }
      } else {
        getStyle().setFontDeclaration(fontDeclaration);
      }
    }

    private void setMargin(Edges margin) {
      this.margin = margin;
    }

    private void setMaxHeight(int maxHeight) {
      this.maxHeight = maxHeight;
    }

    private void setMinHeight(int minHeight) {
      this.minHeight = minHeight;
    }

    private void setPadding(Edges padding) {
      this.padding = padding;
    }

    private void setStyle(StyleDescriptor style) {
      this.style = style;
    }
  }

  private enum ComponentType {
    HEADER, BODY, FOOTER;
  }

  private class RenderInfo {
    private final String rowIdx;
    private final int colIdx;
    private final String classes;
    private final SafeStyles styles;
    private final boolean focusable;
    private final SafeHtml content;

    private RenderInfo(String rowIdx, int colIdx, String classes, SafeStyles styles,
        boolean focusable, SafeHtml content) {
      this.rowIdx = rowIdx;
      this.colIdx = colIdx;
      this.classes = classes;
      this.styles = styles;
      this.focusable = focusable;
      this.content = content;
    }

    private String getClasses() {
      return classes;
    }

    private int getColIdx() {
      return colIdx;
    }

    private SafeHtml getContent() {
      return content;
    }

    private String getRowIdx() {
      return rowIdx;
    }

    private SafeStyles getStyles() {
      return styles;
    }

    private boolean isFocusable() {
      return focusable;
    }

    private SafeHtml render() {
      SafeHtml result;
      if (isFocusable()) {
        result = template.cellFocusable(rowIdx, colIdx, classes, styles, getTabIndex(), content);
      } else {
        result = template.cell(rowIdx, colIdx, classes, styles, content);
      }
      return result;
    }
  }

  private class ResizerMoveTimer extends Timer {
    private boolean pending = false;
    private int pendingMove = 0;

    @Override
    public void cancel() {
      if (pending) {
        pending = false;
        super.cancel();
      }
    }

    @Override
    public void run() {
      pending = false;
      doResize();
    }

    @Override
    public void schedule(int delayMillis) {
      pending = true;
      super.schedule(delayMillis);
    }

    private void doResize() {
      if (pendingMove == 0) {
        return;
      }
      switch (getResizerStatus()) {
        case HORIZONTAL:
          resizeHorizontal(pendingMove);
          break;
        case VERTICAL:
          resizeVertical(pendingMove);
          break;
      }
      pendingMove = 0;
    }

    private void handleMove(int by, int millis) {
      if (by == 0) {
        return;
      }
      pendingMove += by;
      if (!pending && pendingMove != 0) {
        schedule(millis);
      }
    }

    private void reset() {
      cancel();
      pendingMove = 0;
    }

    private void stop() {
      cancel();
      doResize();
    }
  }

  private class ResizerShowTimer extends Timer {
    private boolean pending = false;

    private Element element;
    private String rowIdx = null;
    private int colIdx = BeeConst.UNDEF;

    private ResizerMode resizerMode = null;
    private Rectangle rectangle = null;

    private ResizerShowTimer() {
      super();
    }

    @Override
    public void cancel() {
      if (pending) {
        pending = false;
        super.cancel();
      }
    }

    public void run() {
      pending = false;

      switch (resizerMode) {
        case HORIZONTAL:
          showColumnResizer(element, colIdx);
          break;
        case VERTICAL:
          showRowResizer(element, rowIdx);
          break;
      }
    }

    @Override
    public void schedule(int delayMillis) {
      pending = true;
      super.schedule(delayMillis);
    }

    private void handleEvent(Event event) {
      if (!pending) {
        return;
      }
      if (!EventUtils.isMouseMove(event.getType())) {
        cancel();
        return;
      }

      if (!rectangle.contains(event.getClientX(), event.getClientY())) {
        cancel();
      }
    }

    private void start(Element el, String row, int col, ResizerMode resizer, Rectangle rect,
        int millis) {
      this.element = el;
      this.rowIdx = row;
      this.colIdx = col;
      this.resizerMode = resizer;
      this.rectangle = rect;

      schedule(millis);
    }
  }

  /**
   * Lists possible grid elements for parameter management.
   */

  private enum TargetType {
    CONTAINER, RESIZER, HEADER, BODY, FOOTER;
  }

  public static int defaultBodyCellHeight = 24;
  public static Edges defaultBodyCellPadding = new Edges(2, 3);
  public static Edges defaultBodyBorderWidth = new Edges(1);
  public static Edges defaultBodyCellMargin = null;

  public static int defaultFooterCellHeight = 25;
  public static Edges defaultFooterCellPadding = new Edges(1, 2, 0);
  public static Edges defaultFooterBorderWidth = new Edges(1);
  public static Edges defaultFooterCellMargin = null;

  public static int defaultHeaderCellHeight = 25;
  public static Edges defaultHeaderCellPadding = null;
  public static Edges defaultHeaderBorderWidth = new Edges(1);
  public static Edges defaultHeaderCellMargin = null;

  public static int defaultMinCellWidth = 16;
  public static int defaultMaxCellWidth = 999;
  public static int defaultMinCellHeight = 8;
  public static int defaultMaxCellHeight = 256;

  public static int defaultResizerShowSensitivityMillis = 100;
  public static int defaultResizerMoveSensitivityMillis = 0;
  
  public static int pageSizeCalculationReserve = 3;

  public static String STYLE_GRID = "bee-CellGrid";
  public static String STYLE_EMPTY = "bee-CellGridEmpty";

  public static String STYLE_CELL = "bee-CellGridCell";
  public static String STYLE_HEADER = "bee-CellGridHeader";
  public static String STYLE_BODY = "bee-CellGridBody";
  public static String STYLE_FOOTER = "bee-CellGridFooter";

  public static String STYLE_EVEN_ROW = "bee-CellGridEvenRow";
  public static String STYLE_ODD_ROW = "bee-CellGridOddRow";
  public static String STYLE_SELECTED_ROW = "bee-CellGridSelectedRow";
  public static String STYLE_ACTIVE_ROW = "bee-CellGridActiveRow";
  public static String STYLE_ACTIVE_CELL = "bee-CellGridActiveCell";
  public static String STYLE_RESIZED_CELL = "bee-CellGridResizedCell";

  public static String STYLE_RESIZER = "bee-CellGridResizer";
  public static String STYLE_RESIZER_HANDLE = "bee-CellGridResizerHandle";
  public static String STYLE_RESIZER_BAR = "bee-CellGridResizerBar";

  public static String STYLE_RESIZER_HORIZONTAL = "bee-CellGridResizerHorizontal";
  public static String STYLE_RESIZER_HANDLE_HORIZONTAL = "bee-CellGridResizerHandleHorizontal";
  public static String STYLE_RESIZER_BAR_HORIZONTAL = "bee-CellGridResizerBarHorizontal";

  public static String STYLE_RESIZER_VERTICAL = "bee-CellGridResizerVertical";
  public static String STYLE_RESIZER_HANDLE_VERTICAL = "bee-CellGridResizerHandleVertical";
  public static String STYLE_RESIZER_BAR_VERTICAL = "bee-CellGridResizerBarVertical";

  private static final String HEADER_ROW = "header";
  private static final String FOOTER_ROW = "footer";

  private static final Template template = GWT.create(Template.class);

  private final List<ColumnInfo> columns = Lists.newArrayList();

  private final Component headerComponent =
      new Component(defaultHeaderCellHeight, defaultMinCellHeight, defaultMaxCellHeight,
          defaultHeaderCellPadding, defaultHeaderBorderWidth, defaultHeaderCellMargin);

  private final Component bodyComponent =
      new Component(defaultBodyCellHeight, defaultMinCellHeight, defaultMaxCellHeight,
          defaultBodyCellPadding, defaultBodyBorderWidth, defaultBodyCellMargin);

  private final Component footerComponent =
      new Component(defaultFooterCellHeight, defaultMinCellHeight, defaultMaxCellHeight,
          defaultFooterCellPadding, defaultFooterBorderWidth, defaultFooterCellMargin);

  private int minCellWidth = defaultMinCellWidth;
  private int maxCellWidth = defaultMaxCellWidth;

  private int activeRow = BeeConst.UNDEF;
  private int activeColumn = BeeConst.UNDEF;

  private int pageSize = BeeConst.UNDEF;
  private int pageStart = 0;

  private int rowCount = BeeConst.UNDEF;
  private boolean rowCountIsExact = true;

  private final List<IsRow> rowData = Lists.newArrayList();

  private ConditionalStyle rowStyles = null;

  private SelectionModel<? super IsRow> selectionModel;
  private final LinkedHashMap<Long, RowInfo> selectedRows = Maps.newLinkedHashMap();

  private final Order sortOrder = new Order();

  private int tabIndex = 0;
  private int zIndex = 0;

  private boolean hasCellPreview = false;

  private final String resizerId = DomUtils.createUniqueId("resizer");
  private final String resizerHandleId = DomUtils.createUniqueId("resizer-handle");
  private final String resizerBarId = DomUtils.createUniqueId("resizer-bar");

  private ResizerMode resizerStatus = null;
  private boolean isResizing = false;
  private String resizerRow = null;
  private int resizerCol = BeeConst.UNDEF;
  private Modifiers resizerModifiers = null;

  private int resizerPosition = BeeConst.UNDEF;
  private int resizerPositionMin = BeeConst.UNDEF;
  private int resizerPositionMax = BeeConst.UNDEF;

  private int resizerShowSensitivityMillis = defaultResizerShowSensitivityMillis;
  private int resizerMoveSensitivityMillis = defaultResizerMoveSensitivityMillis;

  private final ResizerShowTimer resizerShowTimer = new ResizerShowTimer();
  private final ResizerMoveTimer resizerMoveTimer = new ResizerMoveTimer();

  private final Map<Long, Integer> resizedRows = Maps.newHashMap();
  private final Table<Long, String, CellInfo> resizedCells = HashBasedTable.create();

  private boolean readOnly = false;
  private boolean editing = false;

  public CellGrid() {
    setElement(Document.get().createDivElement());

    Set<String> eventTypes = Sets.newHashSet(EventUtils.EVENT_TYPE_KEY_DOWN,
        EventUtils.EVENT_TYPE_KEY_PRESS, EventUtils.EVENT_TYPE_CLICK,
        EventUtils.EVENT_TYPE_MOUSE_DOWN, EventUtils.EVENT_TYPE_MOUSE_MOVE,
        EventUtils.EVENT_TYPE_MOUSE_UP, EventUtils.EVENT_TYPE_MOUSE_OUT);
    sinkEvents(eventTypes);

    setStyleName(STYLE_GRID);
    createId();
  }

  public HandlerRegistration addActiveRowChangeHandler(ActiveRowChangeEvent.Handler handler) {
    return addHandler(handler, ActiveRowChangeEvent.getType());
  }
  
  public HandlerRegistration addCellPreviewHandler(CellPreviewEvent.Handler<IsRow> handler) {
    setHasCellPreview(true);
    return addHandler(handler, CellPreviewEvent.getType());
  }

  public void addColumn(String columnId, int dataIndex, AbstractColumn<?> col,
      ColumnHeader header) {
    insertColumn(getColumnCount(), columnId, dataIndex, col, header, null);
  }

  public void addColumn(String columnId, int dataIndex,
      AbstractColumn<?> col, ColumnHeader header, ColumnFooter footer) {
    insertColumn(getColumnCount(), columnId, dataIndex, col, header, footer);
  }

  public HandlerRegistration addEditStartHandler(EditStartEvent.Handler handler) {
    return addHandler(handler, EditStartEvent.getType());
  }

  public HandlerRegistration addLoadingStateChangeHandler(LoadingStateChangeEvent.Handler handler) {
    return addHandler(handler, LoadingStateChangeEvent.TYPE);
  }

  public HandlerRegistration addRangeChangeHandler(RangeChangeEvent.Handler handler) {
    return addHandler(handler, RangeChangeEvent.getType());
  }

  public HandlerRegistration addRowCountChangeHandler(RowCountChangeEvent.Handler handler) {
    return addHandler(handler, RowCountChangeEvent.getType());
  }

  public HandlerRegistration addSelectionCountChangeHandler(
      SelectionCountChangeEvent.Handler handler) {
    return addHandler(handler, SelectionCountChangeEvent.getType());
  }

  public HandlerRegistration addSortHandler(SortEvent.Handler handler) {
    return addHandler(handler, SortEvent.getType());
  }

  public void autoFit() {
    autoFit(true);
  }

  public void autoFit(boolean redraw) {
    for (int i = 0; i < getColumnCount(); i++) {
      int width = estimateColumnWidth(i);
      if (width <= 0) {
        continue;
      }
      setColumnWidth(i, limitCellWidth(width, i));
    }

    boolean pageSizeChanged = updatePageSize();
    if (redraw && !pageSizeChanged) {
      redraw();
    }
  }

  public void autoFitColumn(int col) {
    int oldWidth = getColumnWidth(col);
    int newWidth = estimateColumnWidth(col);
    if (newWidth <= 0) {
      return;
    }
    resizeColumnWidth(col, oldWidth, newWidth - oldWidth);
  }

  public void autoFitColumn(String columnId) {
    int col = getColumnIndex(columnId);
    if (isColumnWithinBounds(col)) {
      autoFitColumn(col);
    }
  }

  public void clearColumnWidth(String columnId) {
    setColumnWidth(columnId, BeeConst.UNDEF);
  }

  public boolean contains(String columnId) {
    if (BeeUtils.isEmpty(columnId)) {
      return false;
    }

    for (ColumnInfo info : columns) {
      if (info.is(columnId)) {
        return true;
      }
    }
    return false;
  }

  public void createId() {
    DomUtils.createId(this, "cell-grid");
  }

  public <T extends IsRow> int estimateColumnWidth(int col) {
    return estimateColumnWidth(col, getVisibleItems(), true);
  }

  public <T extends IsRow> int estimateColumnWidth(int col, boolean update) {
    return estimateColumnWidth(col, getVisibleItems(), update);
  }

  public <T extends IsRow> int estimateColumnWidth(int col, List<T> values, boolean update) {
    Assert.notNull(values);
    return estimateColumnWidth(col, values, values.size(), update);
  }

  public <T extends IsRow> int estimateColumnWidth(int col, List<T> values, int length,
      boolean update) {
    Assert.notNull(values);

    ColumnInfo columnInfo = getColumnInfo(col);
    AbstractColumn<?> column = columnInfo.getColumn();
    Font font = Font.merge(getBodyComponent().getFont(), columnInfo.getBodyFont());

    int width = 0;
    for (int i = 0; i < length; i++) {
      IsRow rowValue = values.get(i);
      if (rowValue == null) {
        continue;
      }
      width = Math.max(width, estimateBodyCellWidth(i, col, rowValue, column, font));
    }

    if (width > 0 && update) {
      columnInfo.ensureBodyWidth(width);
    }
    return width;
  }

  public <T extends IsRow> void estimateColumnWidths(List<T> values) {
    Assert.notNull(values);
    estimateColumnWidths(values, values.size());
  }

  public <T extends IsRow> void estimateColumnWidths(List<T> values, int length) {
    for (int i = 0; i < getColumnCount(); i++) {
      estimateColumnWidth(i, values, length, true);
    }
  }

  public void estimateFooterWidths() {
    for (int i = 0; i < getColumnCount(); i++) {
      ColumnInfo columnInfo = getColumnInfo(i);
      ColumnFooter footer = columnInfo.getFooter();
      if (footer == null) {
        continue;
      }

      SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
      CellContext context = new CellContext(0, i, footer.getKey(), this);
      footer.render(context, cellBuilder);
      SafeHtml cellHtml = cellBuilder.toSafeHtml();

      Font font = Font.merge(getFooterComponent().getFont(), columnInfo.getFooterFont());
      int cellWidth = Rulers.getLineWidth(cellHtml.asString(), font);
      if (cellWidth > 0) {
        columnInfo.ensureFooterWidth(cellWidth);
      }
    }
  }

  public int estimateHeaderWidth(int col, boolean addMargins) {
    ColumnInfo columnInfo = getColumnInfo(col);
    ColumnHeader header = columnInfo.getHeader();
    if (header == null) {
      return 0;
    }

    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    CellContext context = new CellContext(0, col, header.getKey(), this);
    header.render(context, cellBuilder);
    SafeHtml cellHtml = cellBuilder.toSafeHtml();

    Font font = Font.merge(getHeaderComponent().getFont(), columnInfo.getHeaderFont());
    int width = Rulers.getLineWidth(cellHtml.asString(), font);

    if (width > 0) {
      if (addMargins) {
        if (columnInfo.getColumn().isSortable()) {
          width += ColumnHeader.defaultSortInfoHorizontalSize;
        }
        if (header.hasWidthInfo()) {
          width += ColumnHeader.defaultWidthInfoHorizontalSize;
        }
      }
      columnInfo.ensureHeaderWidth(width);
    }
    return width;
  }

  public void estimateHeaderWidths(boolean addMargins) {
    for (int i = 0; i < getColumnCount(); i++) {
      estimateHeaderWidth(i, addMargins);
    }
  }

  public int estimatePageSize() {
    return estimatePageSize(getElement().getClientWidth(), getElement().getClientHeight(), false);
  }

  public int estimatePageSize(int containerWidth, int containerHeight, boolean checkWidth) {
    int availableBodyHeight = containerHeight - getHeaderHeight() - getFooterHeight();

    if (checkWidth) {
      int width = getBodyWidth();
      if (width <= 0 || width > containerWidth) {
        availableBodyHeight -= DomUtils.getScrollbarHeight();
      }
    }

    int bodyRowHeight = getBodyCellHeight() + getBodyCellHeightIncrement();
    if (bodyRowHeight > 0 && availableBodyHeight >= bodyRowHeight) {
      int ps = availableBodyHeight / bodyRowHeight;
      if (ps > 1 && availableBodyHeight % bodyRowHeight < pageSizeCalculationReserve) {
        ps--;
      }
      return ps;
    }
    return BeeConst.UNDEF;
  }

  public void fireLoadingStateChange(LoadingStateChangeEvent.LoadingState loadingState) {
    if (loadingState != null) {
      fireEvent(new LoadingStateChangeEvent(loadingState));
    }
  }

  public int getActiveColumn() {
    return activeColumn;
  }

  public int getActiveRow() {
    return activeRow;
  }

  public RowInfo getActiveRowInfo() {
    int visibleIndex = getActiveRow();
    if (isRowWithinBounds(visibleIndex) && visibleIndex < getVisibleItems().size()) {
      return new RowInfo(getVisibleItem(visibleIndex));
    } else {
      return null;
    }
  }

  public Edges getBodyBorderWidth() {
    return getBodyComponent().getBorderWidth();
  }

  public int getBodyCellHeight() {
    return getBodyComponent().getCellHeight();
  }

  public int getBodyCellHeightIncrement() {
    return getHeightIncrement(getBodyCellPadding(), getBodyBorderWidth(), getBodyCellMargin());
  }

  public Edges getBodyCellMargin() {
    return getBodyComponent().getMargin();
  }

  public Edges getBodyCellPadding() {
    return getBodyComponent().getPadding();
  }

  public int getBodyCellWidthIncrement() {
    return getWidthIncrement(getBodyCellPadding(), getBodyBorderWidth(), getBodyCellMargin());
  }

  public int getBodyWidth() {
    int width = 0;
    int incr = getBodyCellWidthIncrement();

    for (ColumnInfo columnInfo : columns) {
      int w = columnInfo.getColumnWidth();
      if (w <= 0) {
        width = BeeConst.UNDEF;
        break;
      }
      width += w + incr;
    }
    return width;
  }

  public AbstractColumn<?> getColumn(int col) {
    return getColumnInfo(col).getColumn();
  }

  public AbstractColumn<?> getColumn(String columnId) {
    ColumnInfo info = getColumnInfo(columnId);
    if (info == null) {
      return null;
    }
    return info.getColumn();
  }

  public int getColumnCount() {
    return columns.size();
  }

  public String getColumnId(int col) {
    return getColumnInfo(col).getColumnId();
  }

  public int getColumnIndex(String columnId) {
    Assert.notEmpty(columnId);
    for (int i = 0; i < getColumnCount(); i++) {
      if (columns.get(i).is(columnId)) {
        return i;
      }
    }
    BeeKeeper.getLog().warning("columnId", columnId, "not found");
    return BeeConst.UNDEF;
  }

  public int getColumnWidth(int col) {
    return getColumnInfo(col).getColumnWidth();
  }

  public int getColumnWidth(String columnId) {
    ColumnInfo info = getColumnInfo(columnId);
    if (info == null) {
      return BeeConst.UNDEF;
    }
    return info.getColumnWidth();
  }

  public int getFootCellWidthIncrement() {
    return getWidthIncrement(getFooterCellPadding(), getFooterBorderWidth(), getFooterCellMargin());
  }

  public Edges getFooterBorderWidth() {
    return getFooterComponent().getBorderWidth();
  }

  public int getFooterCellHeight() {
    return getFooterComponent().getCellHeight();
  }

  public int getFooterCellHeightIncrement() {
    return getHeightIncrement(getFooterCellPadding(), getFooterBorderWidth(), getFooterCellMargin());
  }

  public Edges getFooterCellMargin() {
    return getFooterComponent().getMargin();
  }

  public Edges getFooterCellPadding() {
    return getFooterComponent().getPadding();
  }

  public int getFooterHeight() {
    if (hasFooters()) {
      return getFooterCellHeight() + getFooterCellHeightIncrement();
    } else {
      return 0;
    }
  }

  public List<ColumnFooter> getFooters() {
    List<ColumnFooter> lst = Lists.newArrayList();
    for (ColumnInfo info : columns) {
      if (info.getFooter() != null) {
        lst.add(info.getFooter());
      }
    }
    return lst;
  }

  public Edges getHeaderBorderWidth() {
    return getHeaderComponent().getBorderWidth();
  }

  public int getHeaderCellHeight() {
    return getHeaderComponent().getCellHeight();
  }

  public int getHeaderCellHeightIncrement() {
    return getHeightIncrement(getHeaderCellPadding(), getHeaderBorderWidth(), getHeaderCellMargin());
  }

  public Edges getHeaderCellMargin() {
    return getHeaderComponent().getMargin();
  }

  public Edges getHeaderCellPadding() {
    return getHeaderComponent().getPadding();
  }

  public int getHeaderCellWidthIncrement() {
    return getWidthIncrement(getHeaderCellPadding(), getHeaderBorderWidth(), getHeaderCellMargin());
  }

  public int getHeaderHeight() {
    if (hasHeaders()) {
      return getHeaderCellHeight() + getHeaderCellHeightIncrement();
    } else {
      return 0;
    }
  }

  public String getId() {
    return DomUtils.getId(this);
  }

  public int getMaxBodyCellHeight() {
    return getBodyComponent().getMaxHeight();
  }

  public int getMinBodyCellHeight() {
    return getBodyComponent().getMinHeight();
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getPageStart() {
    return pageStart;
  }

  public IsRow getRowById(long rowId) {
    for (IsRow row : getVisibleItems()) {
      if (row.getId() == rowId) {
        return row;
      }
    }
    return null;
  }

  public int getRowCount() {
    return rowCount;
  }
  
  public LinkedHashMap<Long, RowInfo> getSelectedRows() {
    return selectedRows;
  }

  public SelectionModel<? super IsRow> getSelectionModel() {
    return selectionModel;
  }

  public int getSortIndex(String columnId) {
    return getSortOrder().getIndex(columnId);
  }

  public Order getSortOrder() {
    return sortOrder;
  }

  public int getTabIndex() {
    return tabIndex;
  }

  public IsRow getVisibleItem(int indexOnPage) {
    checkRowBounds(indexOnPage);
    return rowData.get(indexOnPage);
  }

  public int getVisibleItemCount() {
    return Math.min(getPageSize(), getRowCount());
  }

  public List<IsRow> getVisibleItems() {
    return rowData;
  }

  public Range getVisibleRange() {
    return new Range(getPageStart(), getPageSize());
  }

  public long getVisibleRowId(int indexOnPage) {
    return getVisibleItem(indexOnPage).getId();
  }

  public int getZIndex() {
    return zIndex;
  }

  public boolean handleKeyboardNavigation(int keyCode, boolean hasModifiers) {
    if (getActiveRow() < 0 || getActiveColumn() < 0) {
      return false;
    }

    switch (keyCode) {
      case KeyCodes.KEY_DOWN:
        keyboardNext();
        return true;

      case KeyCodes.KEY_UP:
        keyboardPrev();
        return true;

      case KeyCodes.KEY_PAGEDOWN:
        if (hasModifiers) {
          keyboardEnd();
        } else {
          keyboardNextPage();
        }
        return true;

      case KeyCodes.KEY_PAGEUP:
        if (hasModifiers) {
          keyboardHome();
        } else {
          keyboardPrevPage();
        }
        return true;

      case KeyCodes.KEY_HOME:
        if (hasModifiers) {
          keyboardHome();
        }
        return true;

      case KeyCodes.KEY_END:
        if (hasModifiers) {
          keyboardEnd();
        }
        return true;

      case KeyCodes.KEY_LEFT:
        if (getActiveColumn() > 0) {
          setActiveColumn(getActiveColumn() - 1);
        }
        return true;

      case KeyCodes.KEY_BACKSPACE:
        keyboardLeft();
        return true;

      case KeyCodes.KEY_RIGHT:
        if (getActiveColumn() < getColumnCount() - 1) {
          setActiveColumn(getActiveColumn() + 1);
        }
        return true;

      case KeyCodes.KEY_TAB:
        if (hasModifiers) {
          keyboardLeft();
        } else {
          keyboardRight();
        }
        return true;

      default:
        return false;
    }
  }

  public boolean hasFooters() {
    for (ColumnInfo info : columns) {
      if (info.getFooter() != null) {
        return true;
      }
    }
    return false;
  }

  public boolean hasHeaders() {
    for (ColumnInfo info : columns) {
      if (info.getHeader() != null) {
        return true;
      }
    }
    return false;
  }

  public boolean hasKeyboardNext() {
    return getActiveRow() + getPageStart() < getRowCount() - 1;
  }

  public boolean hasKeyboardPrev() {
    return getActiveRow() > 0 || getPageStart() > 0;
  }

  public void insertColumn(int beforeIndex, String columnId, int dataIndex,
      AbstractColumn<?> column, ColumnHeader header, ColumnFooter footer) {
    if (beforeIndex != getColumnCount()) {
      checkColumnBounds(beforeIndex);
    }
    checkColumnId(columnId);

    columns.add(beforeIndex, new ColumnInfo(columnId, dataIndex, column, header, footer));

    Set<String> consumedEvents = Sets.newHashSet();
    {
      Set<String> cellEvents = column.getCell().getConsumedEvents();
      if (cellEvents != null) {
        consumedEvents.addAll(cellEvents);
      }
    }
    if (header != null) {
      Set<String> headerEvents = header.getCell().getConsumedEvents();
      if (headerEvents != null) {
        consumedEvents.addAll(headerEvents);
      }
    }
    if (footer != null) {
      Set<String> footerEvents = footer.getCell().getConsumedEvents();
      if (footerEvents != null) {
        consumedEvents.addAll(footerEvents);
      }
    }
    if (!consumedEvents.isEmpty()) {
      sinkEvents(consumedEvents);
    }
  }

  public boolean isEditing() {
    return editing;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isRowCountExact() {
    return rowCountIsExact;
  }

  public boolean isRowSelected(long rowId) {
    return getSelectedRows().containsKey(rowId);
  }

  public boolean isSortable(String columnId) {
    ColumnInfo info = getColumnInfo(columnId);
    if (info == null) {
      return false;
    }
    return info.getColumn().isSortable();
  }

  public boolean isSortAscending(String columnId) {
    return getSortOrder().isAscending(columnId);
  }

  @Override
  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);
    if (isEditing()) {
      return;
    }

    String eventType = event.getType();
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return;
    }
    Element target = Element.as(eventTarget);

    TargetType targetType = null;
    String rowIdx = null;
    int row = BeeConst.UNDEF;
    int col = BeeConst.UNDEF;

    if (target == getElement()) {
      targetType = TargetType.CONTAINER;
    } else if (isResizerOrResizerChild(target)) {
      targetType = TargetType.RESIZER;
    } else if (getElement().isOrHasChild(target)) {
      while (target != null && target != getElement()) {
        rowIdx = DomUtils.getDataRow(target);
        if (!BeeUtils.isEmpty(rowIdx)) {
          break;
        }
        target = target.getParentElement();
      }
      if (!BeeUtils.isEmpty(rowIdx)) {
        col = BeeUtils.toInt(DomUtils.getDataColumn(target));
        checkColumnBounds(col);

        if (isHeaderRow(rowIdx)) {
          targetType = TargetType.HEADER;
        } else if (isFooterRow(rowIdx)) {
          targetType = TargetType.FOOTER;
        } else if (isBodyRow(rowIdx)) {
          targetType = TargetType.BODY;
          row = BeeUtils.toInt(rowIdx);
          checkRowBounds(row);
        }
      }
    }

    if (targetType == null) {
      return;
    }

    getResizerShowTimer().handleEvent(event);

    if (EventUtils.isMouseMove(eventType)) {
      if (handleMouseMove(event, target, targetType, rowIdx, col)) {
        EventUtils.eatEvent(event);
        return;
      }
    } else if (EventUtils.isMouseDown(eventType)) {
      if (targetType == TargetType.RESIZER) {
        startResizing(event);
        EventUtils.eatEvent(event);
        return;
      }
    } else if (EventUtils.isMouseUp(eventType)) {
      if (isResizing()) {
        stopResizing();
        EventUtils.eatEvent(event);
        updatePageSize();
        return;
      }
      if (isCellActive(row, col)) {
        checkCellSize(target, row, col);
      }
    } else if (EventUtils.isMouseOut(eventType)) {
      if (targetType == TargetType.RESIZER && !isResizing()) {
        hideResizer();
        return;
      }
      if (event.getRelatedEventTarget() != null
          && !getElement().isOrHasChild(Node.as(event.getRelatedEventTarget()))) {
        if (isResizing()) {
          stopResizing();
        } else if (isResizerVisible()) {
          hideResizer();
        }
        return;
      }
    }

    if (targetType == TargetType.HEADER) {
      ColumnHeader header = getColumnInfo(col).getHeader();
      if (header != null && cellConsumesEventType(header.getCell(), eventType)) {
        CellContext context = new CellContext(0, col, header.getKey(), this);
        header.onBrowserEvent(context, target, event);
      }

    } else if (targetType == TargetType.FOOTER) {
      ColumnFooter footer = getColumnInfo(col).getFooter();
      if (footer != null && cellConsumesEventType(footer.getCell(), eventType)) {
        CellContext context = new CellContext(0, col, footer.getKey(), this);
        footer.onBrowserEvent(context, target, event);
      }

    } else if (targetType == TargetType.BODY) {
      IsRow rowValue = getVisibleItem(row);

      if (EventUtils.isClick(eventType)) {
        if (EventUtils.hasModifierKey(event)) {
          if (event.getShiftKey()) {
            selectRange(row, rowValue);
          } else {
            selectRow(row, rowValue);
          }
          activateCell(row, col);
        } else if (isCellActive(row, col)) {
          startEditing(rowValue, col, target, EditorFactory.START_MOUSE_CLICK);
        } else {
          activateCell(row, col);
        }
        return;

      } else if (EventUtils.isKeyDown(eventType)) {
        if (!isCellActive(row, col)) {
          EventUtils.eatEvent(event);
          refocus();
          return;
        }

        int keyCode = event.getKeyCode();
        if (keyCode == KeyCodes.KEY_ENTER) {
          startEditing(rowValue, col, target, EditorFactory.START_KEY_ENTER);
          EventUtils.eatEvent(event);
        } else if (handleKey(keyCode, EventUtils.hasModifierKey(event), row, col, target)) {
          EventUtils.eatEvent(event);
        }
        return;

      } else if (EventUtils.isKeyPress(eventType)) {
        int charCode = event.getCharCode();
        EventUtils.eatEvent(event);

        if (charCode == BeeConst.CHAR_SPACE) {
          selectRow(row, rowValue);
        } else {
          startEditing(rowValue, col, target, event.getCharCode());
        }
        return;
      }

      AbstractColumn<?> column = getColumn(col);
      CellContext context = new CellContext(row, col, getRowId(rowValue), this);

      if (hasCellPreview()) {
        CellPreviewEvent<IsRow> previewEvent = CellPreviewEvent.fire(this, event, this, context,
            rowValue, false, column.getCell().handlesSelection());
        if (previewEvent.isCanceled()) {
          return;
        }
      }
      fireEventToCell(event, eventType, target, rowValue, context, column);
    }
  }

  public void onCellUpdate(CellUpdateEvent event) {
    Assert.notNull(event);
    long rowId = event.getRowId();
    long version = event.getVersion();
    String columnId = event.getColumnId();
    String value = event.getValue();

    RowInfo selectedRowInfo = getSelectedRows().get(rowId);
    if (selectedRowInfo != null) {
      selectedRowInfo.setVersion(version);
    }

    int row = getRowIndex(rowId);
    if (!isRowWithinBounds(row)) {
      BeeKeeper.getLog().warning("onCellUpdate: row id", rowId, "is not visible");
      return;
    }

    int col = getColumnIndex(columnId);
    if (!isColumnWithinBounds(col)) {
      return;
    }

    IsRow rowValue = getVisibleItem(row);
    rowValue.setVersion(version);
    int dataIndex = getColumnInfo(col).getDataIndex();
    rowValue.setValue(dataIndex, value);
    
    if (getRowStyles() != null) {
      refreshRow(row);
      return;
    }

    if (hasCalculatedColumns()) {
      refreshCalculatedColumns(row);
    }

    if (getColumnInfo(col).getDynStyles() != null) {
      refreshCell(row, col);
    } else {
      updateCellContent(row, col);
    }
  }

  public void onMultiDelete(MultiDeleteEvent event) {
    Assert.notNull(event);
    for (RowInfo rowInfo : event.getRows()) {
      deleteRow(rowInfo.getId());
    }
    setRowCount(getRowCount() - event.getRows().size());
  }

  public void onRowDelete(RowDeleteEvent event) {
    Assert.notNull(event);
    deleteRow(event.getRowId());
    setRowCount(getRowCount() - 1);
  }

  public void preliminaryUpdate(long rowId, String columnId, String value) {
    int row = getRowIndex(rowId);
    if (!isRowWithinBounds(row)) {
      return;
    }

    int col = getColumnIndex(columnId);
    if (!isColumnWithinBounds(col)) {
      return;
    }

    Element cellElement = getCellElement(row, col);
    if (cellElement == null) {
      return;
    }

    if (BeeUtils.isEmpty(value)) {
      cellElement.setInnerHTML(BeeConst.STRING_EMPTY);
      return;
    }

    IsRow rowValue = getVisibleItem(row).clone();
    int dataIndex = getColumnInfo(col).getDataIndex();
    rowValue.setValue(dataIndex, value);

    AbstractColumn<?> column = getColumn(col);

    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    CellContext context = new CellContext(row, col, rowId, this);
    column.render(context, rowValue, cellBuilder);
    SafeHtml cellHtml = cellBuilder.toSafeHtml();

    cellElement.setInnerHTML(cellHtml.asString());
  }

  public void redraw() {
    fireLoadingStateChange(LoadingStateChangeEvent.LoadingState.PARTIALLY_LOADED);

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    if (rowData == null || rowData.isEmpty()) {
      sb.append(template.emptiness(STYLE_EMPTY, BeeConst.STRING_EMPTY));
    } else {
      renderData(sb, rowData);
      renderResizer(sb);
    }
    replaceAllChildren(sb.toSafeHtml());

    fireLoadingStateChange(LoadingStateChangeEvent.LoadingState.LOADED);

    setZIndex(0);

    if (getActiveRow() >= 0 && getActiveColumn() >= 0) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        public void execute() {
          Element cellElement = getActiveCellElement();
          if (cellElement != null) {
            cellElement.getStyle().setZIndex(incrementZIndex());
            cellElement.focus();
          }
        }
      });
    }
  }

  public void refocus() {
    if (getActiveRow() >= 0 && getActiveColumn() >= 0) {
      Element cellElement = getActiveCellElement();
      if (cellElement != null) {
        cellElement.focus();
      }
    }
  }

  public void refreshCellContent(long rowId, String columnId) {
    int row = getRowIndex(rowId);
    if (!isRowWithinBounds(row)) {
      BeeKeeper.getLog().warning("refreshCell: row id", rowId, "is not visible");
      return;
    }

    int col = getColumnIndex(columnId);
    if (!isColumnWithinBounds(col)) {
      return;
    }

    updateCellContent(row, col);
  }

  public void removeColumn(int index) {
    Assert.isIndex(columns, index);
    columns.remove(index);
  }

  public int resizeColumn(int col, int newWidth) {
    int oldWidth = getColumnWidth(col);
    return resizeColumnWidth(col, oldWidth, newWidth - oldWidth);
  }

  public void setActiveColumn(int activeColumn) {
    if (this.activeColumn == activeColumn) {
      return;
    }
    onActivateCell(false);

    this.activeColumn = activeColumn;

    onActivateCell(true);
  }

  public void setActiveRow(int activeRow) {
    if (this.activeRow == activeRow) {
      return;
    }
    onActivateCell(false);
    onActivateRow(false);

    this.activeRow = activeRow;

    onActivateRow(true);
    onActivateCell(true);
  }

  public void setBodyBorderWidth(Edges borderWidth) {
    getBodyComponent().setBorderWidth(borderWidth);
  }

  public void setBodyCellHeight(int cellHeight) {
    getBodyComponent().setCellHeight(cellHeight);
  }

  public void setBodyCellMargin(Edges margin) {
    getBodyComponent().setMargin(margin);
  }

  public void setBodyCellPadding(Edges padding) {
    getBodyComponent().setPadding(padding);
  }

  public void setBodyComponent(GridComponentDescription src) {
    updateComponent(src, getBodyComponent());
  }

  public void setBodyFont(String fontDeclaration) {
    getBodyComponent().setFont(fontDeclaration);
  }

  public void setColumnBodyFont(String columnId, String fontDeclaration) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setBodyFont(fontDeclaration);
  }

  public void setColumnBodyWidth(String columnId, int width) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setBodyWidth(width);
  }

  public void setColumnFooterFont(String columnId, String fontDeclaration) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setFooterFont(fontDeclaration);
  }

  public void setColumnFooterWidth(String columnId, int width) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setFooterWidth(width);
  }

  public void setColumnHeaderFont(String columnId, String fontDeclaration) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setHeaderFont(fontDeclaration);
  }

  public void setColumnHeaderWidth(String columnId, int width) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setHeaderWidth(width);
  }

  public void setColumnInfo(String columnId, ColumnDescription columnDescription,
      List<? extends IsColumn> dataColumns) {
    ColumnInfo columnInfo = getColumnInfo(columnId);
    Assert.notNull(columnInfo);
    Assert.notNull(columnDescription);

    if (columnDescription.getColType().isReadOnly()
        || BeeUtils.isTrue(columnDescription.isReadOnly())) {
      columnInfo.setColReadOnly(true);
    }

    if (columnDescription.getWidth() != null) {
      columnInfo.setWidth(columnDescription.getWidth());
    }
    if (columnDescription.getMinWidth() != null) {
      columnInfo.setMinWidth(columnDescription.getMinWidth());
    }
    if (columnDescription.getMaxWidth() != null) {
      columnInfo.setMaxWidth(columnDescription.getMaxWidth());
    }

    if (columnDescription.getHeaderStyle() != null) {
      columnInfo.setHeaderStyle(StyleDescriptor.copyOf(columnDescription.getHeaderStyle()));
    }
    if (columnDescription.getBodyStyle() != null) {
      columnInfo.setBodyStyle(StyleDescriptor.copyOf(columnDescription.getBodyStyle()));
    }
    if (columnDescription.getFooterStyle() != null) {
      columnInfo.setFooterStyle(StyleDescriptor.copyOf(columnDescription.getFooterStyle()));
    }

    if (columnDescription.getDynStyles() != null) {
      ConditionalStyle dynStyles = ConditionalStyle.create(columnDescription.getDynStyles(),
          columnId, dataColumns);
      if (dynStyles != null) {
        columnInfo.setDynStyles(dynStyles);
      }
    }
  }

  public void setColumnWidth(int col, int width) {
    getColumnInfo(col).setWidth(width);
  }

  public void setColumnWidth(String columnId, double width, Unit unit) {
    int containerSize = getOffsetWidth();
    Assert.isPositive(containerSize);
    setColumnWidth(columnId, width, unit, containerSize);
  }

  public void setColumnWidth(String columnId, double width, Unit unit, int containerSize) {
    setColumnWidth(columnId, Rulers.getIntPixels(width, unit, containerSize));
  }

  public void setColumnWidth(String columnId, int width) {
    ColumnInfo info = getColumnInfo(columnId);
    Assert.notNull(info);

    info.setWidth(width);
  }

  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  public void setFooterBorderWidth(Edges borderWidth) {
    getFooterComponent().setBorderWidth(borderWidth);
  }

  public void setFooterCellHeight(int cellHeight) {
    getFooterComponent().setCellHeight(cellHeight);
  }

  public void setFooterCellMargin(Edges margin) {
    getFooterComponent().setMargin(margin);
  }

  public void setFooterCellPadding(Edges padding) {
    getFooterComponent().setPadding(padding);
  }

  public void setFooterComponent(GridComponentDescription src) {
    updateComponent(src, getFooterComponent());
  }

  public void setFooterFont(String fontDeclaration) {
    getFooterComponent().setFont(fontDeclaration);
  }

  public void setHeaderBorderWidth(Edges borderWidth) {
    getHeaderComponent().setBorderWidth(borderWidth);
  }

  public void setHeaderCellHeight(int cellHeight) {
    getHeaderComponent().setCellHeight(cellHeight);
  }

  public void setHeaderCellMargin(Edges margin) {
    getHeaderComponent().setMargin(margin);
  }

  public void setHeaderCellPadding(Edges padding) {
    getHeaderComponent().setPadding(padding);
  }

  public void setHeaderComponent(GridComponentDescription src) {
    updateComponent(src, getHeaderComponent());
  }

  public void setHeaderFont(String fontDeclaration) {
    getHeaderComponent().setFont(fontDeclaration);
  }

  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  public void setMaxBodyCellHeight(int maxHeight) {
    getBodyComponent().setMaxHeight(maxHeight);
  }

  public void setMaxCellWidth(int maxCellWidth) {
    this.maxCellWidth = maxCellWidth;
  }

  public void setMinBodyCellHeight(int minHeight) {
    getBodyComponent().setMinHeight(minHeight);
  }

  public void setMinCellWidth(int minCellWidth) {
    this.minCellWidth = minCellWidth;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public void setPageStart(int pageStart) {
    this.pageStart = pageStart;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setRowCount(int count) {
    setRowCount(count, true);
  }

  public void setRowCount(int count, boolean isExact) {
    Assert.nonNegative(count);
    if (count == getRowCount() && isExact == isRowCountExact()) {
      return;
    }
    rowCount = count;
    rowCountIsExact = isExact;

    if (getPageStart() > 0 && getPageSize() > 0 && getPageStart() + getPageSize() > count) {
      setPageStart(Math.max(count - getPageSize(), 0));
    }

    RowCountChangeEvent.fire(this, count, isExact);
  }

  public void setRowData(int start, List<? extends IsRow> values) {
    Assert.nonNegative(start);
    Assert.notNull(values);

    int size = values.size();
    if (rowData.size() == size) {
      for (int i = 0; i < size; i++) {
        rowData.set(i, values.get(i));
      }
    } else {
      rowData.clear();
      for (int i = 0; i < size; i++) {
        rowData.add(values.get(i));
      }
    }
    redraw();
  }

  public void setRowData(List<? extends IsRow> values) {
    setRowCount(values.size());
    setVisibleRange(0, values.size());
    setRowData(0, values);
  }

  public void setRowStyles(ConditionalStyle rowStyles) {
    this.rowStyles = rowStyles;
  }

  public void setSelectionModel(SelectionModel<? super IsRow> selectionModel) {
    this.selectionModel = selectionModel;
  }

  public void setTabIndex(int index) {
    this.tabIndex = index;
  }

  public void setVisibleRange(int start, int length) {
    setVisibleRange(new Range(start, length));
  }

  public void setVisibleRange(Range range) {
    setVisibleRange(range, false, false);
  }

  public void setVisibleRangeAndClearData(Range range, boolean forceRangeChangeEvent) {
    setVisibleRange(range, true, forceRangeChangeEvent);
  }

  public void updateActiveRow(List<? extends IsRow> values) {
    Assert.notNull(values);
    int oldRow = getActiveRow();

    if (oldRow >= 0 && oldRow < rowData.size()) {
      int newRow = 0;
      long id = rowData.get(oldRow).getId();
      for (int i = 0; i < values.size(); i++) {
        if (values.get(i).getId() == id) {
          newRow = i;
          break;
        }
      }
      this.activeRow = newRow;
    }
  }

  public void updateOrder(int col, NativeEvent event) {
    checkColumnBounds(col);
    if (getColumn(col).isSortable() && getRowCount() > 1) {
      updateOrder(getColumnId(col), EventUtils.hasModifierKey(event));
      SortEvent.fire(this, getSortOrder());
    }
  }

  @Override
  protected void onUnload() {
    getResizerShowTimer().cancel();
    getResizerMoveTimer().cancel();
    super.onUnload();
  }

  protected void setResizerMoveSensitivityMillis(int resizerMoveSensitivityMillis) {
    this.resizerMoveSensitivityMillis = resizerMoveSensitivityMillis;
  }

  protected void setResizerShowSensitivityMillis(int resizerShowSensitivityMillis) {
    this.resizerShowSensitivityMillis = resizerShowSensitivityMillis;
  }

  private void activateCell(int row, int col) {
    if (getActiveRow() == row) {
      setActiveColumn(col);
      return;
    }
    onActivateCell(false);
    onActivateRow(false);

    this.activeRow = row;
    this.activeColumn = col;

    onActivateRow(true);
    onActivateCell(true);
  }

  private void activateRow(int index) {
    activateRow(index, BeeConst.UNDEF);
  }

  private void activateRow(int index, int start) {
    int rc = getRowCount();
    if (rc <= 0) {
      return;
    }
    if (rc <= 1) {
      setActiveRow(0);
      return;
    }

    int absIndex = BeeUtils.limit(index, 0, rc - 1);
    int oldPageStart = getPageStart();
    if (oldPageStart + getActiveRow() == absIndex) {
      return;
    }

    int size = getPageSize();
    if (size <= 0 || size >= rc) {
      setActiveRow(absIndex);
      return;
    }
    if (size == 1) {
      setActiveRow(0);
      setVisibleRange(absIndex, getPageSize());
      return;
    }

    int newPageStart;
    if (start >= 0 && absIndex >= start && absIndex < start + size) {
      newPageStart = start;
    } else if (absIndex >= oldPageStart && absIndex < oldPageStart + size) {
      newPageStart = oldPageStart;
    } else if (absIndex == oldPageStart - 1) {
      newPageStart = absIndex;
    } else if (absIndex == oldPageStart + size && getActiveRow() == size - 1) {
      newPageStart = oldPageStart + 1;
    } else {
      newPageStart = (absIndex / size) * size;
    }
    newPageStart = BeeUtils.limit(newPageStart, 0, rc - size);

    setActiveRow(absIndex - newPageStart);

    if (newPageStart != oldPageStart) {
      setVisibleRange(newPageStart, getPageSize());
    }
  }

  private boolean cellConsumesEventType(Cell<?> cell, String eventType) {
    Set<String> consumedEvents = cell.getConsumedEvents();
    return consumedEvents != null && consumedEvents.contains(eventType);
  }

  private void checkCellSize(Element element, int row, int col) {
    int width = StyleUtils.getWidth(element);
    int height = StyleUtils.getHeight(element);
    if (width <= 0 || height <= 0) {
      return;
    }

    long rowId = getVisibleRowId(row);
    String columnId = getColumnId(col);

    CellInfo cellInfo;
    Element nextElement;
    Edges padding;
    Edges borders;

    if (width == getColumnWidth(col) && height == getRowHeightById(rowId)) {
      cellInfo = getResizedCells().remove(rowId, columnId);
      if (cellInfo != null) {
        element.removeClassName(STYLE_RESIZED_CELL);

        if (row < getVisibleItemCount() - 1) {
          nextElement = getCellElement(row + 1, col);
          if (nextElement != null) {
            padding = cellInfo.getNextRowPadding();
            if (padding != null) {
              Edges cellPadding = new Edges(nextElement, StyleUtils.STYLE_PADDING);
              cellPadding.setTop(padding);
              cellPadding.setBottom(padding);
              cellPadding.applyTo(nextElement, StyleUtils.STYLE_PADDING);
            }
            borders = cellInfo.getNextRowBorders();
            if (borders != null) {
              Edges cellBorders = new Edges(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
              cellBorders.setTop(borders);
              cellBorders.applyTo(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
            }
          }
        }

        if (col < getColumnCount() - 1) {
          nextElement = getCellElement(row, col + 1);
          if (nextElement != null) {
            padding = cellInfo.getNextColumnPadding();
            if (padding != null) {
              Edges cellPadding = new Edges(nextElement, StyleUtils.STYLE_PADDING);
              cellPadding.setLeft(padding);
              cellPadding.setRight(padding);
              cellPadding.applyTo(nextElement, StyleUtils.STYLE_PADDING);
            }
            borders = cellInfo.getNextColumnBorders();
            if (borders != null) {
              Edges cellBorders = new Edges(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
              cellBorders.setLeft(borders);
              cellBorders.applyTo(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
            }
          }
        }
      }

    } else {
      cellInfo = getResizedCells().get(rowId, columnId);
      if (cellInfo == null) {
        element.addClassName(STYLE_RESIZED_CELL);
        cellInfo = new CellInfo(width, height);

        if (row < getVisibleItemCount() - 1 && Edges.hasPositiveTop(getBodyBorderWidth())) {
          nextElement = getCellElement(row + 1, col);
          if (nextElement != null) {
            padding = new Edges(nextElement, StyleUtils.STYLE_PADDING);
            borders = new Edges(nextElement, StyleUtils.STYLE_BORDER_WIDTH);

            if (borders.getIntTop() <= 0) {
              cellInfo.setNextRowPadding(Edges.copyOf(padding));
              cellInfo.setNextRowBorders(Edges.copyOf(borders));

              int px = getBodyBorderWidth().getIntTop();
              borders.setTop(borders.getIntTop() + px);
              padding = incrementEdges(padding, 0, -px);

              padding.applyTo(nextElement, StyleUtils.STYLE_PADDING);
              borders.applyTo(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
            }
          }
        }

        if (col < getColumnCount() - 1 && Edges.hasPositiveLeft(getBodyBorderWidth())) {
          nextElement = getCellElement(row, col + 1);
          if (nextElement != null) {
            padding = new Edges(nextElement, StyleUtils.STYLE_PADDING);
            borders = new Edges(nextElement, StyleUtils.STYLE_BORDER_WIDTH);

            if (borders.getIntLeft() <= 0) {
              cellInfo.setNextColumnPadding(Edges.copyOf(padding));
              cellInfo.setNextColumnBorders(Edges.copyOf(borders));

              int px = getBodyBorderWidth().getIntLeft();
              borders.setLeft(borders.getIntLeft() + px);
              padding = incrementEdges(padding, -px, 0);

              padding.applyTo(nextElement, StyleUtils.STYLE_PADDING);
              borders.applyTo(nextElement, StyleUtils.STYLE_BORDER_WIDTH);
            }
          }
        }

        getResizedCells().put(rowId, columnId, cellInfo);
      } else {
        cellInfo.setSize(width, height);
      }
    }
  }

  private void checkColumnBounds(int col) {
    Assert.isTrue(isColumnWithinBounds(col));
  }

  private void checkColumnId(String columnId) {
    Assert.notNull(columnId);
    Assert.isFalse(contains(columnId), "Duplicate Column Id " + columnId);
  }

  private boolean checkResizerBounds(int position) {
    return position >= getResizerPositionMin() && position <= getResizerPositionMax();
  }

  private void checkRowBounds(int row) {
    Assert.isTrue(isRowWithinBounds(row), "row index " + row + " out of bounds: page size "
        + getPageSize() + ", row count " + getRowCount());
  }

  private void deleteRow(long rowId) {
    if (isRowSelected(rowId)) {
      getSelectedRows().remove(rowId);
      fireSelectionCountChange();
    }

    if (getResizedRows().containsKey(rowId)) {
      getResizedRows().remove(rowId);
    }
    if (getResizedCells().containsRow(rowId)) {
      getResizedCells().rowKeySet().remove(rowId);
    }
  }

  private int estimateBodyCellWidth(int visibleIndex, int col, IsRow rowValue,
      AbstractColumn<?> column, Font font) {
    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    CellContext context = new CellContext(visibleIndex, col, getRowId(rowValue), this);
    column.render(context, rowValue, cellBuilder);
    SafeHtml cellHtml = cellBuilder.toSafeHtml();

    return Rulers.getLineWidth(cellHtml.asString(), font);
  }

  private <C> void fireEventToCell(Event event, String eventType,
      Element parentElem, IsRow value, CellContext context, AbstractColumn<C> column) {
    Cell<C> cell = column.getCell();
    if (cellConsumesEventType(cell, eventType)) {
      column.onBrowserEvent(context, parentElem, value, event);
    }
  }

  private void fireSelectionCountChange() {
    fireEvent(new SelectionCountChangeEvent(getSelectedRows().size()));
  }

  private Element getActiveCellElement() {
    return getBodyCellElement(getActiveRow(), getActiveColumn());
  }

  private NodeList<Element> getActiveRowElements() {
    return getRowElements(getActiveRow());
  }

  private Element getBodyCellElement(int row, int col) {
    return Selectors.getElement(getElement(), getBodyCellSelector(row, col));
  }

  private String getBodyCellSelector(int row, int col) {
    return Selectors.conjunction(getBodyRowSelector(row), getColumnSelector(col));
  }

  private Component getBodyComponent() {
    return bodyComponent;
  }

  private int getBodyHeight() {
    int height = 0;
    int increment = getBodyCellHeightIncrement();
    for (int i = 0; i < getVisibleItemCount(); i++) {
      height += getRowHeight(i) + increment;
    }
    return height;
  }

  private String getBodyRowSelector(int visibleIndex) {
    return Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, visibleIndex);
  }

  private Edges[][] getBorders(List<IsRow> values, Edges defaultBorders, Edges margin) {
    if (values == null || defaultBorders == null || defaultBorders.isEmpty()) {
      return null;
    }
    int rc = values.size();
    int cc = getColumnCount();
    if (rc <= 0 || cc <= 0) {
      return null;
    }

    int left = BeeUtils.toNonNegativeInt(defaultBorders.getLeftValue());
    int right = BeeUtils.toNonNegativeInt(defaultBorders.getRightValue());
    int top = BeeUtils.toNonNegativeInt(defaultBorders.getTopValue());
    int bottom = BeeUtils.toNonNegativeInt(defaultBorders.getBottomValue());

    boolean collapseHorizontally =
        cc > 1 && left > 0 && right > 0 && !Edges.hasPositiveHorizontalValue(margin);
    boolean collapseVertically =
        top > 0 && bottom > 0 && !Edges.hasPositiveVerticalValue(margin);
    if (!collapseHorizontally && !collapseVertically) {
      return null;
    }

    Edges firstColumn = Edges.copyOf(defaultBorders);
    Edges lastColumn = Edges.copyOf(defaultBorders);
    Edges firstRow = Edges.copyOf(defaultBorders);
    Edges lastRow = Edges.copyOf(defaultBorders);
    Edges defaultCell = Edges.copyOf(defaultBorders);

    if (collapseHorizontally) {
      firstColumn.setRight(Math.max(left, right));
      defaultCell.setLeft(0);
      defaultCell.setRight(Math.max(left, right));
      lastColumn.setLeft(0);
    }

    if (collapseVertically) {
      if (hasHeaders() && Edges.hasPositiveBottom(getHeaderBorderWidth())
          && !Edges.hasPositiveBottom(getHeaderCellMargin())) {
        firstRow.setTop(0);
      }
      if (rc > 1) {
        firstRow.setBottom(Math.max(top, bottom));
      }
      defaultCell.setTop(0);
      defaultCell.setBottom(Math.max(top, bottom));
      lastRow.setTop(0);
      if (hasFooters() && Edges.hasPositiveTop(getFooterBorderWidth())
          && !Edges.hasPositiveTop(getFooterCellMargin())) {
        lastRow.setBottom(0);
        if (rc <= 1) {
          firstRow.setBottom(0);
        }
      }
    }

    if (collapseHorizontally && collapseVertically) {
      firstColumn.setTop(defaultCell);
      firstColumn.setBottom(defaultCell);
      lastColumn.setTop(defaultCell);
      lastColumn.setBottom(defaultCell);

      firstRow.setLeft(defaultCell);
      firstRow.setRight(defaultCell);
      lastRow.setLeft(defaultCell);
      lastRow.setRight(defaultCell);
    }

    Edges[][] borders = new Edges[rc][cc];
    Edges cellBorders = null;

    for (int i = 0; i < rc; i++) {
      for (int j = 0; j < cc; j++) {
        if (i == 0) {
          if (j == 0) {
            cellBorders = Edges.copyOf(firstRow);
            cellBorders.setLeft(firstColumn);
            cellBorders.setRight(firstColumn);
          } else if (j == cc - 1) {
            cellBorders = Edges.copyOf(firstRow);
            cellBorders.setLeft(lastColumn);
            cellBorders.setRight(lastColumn);
          } else {
            cellBorders = firstRow;
          }

        } else if (i == rc - 1) {
          if (j == 0) {
            cellBorders = Edges.copyOf(lastRow);
            cellBorders.setLeft(firstColumn);
            cellBorders.setRight(firstColumn);
          } else if (j == cc - 1) {
            cellBorders = Edges.copyOf(lastRow);
            cellBorders.setLeft(lastColumn);
            cellBorders.setRight(lastColumn);
          } else {
            cellBorders = lastRow;
          }

        } else {
          if (j == 0) {
            cellBorders = firstColumn;
          } else if (j == cc - 1) {
            cellBorders = lastColumn;
          } else {
            cellBorders = defaultCell;
          }
        }
        borders[i][j] = cellBorders;
      }
    }

    if (!getResizedCells().isEmpty()) {
      for (int i = 0; i < rc; i++) {
        long rowId = values.get(i).getId();
        if (!getResizedCells().containsRow(rowId)) {
          continue;
        }

        for (int j = 0; j < cc; j++) {
          if (!getResizedCells().contains(rowId, getColumnId(j))) {
            continue;
          }
          if (collapseVertically && i < rc - 1) {
            borders[i + 1][j] = Edges.copyOf(borders[i + 1][j]);
            borders[i + 1][j].setTop(defaultBorders);
          }
          if (collapseHorizontally && j < cc - 1) {
            borders[i][j + 1] = Edges.copyOf(borders[i][j + 1]);
            borders[i][j + 1].setLeft(defaultBorders);
          }
        }
      }
    }

    return borders;
  }

  private Element getCellElement(int visibleIndex, int col) {
    return getCellElement(BeeUtils.toString(visibleIndex), col);
  }

  private Element getCellElement(String rowIdx, int col) {
    return Selectors.getElement(getElement(), getCellSelector(rowIdx, col));
  }

  private String getCellSelector(String rowIdx, int col) {
    return Selectors.conjunction(getRowSelector(rowIdx), getColumnSelector(col));
  }

  private int getChildrenHeight() {
    return getHeaderHeight() + getBodyHeight() + getFooterHeight();
  }

  private NodeList<Element> getColumnElements(int col) {
    return Selectors.getNodes(getElement(),
        Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_COLUMN, col));
  }

  private ColumnInfo getColumnInfo(int col) {
    checkColumnBounds(col);
    return columns.get(col);
  }

  private ColumnInfo getColumnInfo(String columnId) {
    Assert.notEmpty(columnId);
    for (ColumnInfo info : columns) {
      if (info.is(columnId)) {
        return info;
      }
    }
    return null;
  }

  private String getColumnSelector(int col) {
    return Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_COLUMN, col);
  }

  private Component getComponent(String rowIdx) {
    if (BeeUtils.isEmpty(rowIdx)) {
      return null;
    }
    if (isHeaderRow(rowIdx)) {
      return getHeaderComponent();
    }
    if (isBodyRow(rowIdx)) {
      return getBodyComponent();
    }
    if (isFooterRow(rowIdx)) {
      return getFooterComponent();
    }
    return null;
  }

  private String getCssValue(Edges edges) {
    if (edges == null) {
      return Edges.EMPTY_CSS_VALUE;
    } else {
      return edges.getCssValue();
    }
  }

  private Element getFooterCellElement(int col) {
    return Selectors.getElement(getElement(), getFooterCellSelector(col));
  }

  private String getFooterCellSelector(int col) {
    return Selectors.conjunction(getFooterRowSelector(), getColumnSelector(col));
  }

  private Component getFooterComponent() {
    return footerComponent;
  }

  private NodeList<Element> getFooterElements() {
    if (hasFooters()) {
      return getRowElements(FOOTER_ROW);
    } else {
      return null;
    }
  }

  private String getFooterRowSelector() {
    return Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, FOOTER_ROW);
  }

  private Element getHeaderCellElement(int col) {
    return Selectors.getElement(getElement(), getHeaderCellSelector(col));
  }

  private String getHeaderCellSelector(int col) {
    return Selectors.conjunction(getHeaderRowSelector(), getColumnSelector(col));
  }

  private Component getHeaderComponent() {
    return headerComponent;
  }

  private NodeList<Element> getHeaderElements() {
    if (hasHeaders()) {
      return getRowElements(HEADER_ROW);
    } else {
      return null;
    }
  }

  private String getHeaderRowSelector() {
    return Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, HEADER_ROW);
  }

  private int getHeightIncrement(Edges edges) {
    int incr = 0;
    if (edges != null) {
      incr += BeeUtils.toNonNegativeInt(edges.getTopValue());
      incr += BeeUtils.toNonNegativeInt(edges.getBottomValue());
    }
    return incr;
  }

  private int getHeightIncrement(Edges padding, Edges border, Edges margin) {
    return getHeightIncrement(padding) + getHeightIncrement(border) + getHeightIncrement(margin);
  }

  private int getMaxCellWidth() {
    return maxCellWidth;
  }

  private int getMaxColumnWidth(int col) {
    int maxWidth = getColumnInfo(col).getMaxWidth();
    if (maxWidth > 0) {
      return maxWidth;
    }
    return getMaxCellWidth();
  }

  private int getMinCellWidth() {
    return minCellWidth;
  }

  private int getMinColumnWidth(int col) {
    int minWidth = getColumnInfo(col).getMinWidth();
    if (minWidth > 0) {
      return minWidth;
    }
    return getMinCellWidth();
  }

  private Table<Long, String, CellInfo> getResizedCells() {
    return resizedCells;
  }

  private Map<Long, Integer> getResizedRows() {
    return resizedRows;
  }

  private Element getResizerBar() {
    return Document.get().getElementById(resizerBarId);
  }

  private int getResizerCol() {
    return resizerCol;
  }

  private Element getResizerContainer() {
    return Document.get().getElementById(resizerId);
  }

  private Element getResizerHandle() {
    return Document.get().getElementById(resizerHandleId);
  }

  private Modifiers getResizerModifiers() {
    return resizerModifiers;
  }

  private int getResizerMoveSensitivityMillis() {
    return resizerMoveSensitivityMillis;
  }

  private ResizerMoveTimer getResizerMoveTimer() {
    return resizerMoveTimer;
  }

  private int getResizerPosition() {
    return resizerPosition;
  }

  private int getResizerPositionMax() {
    return resizerPositionMax;
  }

  private int getResizerPositionMin() {
    return resizerPositionMin;
  }

  private String getResizerRow() {
    return resizerRow;
  }

  private int getResizerShowSensitivityMillis() {
    return resizerShowSensitivityMillis;
  }

  private ResizerShowTimer getResizerShowTimer() {
    return resizerShowTimer;
  }

  private ResizerMode getResizerStatus() {
    return resizerStatus;
  }

  private NodeList<Element> getRowElements(int row) {
    return Selectors.getNodes(getElement(),
        Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, row));
  }

  private NodeList<Element> getRowElements(String rowIdx) {
    return Selectors.getNodes(getElement(),
        Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, rowIdx));
  }

  private int getRowHeight(int row) {
    return getRowHeightById(getVisibleRowId(row));
  }

  private int getRowHeight(String rowIdx) {
    if (isHeaderRow(rowIdx)) {
      return getHeaderCellHeight();
    }
    if (isFooterRow(rowIdx)) {
      return getFooterCellHeight();
    }
    return getRowHeight(BeeUtils.toInt(rowIdx));
  }

  private int getRowHeightById(long id) {
    Integer height = getResizedRows().get(id);
    if (height == null) {
      return getBodyCellHeight();
    } else {
      return height;
    }
  }

  private Long getRowId(IsRow value) {
    return (value == null) ? null : value.getId();
  }

  private int getRowIndex(long rowId) {
    int row = BeeConst.UNDEF;
    for (int i = 0; i < getVisibleItems().size(); i++) {
      if (rowData.get(i).getId() == rowId) {
        row = i;
        break;
      }
    }
    return row;
  }

  private String getRowSelector(String rowIdx) {
    return Selectors.attributeEquals(DomUtils.ATTRIBUTE_DATA_ROW, rowIdx);
  }

  private ConditionalStyle getRowStyles() {
    return rowStyles;
  }

  private int getRowWidth(String rowIdx) {
    int col = getColumnCount() - 1;
    if (col < 0) {
      return 0;
    }
    Element element = null;
    if (!BeeUtils.isEmpty(rowIdx)) {
      element = getCellElement(rowIdx, col);
    }

    if (element == null && hasHeaders()) {
      element = getHeaderCellElement(col);
    }
    if (element == null && hasFooters()) {
      element = getFooterCellElement(col);
    }
    if (element == null) {
      for (int i = 0; i < getVisibleItemCount(); i++) {
        element = getBodyCellElement(i, col);
        if (element != null) {
          break;
        }
      }
    }

    if (element == null) {
      return 0;
    } else {
      return element.getOffsetLeft() + element.getOffsetWidth();
    }
  }

  private int getWidthIncrement(Edges edges) {
    int incr = 0;
    if (edges != null) {
      incr += BeeUtils.toNonNegativeInt(edges.getLeftValue());
      incr += BeeUtils.toNonNegativeInt(edges.getRightValue());
    }
    return incr;
  }

  private int getWidthIncrement(Edges padding, Edges border, Edges margin) {
    return getWidthIncrement(padding) + getWidthIncrement(border) + getWidthIncrement(margin);
  }

  private boolean handleKey(int keyCode, boolean hasModifiers, int row, int col, Element cell) {
    if (resizeCell(keyCode, hasModifiers, row, col, cell)) {
      return true;
    }
    return handleKeyboardNavigation(keyCode, hasModifiers);
  }

  private boolean handleMouseMove(Event event, Element element, TargetType targetType,
      String eventRow, int eventCol) {
    int x = event.getClientX();
    int y = event.getClientY();

    if (!isResizerVisible()) {
      int millis = getResizerShowSensitivityMillis();

      if (isResizeAllowed(targetType, eventRow, eventCol, ResizerMode.HORIZONTAL)) {
        int size = ResizerMode.HORIZONTAL.getHandlePx();
        int right = element.getAbsoluteRight();

        if (BeeUtils.betweenInclusive(right - x, 0, size / 2)) {
          if (millis > 0) {
            getResizerShowTimer().start(element, eventRow, eventCol, ResizerMode.HORIZONTAL,
                new Rectangle(right - size / 2, y, size, size), millis);
          } else {
            showColumnResizer(element, eventCol);
          }
          setResizerPosition(x);
          return true;
        }
      }

      if (isResizeAllowed(targetType, eventRow, eventCol, ResizerMode.VERTICAL)) {
        int size = ResizerMode.VERTICAL.getHandlePx();
        int bottom = element.getAbsoluteBottom();

        if (BeeUtils.betweenInclusive(bottom - y, 0, size / 2)) {
          if (millis > 0) {
            getResizerShowTimer().start(element, eventRow, eventCol, ResizerMode.VERTICAL,
                new Rectangle(x, bottom - size / 2, size, size), millis);
          } else {
            showRowResizer(element, eventRow);
          }
          setResizerPosition(y);
          return true;
        }
      }

    } else if (isResizing()) {
      int position = getResizerPosition();
      int millis = getResizerMoveSensitivityMillis();

      switch (getResizerStatus()) {
        case HORIZONTAL:
          if (checkResizerBounds(x)) {
            if (millis > 0) {
              getResizerMoveTimer().handleMove(x - position, millis);
            } else {
              resizeHorizontal(x - position);
            }
            setResizerPosition(x);
            return true;
          }
          break;
        case VERTICAL:
          if (checkResizerBounds(y)) {
            if (millis > 0) {
              getResizerMoveTimer().handleMove(y - position, millis);
            } else {
              resizeVertical(y - position);
            }
            setResizerPosition(y);
            return true;
          }
          break;
        default:
          Assert.untouchable();
      }

    } else {
      if (!Rectangle.createFromAbsoluteCoordinates(getResizerContainer()).contains(x, y)) {
        hideResizer();
      }
    }

    return false;
  }

  private boolean hasCalculatedColumns() {
    for (ColumnInfo columnInfo : columns) {
      if (columnInfo.isCalculated()) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCellPreview() {
    return hasCellPreview;
  }

  private boolean hasPaging() {
    return getPageSize() > 0 && getPageSize() < getRowCount();
  }

  private void hideResizer() {
    StyleUtils.hideDisplay(resizerId);
    StyleUtils.hideDisplay(resizerHandleId);
    StyleUtils.hideDisplay(resizerBarId);

    setResizerStatus(null);
    setResizerRow(null);
    setResizerCol(BeeConst.UNDEF);
  }

  private Edges incrementEdges(Edges defaultEdges, int dw, int dh) {
    Edges edges = (defaultEdges == null) ? new Edges(0) : Edges.copyOf(defaultEdges);

    if (dw != 0) {
      int dr = dw / 2;
      int dl = dw - dr;

      if (dl != 0) {
        edges.setLeft(edges.getIntLeft() + dl);
      }
      if (dr != 0) {
        edges.setLeft(edges.getIntRight() + dr);
      }
    }

    if (dh != 0) {
      int db = dh / 2;
      int dt = dh - db;

      if (dt != 0) {
        edges.setTop(edges.getIntTop() + dt);
      }
      if (db != 0) {
        edges.setBottom(edges.getIntBottom() + db);
      }
    }

    return edges;
  }

  private void incrementHeight(NodeList<Element> nodes, int dh) {
    if (nodes != null && nodes.getLength() > 0 && dh != 0) {
      int height = StyleUtils.getHeight(nodes.getItem(0));
      if (height + dh >= 0) {
        StyleUtils.setStylePropertyPx(nodes, StyleUtils.STYLE_HEIGHT, height + dh);
      }
    }
  }

  private void incrementTop(NodeList<Element> nodes, int dt) {
    if (nodes != null && nodes.getLength() > 0 && dt != 0) {
      int top = StyleUtils.getTop(nodes.getItem(0));
      if (top + dt >= 0) {
        StyleUtils.setStylePropertyPx(nodes, StyleUtils.STYLE_TOP, top + dt);
      }
    }
  }

  private int incrementZIndex() {
    int z = getZIndex() + 1;
    setZIndex(z);
    return z;
  }

  private boolean isBodyRow(String rowIdx) {
    return BeeUtils.isDigit(rowIdx);
  }

  private boolean isCellActive(int row, int col) {
    return row >= 0 && col >= 0 && getActiveRow() == row && getActiveColumn() == col;
  }

  private boolean isCellResized(int row, int col) {
    return getResizedCells().contains(getVisibleRowId(row), getColumnId(col));
  }

  private boolean isColumnWithinBounds(int col) {
    return col >= 0 && col < getColumnCount();
  }

  private boolean isFooterRow(String rowIdx) {
    return BeeUtils.same(rowIdx, FOOTER_ROW);
  }

  private boolean isHeaderRow(String rowIdx) {
    return BeeUtils.same(rowIdx, HEADER_ROW);
  }

  private boolean isResizeAllowed(TargetType target, String rowIdx, int col, ResizerMode resizer) {
    if (resizer.getHandlePx() <= 0) {
      return false;
    }
    if (target == TargetType.HEADER || target == TargetType.FOOTER) {
      return true;
    }
    if (target != TargetType.BODY) {
      return false;
    }

    int row = BeeUtils.toInt(rowIdx);
    if (isCellActive(row, col)) {
      return false;
    }

    switch (resizer) {
      case HORIZONTAL:
        return row == 0 && !hasHeaders() || row == getVisibleItemCount() - 1 && !hasFooters();
      case VERTICAL:
        return col == 0 || col == getColumnCount() - 1;
      default:
        Assert.untouchable();
        return false;
    }
  }

  private boolean isResizerOrResizerChild(Element element) {
    if (element == null) {
      return false;
    }
    String id = element.getId();
    if (BeeUtils.isEmpty(id)) {
      return false;
    }
    return BeeUtils.inListSame(id, resizerId, resizerHandleId, resizerBarId);
  }

  private boolean isResizerVisible() {
    return getResizerStatus() != null;
  }

  private boolean isResizing() {
    return isResizing;
  }

  private boolean isRowSelected(IsRow rowValue) {
    if (rowValue == null) {
      return false;
    } else {
      return isRowSelected(rowValue.getId());
    }
  }

  private boolean isRowWithinBounds(int row) {
    return row >= 0 && row < getVisibleItemCount();
  }

  private void keyboardEnd() {
    activateRow(getRowCount() - 1);
  }

  private void keyboardHome() {
    activateRow(0);
  }

  private void keyboardLeft() {
    int prevColumn = getActiveColumn() - 1;
    if (prevColumn < 0) {
      if (hasKeyboardPrev()) {
        setActiveColumn(getColumnCount() - 1);
        keyboardPrev();
      }
    } else {
      setActiveColumn(prevColumn);
    }
  }

  private void keyboardNext() {
    activateRow(getPageStart() + getActiveRow() + 1);
  }

  private void keyboardNextPage() {
    if (getRowCount() <= 1 || getPageSize() <= 0 || getPageSize() >= getRowCount()
        || getPageStart() >= getRowCount() - getPageSize()) {
      activateRow(getRowCount() - 1);
      return;
    }

    int absIndex = getPageStart() + getPageSize();
    int start = Math.min(absIndex, getRowCount() - getPageSize());
    if (absIndex > start) {
      if (absIndex >= getRowCount() - 1) {
        absIndex = getRowCount() - 1;
      } else {
        absIndex = start;
      }
    }
    activateRow(absIndex, start);
  }

  private void keyboardPrev() {
    activateRow(getPageStart() + getActiveRow() - 1);
  }

  private void keyboardPrevPage() {
    if (getRowCount() <= 1 || getPageSize() <= 0 || getPageSize() >= getRowCount()
        || getPageStart() <= 0) {
      activateRow(0);
      return;
    }

    int absIndex = getPageStart() - 1;
    int start = Math.max(0, getPageStart() - getPageSize());
    absIndex = Math.max(absIndex, start + getPageSize() - 1);
    activateRow(absIndex, start);
  }

  private void keyboardRight() {
    int nextColumn = getActiveColumn() + 1;
    if (nextColumn >= getColumnCount()) {
      if (hasKeyboardNext()) {
        setActiveColumn(0);
        keyboardNext();
      }
    } else {
      setActiveColumn(nextColumn);
    }
  }

  private int limitCellHeight(int height, Component component) {
    if (component == null) {
      return height;
    }
    int h = height;
    if (component.getMinHeight() > 0) {
      h = Math.max(h, component.getMinHeight());
    }
    if (component.getMaxHeight() > 0 && component.getMaxHeight() > component.getMinHeight()) {
      h = Math.min(h, component.getMaxHeight());
    }
    return h;
  }

  private int limitCellWidth(int width, int col) {
    int w = width;
    int minWidth = getMinColumnWidth(col);
    int maxWidth = getMaxColumnWidth(col);

    if (minWidth > 0) {
      w = Math.max(w, minWidth);
    }
    if (maxWidth > 0 && maxWidth > minWidth) {
      w = Math.min(w, maxWidth);
    }
    return w;
  }

  private void onActivateCell(boolean activate) {
    if (getActiveRow() >= 0 && getActiveColumn() >= 0) {
      Element activeCell = getActiveCellElement();
      if (activeCell != null) {
        if (activate) {
          activeCell.getStyle().setZIndex(incrementZIndex());
          activeCell.addClassName(STYLE_ACTIVE_CELL);
          activeCell.focus();
        } else {
          activeCell.removeClassName(STYLE_ACTIVE_CELL);
        }
      }
    }
  }

  private void onActivateRow(boolean activate) {
    if (getActiveRow() >= 0) {
      NodeList<Element> rowElements = getActiveRowElements();
      if (rowElements != null && rowElements.getLength() > 0) {
        if (activate) {
          StyleUtils.addClassName(rowElements, STYLE_ACTIVE_ROW);
        } else {
          StyleUtils.removeClassName(rowElements, STYLE_ACTIVE_ROW);
        }
      }
      
      if (activate && getActiveRow() < rowData.size()) {
        fireEvent(new ActiveRowChangeEvent(rowData.get(getActiveRow())));
      }
    }
  }

  private void onSelectRow(int row, boolean select) {
    NodeList<Element> rowElements = getRowElements(row);
    if (rowElements != null && rowElements.getLength() > 0) {
      if (select) {
        StyleUtils.addClassName(rowElements, STYLE_SELECTED_ROW);
      } else {
        StyleUtils.removeClassName(rowElements, STYLE_SELECTED_ROW);
      }
    }
  }

  private void refreshCalculatedColumns(int rowIndex) {
    List<Integer> colIndexes = Lists.newArrayList();
    for (int col = 0; col < getColumnCount(); col++) {
      if (getColumnInfo(col).isCalculated()) {
        colIndexes.add(col);
      }
    }
    
    if (!colIndexes.isEmpty()) {
      refreshRow(rowIndex, colIndexes);
    }
  }

  private void refreshCell(int rowIndex, int colIndex) {
    refreshRow(rowIndex, Sets.newHashSet(colIndex));
  }

  private void refreshHeader(int col) {
    ColumnHeader header = getColumnInfo(col).getHeader();
    if (header == null) {
      return;
    }
    SafeHtmlBuilder builder = new SafeHtmlBuilder();
    CellContext context = new CellContext(0, col, header.getKey(), this);
    header.render(context, builder);

    getHeaderCellElement(col).setInnerHTML(builder.toSafeHtml().asString());
  }

  private void refreshRow(int rowIndex) {
    refreshRow(rowIndex, null);
  }

  private void refreshRow(int rowIndex, Collection<Integer> colIndexes) {
    checkRowBounds(rowIndex);
    
    List<RenderInfo> renderList = renderBody(getVisibleItems(), Sets.newHashSet(rowIndex),
        colIndexes);
    Assert.notEmpty(renderList);
    
    for (RenderInfo renderInfo : renderList) {
      Element cellElement = getCellElement(renderInfo.getRowIdx(), renderInfo.getColIdx());
      Assert.notNull(cellElement);
      
      if (!BeeUtils.same(cellElement.getClassName(), renderInfo.getClasses())) {
        cellElement.setClassName(renderInfo.getClasses());
      }
      if (renderInfo.getStyles() != null) {
        String styles = renderInfo.getStyles().asString();
        if (!BeeUtils.same(StyleUtils.getCssText(cellElement), styles)) {
          StyleUtils.setCssText(cellElement, styles);
        }
      }
      cellElement.setInnerHTML(renderInfo.getContent().asString());
    }
  }

  private List<RenderInfo> renderBody(List<IsRow> rowValues, Collection<Integer> rowIndexes,
      Collection<Integer> colIndexes) {
    int size = getVisibleItemCount();
    int start = getPageStart();
    int actRow = getActiveRow();
    int actCol = getActiveColumn();

    String classes =
        StyleUtils.buildClasses(STYLE_CELL, STYLE_BODY, getBodyComponent().getClassName());

    Edges padding = getBodyCellPadding();
    Edges borderWidth = getBodyBorderWidth();
    Edges margin = getBodyCellMargin();

    SafeStyles defaultPaddingStyle = StyleUtils.buildPadding(getCssValue(padding));
    SafeStyles defaultBorderWidthStyle = StyleUtils.buildBorderWidth(getCssValue(borderWidth));

    Edges[][] cellBorders = getBorders(rowValues, borderWidth, margin);
    boolean collapseBorders = (cellBorders != null);

    SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
    getBodyComponent().buildSafeStyles(stylesBuilder);

    stylesBuilder.append(StyleUtils.buildMargin(getCssValue(margin)));
    if (!collapseBorders) {
      stylesBuilder.append(defaultPaddingStyle);
      stylesBuilder.append(defaultBorderWidthStyle);
    }
    SafeStyles defaultStyles = stylesBuilder.toSafeStyles();

    int defaultWidthIncr = getWidthIncrement(padding, borderWidth, margin);
    int defaultBorderWidthIncr = getWidthIncrement(borderWidth);

    int defaultHeightIncr = getHeightIncrement(padding, borderWidth, margin);
    int defaultBorderHeightIncr = getHeightIncrement(borderWidth);

    int top = getHeaderHeight();
    
    List<RenderInfo> result = Lists.newArrayList();

    for (int i = 0; i < size; i++) {
      IsRow rowValue = rowValues.get(i);
      Assert.notNull(rowValue);

      long valueId = rowValue.getId();
      int rowHeight = getRowHeightById(valueId);
      
      if (rowIndexes != null && !rowIndexes.contains(i)) {
        top += rowHeight + defaultHeightIncr;
        continue;
      }
      
      boolean isSelected = isRowSelected(rowValue);
      boolean isActive = i == actRow;

      String rowClasses = StyleUtils.buildClasses(classes,
          ((i + start) % 2 == 1) ? STYLE_EVEN_ROW : STYLE_ODD_ROW);
      if (isActive) {
        rowClasses = StyleUtils.buildClasses(rowClasses, STYLE_ACTIVE_ROW);
      }
      if (isSelected) {
        rowClasses = StyleUtils.buildClasses(rowClasses, STYLE_SELECTED_ROW);
      }

      SafeStyles extraRowStyles = null;
      if (getRowStyles() != null) {
        StyleDescriptor dynRowStyle =
            getRowStyles().getStyleDescriptor(rowValue, i, BeeConst.UNDEF);
        if (dynRowStyle != null) {
          String dynRowClass = dynRowStyle.getClassName();
          if (!BeeUtils.isEmpty(dynRowClass)) {
            rowClasses = StyleUtils.buildClasses(rowClasses, dynRowClass);
          }

          if (dynRowStyle.hasSafeStylesOrFont()) {
            SafeStylesBuilder extraRowstylesBuilder = new SafeStylesBuilder();
            dynRowStyle.buildSafeStyles(extraRowstylesBuilder);
            extraRowStyles = extraRowstylesBuilder.toSafeStyles();
          }
        }
      }

      int col = 0;
      int left = 0;

      String rowIdx = BeeUtils.toString(i);

      int cellWidth;
      int cellHeight;

      for (ColumnInfo columnInfo : columns) {
        AbstractColumn<?> column = columnInfo.getColumn();
        int columnWidth = columnInfo.getColumnWidth();
        
        if (colIndexes == null || colIndexes.contains(col)) {
          String cellClasses = StyleUtils.buildClasses(rowClasses,
              columnInfo.getClassName(ComponentType.BODY));
          if (isActive && col == actCol) {
            cellClasses = StyleUtils.buildClasses(cellClasses, STYLE_ACTIVE_CELL);
          }

          SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
          CellContext context = new CellContext(i, col, getRowId(rowValue), this);
          column.render(context, rowValue, cellBuilder);
          SafeHtml cellHtml = cellBuilder.toSafeHtml();

          SafeStylesBuilder extraStylesBuilder = new SafeStylesBuilder();
          if (extraRowStyles != null) {
            extraStylesBuilder.append(extraRowStyles);
          }
          columnInfo.buildSafeStyles(extraStylesBuilder, ComponentType.BODY);

          if (columnInfo.getDynStyles() != null) {
            StyleDescriptor dynColStyle = columnInfo.getDynStyles().getStyleDescriptor(rowValue,
                i, col, column.getValueType(), column.getString(context, rowValue));
            if (dynColStyle != null) {
              if (!BeeUtils.isEmpty(dynColStyle.getClassName())) {
                cellClasses = StyleUtils.buildClasses(cellClasses, dynColStyle.getClassName());
              }
              dynColStyle.buildSafeStyles(extraStylesBuilder);
            }
          }

          if (collapseBorders) {
            Edges borders = cellBorders[i][col];
            if (borders == null) {
              extraStylesBuilder.append(defaultPaddingStyle);
              extraStylesBuilder.append(defaultBorderWidthStyle);
            } else {
              int widthIncr = defaultBorderWidthIncr - getWidthIncrement(borders);
              int heightIncr = defaultBorderHeightIncr - getHeightIncrement(borders);
              if (widthIncr != 0 || heightIncr != 0) {
                extraStylesBuilder.append(StyleUtils.buildPadding(getCssValue(incrementEdges(
                    padding, widthIncr, heightIncr))));
              } else {
                extraStylesBuilder.append(defaultPaddingStyle);
              }
              extraStylesBuilder.append(StyleUtils.buildBorderWidth(getCssValue(borders)));
            }
          }

          CellInfo cellInfo = getResizedCells().get(valueId, columnInfo.getColumnId());

          if (cellInfo == null) {
            cellWidth = columnWidth;
            cellHeight = rowHeight;

          } else {
            cellWidth = cellInfo.getWidth();
            cellHeight = cellInfo.getHeight();

            cellClasses = StyleUtils.buildClasses(cellClasses, STYLE_RESIZED_CELL);
            if (cellWidth > columnWidth || cellHeight > rowHeight) {
              extraStylesBuilder.append(StyleUtils.buildZIndex(incrementZIndex()));
            }
          }

          result.add(renderCell(rowIdx, col, cellClasses, left, top, cellWidth, cellHeight,
              defaultStyles, extraStylesBuilder.toSafeStyles(), column.getHorizontalAlignment(),
              cellHtml, true));
        }
        left += columnWidth + defaultWidthIncr;
        col++;
      }
      top += rowHeight + defaultHeightIncr;
    }
    return result;
  }

  private RenderInfo renderCell(String rowIdx, int col, String classes, int left, int top,
      int width, int height, SafeStyles styles, SafeStyles extraStyles,
      HorizontalAlignmentConstant hAlign, SafeHtml content, boolean focusable) {

    SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
    stylesBuilder.append(StyleUtils.PREFAB_POSITION_ABSOLUTE);
    
    if (styles != null) {
      stylesBuilder.append(styles);
    }
    if (extraStyles != null) {
      stylesBuilder.append(extraStyles);
    }
    if (hAlign != null) {
      stylesBuilder.append(StyleUtils.buildStyle(StyleUtils.CSS_TEXT_ALIGN,
          hAlign.getTextAlignString()));
    }

    stylesBuilder.append(StyleUtils.buildLeft(left));
    stylesBuilder.append(StyleUtils.buildTop(top));

    if (width > 0) {
      stylesBuilder.append(StyleUtils.buildWidth(width));
    }
    if (height > 0) {
      stylesBuilder.append(StyleUtils.buildHeight(height));
    }
    return new RenderInfo(rowIdx, col, classes, stylesBuilder.toSafeStyles(), focusable, content);
  }

  private void renderData(SafeHtmlBuilder sb, List<IsRow> values) {
    renderHeaders(sb, true);

    List<RenderInfo> body = renderBody(values, null, null);
    if (body != null) {
      for (RenderInfo cell : body) {
        sb.append(cell.render());
      }
    }

    renderHeaders(sb, false);
  }

  private void renderHeaders(SafeHtmlBuilder sb, boolean isHeader) {
    if (isHeader ? !hasHeaders() : !hasFooters()) {
      return;
    }
    int columnCount = getColumnCount();

    Component component = isHeader ? getHeaderComponent() : getFooterComponent();
    ComponentType componentType = isHeader ? ComponentType.HEADER : ComponentType.FOOTER;

    String classes = StyleUtils.buildClasses(STYLE_CELL,
        isHeader ? STYLE_HEADER : STYLE_FOOTER, component.getClassName());

    Edges padding = component.getPadding();
    Edges borderWidth = Edges.copyOf(component.getBorderWidth());
    Edges margin = component.getMargin();

    SafeStylesBuilder stylesBuilder = new SafeStylesBuilder();
    component.buildSafeStyles(stylesBuilder);

    stylesBuilder.append(StyleUtils.buildPadding(getCssValue(padding)));
    stylesBuilder.append(StyleUtils.buildMargin(getCssValue(margin)));

    SafeStyles firstColumnStyles = null;
    SafeStyles defaultColumnStyles = null;
    SafeStyles lastColumnStyles = null;

    int firstColumnWidthIncr = 0;
    int defaultColumnWidthIncr = 0;
    int lastColumnWidthIncr = 0;

    if (columnCount > 1 && borderWidth != null && !Edges.hasPositiveHorizontalValue(margin)) {
      int borderLeft = borderWidth.getIntLeft();
      int borderRight = borderWidth.getIntRight();

      if (borderLeft > 0 && borderRight > 0) {
        borderWidth.setRight(Math.max(borderLeft, borderRight));
        firstColumnStyles = StyleUtils.buildBorderWidth(getCssValue(borderWidth));
        firstColumnWidthIncr = getWidthIncrement(padding, borderWidth, margin);

        borderWidth.setLeft(0);
        lastColumnStyles = StyleUtils.buildBorderWidth(getCssValue(borderWidth));
        lastColumnWidthIncr = getWidthIncrement(padding, borderWidth, margin);

        borderWidth.setRight(borderRight);
        defaultColumnStyles = StyleUtils.buildBorderWidth(getCssValue(borderWidth));
        defaultColumnWidthIncr = getWidthIncrement(padding, borderWidth, margin);
      }
    }

    if (defaultColumnStyles == null) {
      stylesBuilder.append(StyleUtils.buildBorderWidth(getCssValue(borderWidth)));
      firstColumnWidthIncr =
          defaultColumnWidthIncr =
              lastColumnWidthIncr = getWidthIncrement(padding, borderWidth, margin);
    }
    SafeStyles styles = stylesBuilder.toSafeStyles();

    int top = isHeader ? 0 : getHeaderHeight() + getBodyHeight();
    int cellHeight = component.getCellHeight();
    int left = 0;

    int xIncr = getBodyCellWidthIncrement();
    int widthIncr;

    String rowIdx = isHeader ? HEADER_ROW : FOOTER_ROW;

    for (int i = 0; i < columnCount; i++) {
      ColumnInfo columnInfo = getColumnInfo(i);

      SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();

      if (isHeader) {
        if (columnInfo.getHeader() != null) {
          CellContext context = new CellContext(0, i, columnInfo.getHeader().getKey(), this);
          columnInfo.getHeader().render(context, cellBuilder);
        }
      } else if (columnInfo.getFooter() != null) {
        CellContext context = new CellContext(0, i, columnInfo.getFooter().getKey(), this);
        columnInfo.getFooter().render(context, cellBuilder);
      }

      int width = columnInfo.getColumnWidth();
      widthIncr = (i == 0) ? firstColumnWidthIncr
          : (i == columnCount - 1) ? lastColumnWidthIncr : defaultColumnWidthIncr;

      String cellClasses = StyleUtils.buildClasses(classes, columnInfo.getClassName(componentType));

      SafeStylesBuilder extraStylesBuilder = new SafeStylesBuilder();
      columnInfo.buildSafeStyles(extraStylesBuilder, componentType);

      SafeStyles extraStyles = (i == 0) ? firstColumnStyles
          : (i == columnCount - 1) ? lastColumnStyles : defaultColumnStyles;
      if (extraStyles != null) {
        extraStylesBuilder.append(extraStyles);
      }

      SafeHtml contents = renderCell(rowIdx, i, cellClasses, left, top,
          width + xIncr - widthIncr, cellHeight, styles, extraStylesBuilder.toSafeStyles(), null,
          cellBuilder.toSafeHtml(), false).render();
      sb.append(contents);

      left += width + xIncr;
    }
  }

  private void renderResizer(SafeHtmlBuilder sb) {
    sb.append(template.resizer(resizerId,
        template.resizerHandle(resizerHandleId), template.resizerBar(resizerBarId)));
  }

  private void replaceAllChildren(SafeHtml html) {
    getElement().setInnerHTML(html.asString());
  }

  private boolean resizeCell(int keyCode, boolean hasModifiers, int row, int col, Element cell) {
    if (cell == null) {
      return false;
    }

    if (keyCode == KeyCodes.KEY_ESCAPE || hasModifiers && BeeUtils.inList(keyCode,
        EventUtils.KEY_INSERT, KeyCodes.KEY_DELETE, KeyCodes.KEY_DOWN, KeyCodes.KEY_LEFT,
        KeyCodes.KEY_RIGHT, KeyCodes.KEY_UP)) {
      int oldWidth = StyleUtils.getWidth(cell);
      int oldHeight = StyleUtils.getHeight(cell);
      if (oldWidth <= 0 || oldHeight <= 0) {
        return false;
      }

      int newWidth = oldWidth;
      int newHeight = oldHeight;

      switch (keyCode) {
        case EventUtils.KEY_INSERT:
          newWidth++;
          newHeight++;
          break;
        case KeyCodes.KEY_DELETE:
          newWidth--;
          newHeight--;
          break;
        case KeyCodes.KEY_ESCAPE:
          newWidth = getColumnWidth(col);
          newHeight = getRowHeight(row);
          break;
        case KeyCodes.KEY_DOWN:
          newHeight++;
          break;
        case KeyCodes.KEY_LEFT:
          newWidth--;
          break;
        case KeyCodes.KEY_RIGHT:
          newWidth++;
          break;
        case KeyCodes.KEY_UP:
          newHeight--;
          break;
      }

      newWidth = limitCellWidth(newWidth, col);
      newHeight = limitCellHeight(newHeight, getBodyComponent());
      if (newWidth <= 0) {
        newWidth = oldWidth;
      }
      if (newHeight <= 0) {
        newHeight = oldHeight;
      }

      if (newWidth == oldWidth && newHeight == oldHeight) {
        return false;
      }
      StyleUtils.setWidth(cell, newWidth);
      StyleUtils.setHeight(cell, newHeight);

      checkCellSize(cell, row, col);
      return true;
    }
    return false;
  }

  private int resizeColumnWidth(int col, int oldWidth, int incr) {
    if (incr == 0 || oldWidth <= 0) {
      return BeeConst.UNDEF;
    }

    int newWidth = limitCellWidth(oldWidth + incr, col);
    if (newWidth <= 0 || !BeeUtils.sameSign(newWidth - oldWidth, incr)) {
      return BeeConst.UNDEF;
    }

    setColumnWidth(col, newWidth);

    NodeList<Element> nodes = getColumnElements(col);

    String cssWidth = null;
    for (int i = 0; i < nodes.getLength(); i++) {
      Element cellElement = nodes.getItem(i);
      String rowIdx = DomUtils.getDataRow(cellElement);

      if (isBodyRow(rowIdx)) {
        if (isCellResized(BeeUtils.toInt(rowIdx), col)) {
          continue;
        }
        if (cssWidth == null) {
          cssWidth = StyleUtils.toCssLength(StyleUtils.getWidth(cellElement)
              + newWidth - oldWidth, Unit.PX);
        }
        cellElement.getStyle().setProperty(StyleUtils.STYLE_WIDTH, cssWidth);
      } else {
        DomUtils.resizeHorizontalBy(cellElement, newWidth - oldWidth);
      }
    }

    refreshHeader(col);

    if (col < getColumnCount() - 1) {
      for (int i = col + 1; i < getColumnCount(); i++) {
        nodes = getColumnElements(i);
        if (nodes == null || nodes.getLength() <= 0) {
          continue;
        }
        int left = StyleUtils.getLeft(nodes.getItem(0));
        StyleUtils.setStylePropertyPx(nodes, StyleUtils.STYLE_LEFT, left + newWidth - oldWidth);
      }
    }
    return newWidth;
  }

  private void resizeHorizontal(int by) {
    if (by == 0) {
      return;
    }

    int col = getResizerCol();
    int oldWidth = getColumnWidth(col);
    int newWidth = resizeColumnWidth(col, oldWidth, by);
    if (BeeConst.isUndef(newWidth)) {
      return;
    }

    int incr = newWidth - oldWidth;
    if (incr != 0) {
      DomUtils.moveHorizontalBy(resizerId, incr);
    }
  }

  private void resizeRowElements(int row, NodeList<Element> nodes, int dh) {
    if (getResizedCells().containsRow(getVisibleRowId(row))) {
      for (int i = 0; i < nodes.getLength(); i++) {
        Element cellElement = nodes.getItem(i);
        int col = BeeUtils.toInt(DomUtils.getDataColumn(cellElement));
        if (isCellResized(row, col)) {
          continue;
        }
        DomUtils.resizeVerticalBy(cellElement, dh);
      }
    } else {
      incrementHeight(nodes, dh);
    }
  }

  private int resizeRowHeight(String rowIdx, int oldHeight, int incr, Modifiers modifiers) {
    if (oldHeight <= 0 || incr == 0) {
      return BeeConst.UNDEF;
    }
    int newHeight = limitCellHeight(oldHeight + incr, getComponent(rowIdx));
    if (newHeight <= 0 || !BeeUtils.sameSign(newHeight - oldHeight, incr)) {
      return BeeConst.UNDEF;
    }

    int dh = newHeight - oldHeight;
    int rc = getVisibleItemCount();
    NodeList<Element> nodes;

    if (isBodyRow(rowIdx) && Modifiers.isNotEmpty(modifiers)) {
      int dt = 0;
      int row = BeeUtils.toInt(rowIdx);

      int start = 0;
      int end = rc;
      boolean updateDefault = false;

      if (modifiers.isCtrlKey()) {
        updateDefault = true;
      } else if (modifiers.isShiftKey() && row < rc - 1 || row == 0) {
        start = row;
      } else if (modifiers.isAltKey() && row > 0 || row == rc - 1) {
        end = row + 1;
      } else {
        start = row;
        end = row + 1;
      }

      for (int i = start; i < rc; i++) {
        nodes = getRowElements(i);
        if (dt != 0) {
          incrementTop(nodes, dt);
        }
        if (i >= end) {
          continue;
        }

        int rh = getRowHeight(i);
        resizeRowElements(i, nodes, newHeight - rh);
        dt += newHeight - rh;
        if (!updateDefault) {
          setRowHeight(i, newHeight);
        }
      }

      if (updateDefault) {
        setBodyCellHeight(newHeight);
        getResizedRows().clear();
      }

      nodes = getFooterElements();
      if (nodes != null) {
        incrementTop(nodes, dt);
      }

    } else if (isHeaderRow(rowIdx)) {
      incrementHeight(getHeaderElements(), dh);
      for (int i = 0; i < rc; i++) {
        incrementTop(getRowElements(i), dh);
      }
      nodes = getFooterElements();
      if (nodes != null) {
        incrementTop(nodes, dh);
      }
      setHeaderCellHeight(newHeight);

    } else if (isBodyRow(rowIdx)) {
      int row = BeeUtils.toInt(rowIdx);
      resizeRowElements(row, getRowElements(row), dh);
      if (row < rc - 1) {
        for (int i = row + 1; i < rc; i++) {
          incrementTop(getRowElements(i), dh);
        }
      }
      nodes = getFooterElements();
      if (nodes != null) {
        incrementTop(nodes, dh);
      }
      setRowHeight(row, newHeight);

    } else if (isFooterRow(rowIdx)) {
      incrementHeight(getFooterElements(), dh);
      setFooterCellHeight(newHeight);
    }

    return newHeight;
  }

  private void resizeVertical(int by) {
    if (by == 0) {
      return;
    }

    String rowIdx = getResizerRow();
    Element cellElement = getCellElement(rowIdx, 0);
    int oldHeight = getRowHeight(rowIdx);
    int oldTop = cellElement.getOffsetTop();

    int newHeight = resizeRowHeight(rowIdx, oldHeight, by, getResizerModifiers());
    if (BeeConst.isUndef(newHeight)) {
      return;
    }

    int newTop = cellElement.getOffsetTop();
    int incr = newTop - oldTop + newHeight - oldHeight;
    if (incr != 0) {
      DomUtils.moveVerticalBy(resizerId, incr);
    }
  }

  private void selectRange(int visibleIndex, IsRow rowValue) {
    if (rowValue == null) {
      return;
    }
    long rowId = rowValue.getId();

    if (isRowSelected(rowId)) {
      for (int i = 0; i < getVisibleItemCount(); i++) {
        IsRow value = getVisibleItem(i);
        if (isRowSelected(value)) {
          selectRow(i, value);
        }
      }
      if (getSelectionModel() == null) {
        getSelectedRows().clear();
        fireSelectionCountChange();
      }

    } else {
      int lastSelectedRow = BeeConst.UNDEF;
      if (!getSelectedRows().isEmpty()) {
        List<Long> selectedIds = Lists.newArrayList(getSelectedRows().keySet());
        int maxIndex = -1;
        for (int i = 0; i < getVisibleItemCount(); i++) {
          if (i == visibleIndex) {
            continue;
          }
          int index = selectedIds.indexOf(getVisibleRowId(i));
          if (index > maxIndex) {
            maxIndex = index;
            lastSelectedRow = i;
          }
        }
      }

      if (lastSelectedRow == BeeConst.UNDEF) {
        selectRow(visibleIndex, rowValue);
      } else if (lastSelectedRow < visibleIndex) {
        for (int i = lastSelectedRow + 1; i <= visibleIndex; i++) {
          IsRow value = getVisibleItem(i);
          if (!isRowSelected(value)) {
            selectRow(i, value);
          }
        }
      } else {
        for (int i = visibleIndex; i < lastSelectedRow; i++) {
          IsRow value = getVisibleItem(i);
          if (!isRowSelected(value)) {
            selectRow(i, value);
          }
        }
      }
    }
  }

  private void selectRow(int visibleIndex, IsRow rowValue) {
    if (rowValue == null) {
      return;
    }
    long rowId = rowValue.getId();
    boolean wasSelected = isRowSelected(rowId);

    if (wasSelected) {
      getSelectedRows().remove(rowId);
    } else {
      getSelectedRows().put(rowId, new RowInfo(rowValue));
    }
    if (getSelectionModel() != null) {
      getSelectionModel().setSelected(rowValue, !wasSelected);
    }
    onSelectRow(visibleIndex, !wasSelected);
    fireSelectionCountChange();
  }

  private void setHasCellPreview(boolean hasCellPreview) {
    this.hasCellPreview = hasCellPreview;
  }

  private void setResizerBounds(int min, int max) {
    setResizerPositionMin(min);
    setResizerPositionMax(max);
  }

  private void setResizerCol(int resizerCol) {
    this.resizerCol = resizerCol;
  }

  private void setResizerModifiers(Modifiers resizerModifiers) {
    this.resizerModifiers = resizerModifiers;
  }

  private void setResizerPosition(int resizerPosition) {
    this.resizerPosition = resizerPosition;
  }

  private void setResizerPositionMax(int resizerPositionMax) {
    this.resizerPositionMax = resizerPositionMax;
  }

  private void setResizerPositionMin(int resizerPositionMin) {
    this.resizerPositionMin = resizerPositionMin;
  }

  private void setResizerRow(String resizerRow) {
    this.resizerRow = resizerRow;
  }

  private void setResizerStatus(ResizerMode resizerStatus) {
    this.resizerStatus = resizerStatus;
  }

  private void setResizing(boolean isResizing) {
    this.isResizing = isResizing;
  }

  private void setRowHeight(int row, int height) {
    Assert.isPositive(height);
    long id = getVisibleRowId(row);
    if (height == getBodyCellHeight()) {
      getResizedRows().remove(id);
    } else {
      getResizedRows().put(id, height);
    }
  }

  private void setVisibleRange(Range range, boolean clearData, boolean forceRangeChangeEvent) {
    int start = range.getStart();
    int length = range.getLength();
    Assert.nonNegative(start);
    Assert.isPositive(length);

    int oldStart = getPageStart();
    int oldSize = getPageSize();

    boolean pageStartChanged = (oldStart != start);
    boolean pageSizeChanged = (oldSize != length);

    if (pageStartChanged) {
      this.pageStart = start;
    }
    if (pageSizeChanged) {
      this.pageSize = length;
    }

    if (clearData) {
      rowData.clear();
    }

    if (pageStartChanged || pageSizeChanged || forceRangeChangeEvent) {
      RangeChangeEvent.fire(this, getVisibleRange());
    }
  }

  private void setZIndex(int zIndex) {
    this.zIndex = zIndex;
  }

  private void showColumnResizer(Element cellElement, int col) {
    int x = cellElement.getOffsetLeft() + cellElement.getOffsetWidth();
    int y = cellElement.getOffsetTop();
    int h = cellElement.getOffsetHeight();

    int handleWidth = ResizerMode.HORIZONTAL.getHandlePx();
    int barWidth = ResizerMode.HORIZONTAL.getBarPx();
    int width = Math.max(handleWidth, barWidth);
    int left = Math.max(x - width / 2, 0);

    int top = (barWidth > 0) ? 0 : y;
    int height = (barWidth > 0) ? getChildrenHeight() : h;

    Element resizerElement = getResizerContainer();
    StyleUtils.setRectangle(resizerElement, left, top, width, height);
    StyleUtils.setZIndex(resizerElement, incrementZIndex());
    resizerElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER, STYLE_RESIZER_HORIZONTAL));

    if (barWidth > 0) {
      Element barElement = getResizerBar();
      StyleUtils.setRectangle(barElement, (width - barWidth) / 2, 0, barWidth, height);
      barElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER_BAR,
          STYLE_RESIZER_BAR_HORIZONTAL));
      if (handleWidth > 0) {
        StyleUtils.hideDisplay(barElement);
      } else {
        StyleUtils.unhideDisplay(barElement);
      }
    }

    if (handleWidth > 0) {
      Element handleElement = getResizerHandle();
      StyleUtils.setRectangle(handleElement, (width - handleWidth) / 2, y, handleWidth, h);
      handleElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER_HANDLE,
          STYLE_RESIZER_HANDLE_HORIZONTAL));
      StyleUtils.unhideDisplay(handleElement);
    }

    int absLeft = cellElement.getAbsoluteLeft();
    int cellWidth = cellElement.getOffsetWidth();
    int min = absLeft + Math.min(Math.max(getMinColumnWidth(col), 0), cellWidth);
    int max = absLeft + Math.max(getMaxColumnWidth(col), cellWidth);
    setResizerBounds(min, max);

    StyleUtils.unhideDisplay(resizerElement);
    setResizerStatus(ResizerMode.HORIZONTAL);
    setResizerCol(col);
  }

  private void showRowResizer(Element cellElement, String rowIdx) {
    int x = cellElement.getOffsetLeft();
    int y = cellElement.getOffsetTop() + cellElement.getOffsetHeight();
    int w = cellElement.getOffsetWidth();

    int handleHeight = ResizerMode.VERTICAL.getHandlePx();
    int barHeight = ResizerMode.VERTICAL.getBarPx();
    int height = Math.max(handleHeight, barHeight);
    int top = Math.max(y - height / 2, 0);

    int left = (barHeight > 0) ? 0 : x;
    int width = (barHeight > 0) ? getRowWidth(rowIdx) : w;

    Element resizerElement = getResizerContainer();
    StyleUtils.setRectangle(resizerElement, left, top, width, height);
    StyleUtils.setZIndex(resizerElement, incrementZIndex());
    resizerElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER, STYLE_RESIZER_VERTICAL));

    if (barHeight > 0) {
      Element barElement = getResizerBar();
      StyleUtils.setRectangle(barElement, 0, (height - barHeight) / 2, width, barHeight);
      barElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER_BAR,
          STYLE_RESIZER_BAR_VERTICAL));
      if (handleHeight > 0) {
        StyleUtils.hideDisplay(barElement);
      } else {
        StyleUtils.unhideDisplay(barElement);
      }
    }

    if (handleHeight > 0) {
      Element handleElement = getResizerHandle();
      StyleUtils.setRectangle(handleElement, x, (height - handleHeight) / 2, w, handleHeight);
      handleElement.setClassName(StyleUtils.buildClasses(STYLE_RESIZER_HANDLE,
          STYLE_RESIZER_HANDLE_VERTICAL));
      StyleUtils.unhideDisplay(handleElement);
    }

    int absTop = cellElement.getAbsoluteTop();
    int cellHeight = cellElement.getOffsetHeight();

    Component component = getComponent(rowIdx);
    if (component != null) {
      int min = absTop + Math.min(Math.max(component.getMinHeight(), 0), cellHeight);
      int max = absTop + Math.max(component.getMaxHeight(), cellHeight);
      setResizerBounds(min, max);
    }

    StyleUtils.unhideDisplay(resizerElement);
    setResizerStatus(ResizerMode.VERTICAL);
    setResizerRow(rowIdx);
  }

  private void sinkEvents(Set<String> typeNames) {
    if (typeNames == null) {
      return;
    }

    int eventsToSink = 0;
    for (String typeName : typeNames) {
      int typeInt = Event.getTypeInt(typeName);
      if (typeInt > 0) {
        eventsToSink |= typeInt;
      }
    }
    if (eventsToSink > 0) {
      sinkEvents(eventsToSink);
    }
  }

  private void startEditing(IsRow rowValue, int col, Element cellElement, int charCode) {
    if (!isReadOnly() && !getColumnInfo(col).isColReadOnly()) {
      fireEvent(new EditStartEvent(rowValue, getColumnId(col), cellElement, charCode));
    }
  }
  
  private void startResizing(Event event) {
    setResizerModifiers(new Modifiers(event));

    if (getResizerStatus().getBarPx() > 0 && getResizerStatus().getHandlePx() > 0) {
      StyleUtils.unhideDisplay(resizerBarId);
    }

    getResizerMoveTimer().reset();
    setResizing(true);
  }

  private void stopResizing() {
    getResizerMoveTimer().stop();
    setResizing(false);
    hideResizer();

    setResizerModifiers(null);
  }
  
  private void updateCellContent(int visibleIndex, int col) {
    IsRow rowValue = getVisibleItem(visibleIndex);
    Assert.notNull(rowValue);
    AbstractColumn<?> column = getColumn(col);

    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    CellContext context = new CellContext(visibleIndex, col, rowValue.getId(), this);
    column.render(context, rowValue, cellBuilder);
    SafeHtml cellHtml = cellBuilder.toSafeHtml();

    Element cellElement = getCellElement(visibleIndex, col);
    Assert.notNull(cellElement, "cell not found: row " + visibleIndex + " col " + col);
    cellElement.setInnerHTML(cellHtml.asString());
  }

  private void updateComponent(GridComponentDescription src, Component dst) {
    if (src == null || dst == null) {
      return;
    }

    if (src.getStyle() != null) {
      dst.setStyle(StyleDescriptor.copyOf(src.getStyle()));
    }

    if (src.getHeight() != null) {
      dst.setCellHeight(src.getHeight());
    }
    if (src.getMinHeight() != null) {
      dst.setMinHeight(src.getMinHeight());
    }
    if (src.getMaxHeight() != null) {
      dst.setMaxHeight(src.getMaxHeight());
    }

    if (!BeeUtils.isEmpty(src.getPadding())) {
      dst.setPadding(Edges.parse(src.getPadding()));
    }
    if (!BeeUtils.isEmpty(src.getBorderWidth())) {
      dst.setBorderWidth(Edges.parse(src.getBorderWidth()));
    }
    if (!BeeUtils.isEmpty(src.getMargin())) {
      dst.setMargin(Edges.parse(src.getMargin()));
    }
  }

  private void updateOrder(String columnId, boolean hasModifiers) {
    Assert.notEmpty(columnId);
    Order ord = getSortOrder();
    int size = ord.getSize();

    if (size <= 0) {
      ord.add(columnId, true);
      return;
    }

    int index = ord.getIndex(columnId);
    if (BeeConst.isUndef(index)) {
      if (!hasModifiers) {
        ord.clear();
      }
      ord.add(columnId, true);
      return;
    }

    boolean asc = ord.isAscending(columnId);

    if (hasModifiers) {
      if (size == 1) {
        ord.setAscending(columnId, !asc);
      } else if (index == size - 1) {
        if (asc) {
          ord.setAscending(columnId, !asc);
        } else {
          ord.remove(columnId);
        }
      } else {
        ord.remove(columnId);
        ord.add(columnId, true);
      }

    } else if (size > 1) {
      ord.clear();
      ord.add(columnId, true);
    } else if (asc) {
      ord.setAscending(columnId, !asc);
    } else {
      ord.clear();
    }
  }
  
  private boolean updatePageSize() {
    if (hasPaging()) {
      int newPageSize = estimatePageSize();
      if (newPageSize > 0 && newPageSize != getPageSize()) {
        setVisibleRange(getPageStart(), newPageSize);
        return true;
      }
    }
    return false;
  }
}
