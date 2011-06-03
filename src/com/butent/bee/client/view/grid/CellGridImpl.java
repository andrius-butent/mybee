package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Element;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dialog.Notification;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.ColumnFooter;
import com.butent.bee.client.grid.ColumnHeader;
import com.butent.bee.client.grid.GridFactory;
import com.butent.bee.client.grid.RowIdColumn;
import com.butent.bee.client.grid.RowVersionColumn;
import com.butent.bee.client.i18n.LocaleUtils;
import com.butent.bee.client.layout.Absolute;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.ui.ConditionalStyle;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.client.view.edit.EditEndEvent;
import com.butent.bee.client.view.edit.EditStartEvent;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorFactory;
import com.butent.bee.client.view.search.SearchView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.filter.CompoundFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.BooleanValue;
import com.butent.bee.shared.data.value.TextValue;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Creates cell grid elements, connecting view and presenter elements of them.
 */

public class CellGridImpl extends Absolute implements GridView, SearchView, EditStartEvent.Handler {

  private class EditableColumn implements KeyDownHandler, BlurHandler, EditStopEvent.Handler {
    private final int colIndex;
    private final BeeColumn dataColumn;
    private final ColumnDescription columnDescription; 

    private Editor editor = null;
    private IsRow rowValue = null;

    private State state = State.PENDING;

    private EditableColumn(int colIndex, BeeColumn dataColumn,
        ColumnDescription columnDescription) {
      this.colIndex = colIndex;
      this.dataColumn = dataColumn;
      this.columnDescription = columnDescription;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof EditableColumn)) {
        return false;
      }
      return getColIndex() == ((EditableColumn) obj).getColIndex();
    }

    @Override
    public int hashCode() {
      return getColIndex();
    }

    public void onBlur(BlurEvent event) {
      if (State.OPEN.equals(getState())) {
        closeEditor();
      }
    }

    public void onEditStop(EditStopEvent event) {
      if (event.isFinished()) {
        endEdit();
      } else if (event.isError()) {
        notifySevere(event.getMessage());
      } else {
        closeEditor();
      }
    }

    public void onKeyDown(KeyDownEvent event) {
      int keyCode = event.getNativeKeyCode();
      NativeEvent nativeEvent = event.getNativeEvent();

      switch (keyCode) {
        case KeyCodes.KEY_ESCAPE:
          EventUtils.eatEvent(nativeEvent);
          closeEditor();
          break;

        case KeyCodes.KEY_ENTER:
          EventUtils.eatEvent(nativeEvent);
          endEdit();
          break;

        case KeyCodes.KEY_TAB:
        case KeyCodes.KEY_UP:
        case KeyCodes.KEY_DOWN:
        case KeyCodes.KEY_PAGEDOWN:
        case KeyCodes.KEY_PAGEUP:
          EventUtils.eatEvent(event.getNativeEvent());
          if (endEdit()) {
            getGrid().handleKeyboardNavigation(keyCode, EventUtils.hasModifierKey(nativeEvent));
          }
          break;
      }
    }

    private void closeEditor() {
      setState(State.CLOSED);
      StyleUtils.hideDisplay(getEditor().asWidget());

      getGrid().refocus();
      getGrid().setEditing(false);
    }

    private boolean endEdit() {
      if (State.OPEN.equals(getState())) {
        String oldValue = getRowValue().getString(getColIndex());
        String editorValue = getEditor().getValue();

        if (BeeUtils.equalsTrimRight(oldValue, editorValue)) {
          closeEditor();
          return true;
        }

        String errorMessage = getEditor().validate();
        if (!BeeUtils.isEmpty(errorMessage)) {
          notifySevere(editorValue, errorMessage);
          return false;
        }

        String newValue = getEditor().getNormalizedValue();
        closeEditor();

        if (!BeeUtils.equalsTrimRight(oldValue, newValue)) {
          updateCell(getRowValue(), getDataColumn(), oldValue, newValue);
        }
        return true;
      }
      return false;
    }

    private int getColIndex() {
      return colIndex;
    }

    private BeeColumn getDataColumn() {
      return dataColumn;
    }

    private Editor getEditor() {
      return editor;
    }

    private IsRow getRowValue() {
      return rowValue;
    }

    private State getState() {
      return state;
    }

    private void setEditor(Editor editor) {
      this.editor = editor;
    }

    private void setRowValue(IsRow rowValue) {
      this.rowValue = rowValue;
    }

    private void setState(State state) {
      this.state = state;
    }
  }

  private class FilterUpdater implements ValueUpdater<String> {
    public void update(String value) {
      if (filterChangeHandler != null) {
        filterChangeHandler.onChange(null);
      }
    }
  }

  private static final String STYLE_EDITOR = "bee-CellGridEditor";

  private Presenter viewPresenter = null;

  private ChangeHandler filterChangeHandler = null;
  private final FilterUpdater filterUpdater = new FilterUpdater();

  private final CellGrid grid = new CellGrid();
  private GridDescription gridDescription = null;
  
  private Evaluator<TextValue> rowMessage = null;
  private Evaluator<BooleanValue> rowEditable = null;

  private final Map<String, EditableColumn> editableColumns = Maps.newHashMap();

  private final Notification notification = new Notification();

  public CellGridImpl() {
    super();
  }

  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    filterChangeHandler = handler;
    return new HandlerRegistration() {
      public void removeHandler() {
        filterChangeHandler = null;
      }
    };
  }

  public HandlerRegistration addEditEndHandler(EditEndEvent.Handler handler) {
    return addHandler(handler, EditEndEvent.getType());
  }

  public void applyOptions(String options) {
    if (BeeUtils.isEmpty(options)) {
      return;
    }

    boolean redraw = false;
    String[] opt = BeeUtils.split(options, ";");

    for (int i = 0; i < opt.length; i++) {
      String[] arr = BeeUtils.split(opt[i], " ");
      int len = arr.length;
      if (len <= 1) {
        continue;
      }
      String cmd = arr[0].trim().toLowerCase();
      String args = opt[i].trim().substring(cmd.length() + 1).trim();

      int[] xp = new int[len - 1];
      String[] sp = new String[len - 1];

      for (int j = 1; j < len; j++) {
        sp[j - 1] = arr[j].trim();
        if (BeeUtils.isDigit(arr[j])) {
          xp[j - 1] = BeeUtils.toInt(arr[j]);
        } else {
          xp[j - 1] = 0;
        }
      }

      Edges edges = null;
      switch (len - 1) {
        case 1:
          edges = new Edges(xp[0]);
          break;
        case 2:
          edges = new Edges(xp[0], xp[1]);
          break;
        case 3:
          edges = new Edges(xp[0], xp[1], xp[2]);
          break;
        default:
          edges = new Edges(xp[0], xp[1], xp[2], xp[3]);
      }

      int cc = getGrid().getColumnCount();
      String colId = sp[0];
      if (BeeUtils.isDigit(colId) && xp[0] < cc) {
        colId = getGrid().getColumnId(xp[0]);
      }

      String msg = null;

      if (cmd.startsWith("bh")) {
        msg = "setBodyCellHeight " + xp[0];
        getGrid().setBodyCellHeight(xp[0]);
        redraw = true;
      } else if (cmd.startsWith("bp")) {
        msg = "setBodyCellPadding " + edges.getCssValue();
        getGrid().setBodyCellPadding(edges);
        redraw = true;
      } else if (cmd.startsWith("bw")) {
        msg = "setBodyBorderWidth " + edges.getCssValue();
        getGrid().setBodyBorderWidth(edges);
        redraw = true;
      } else if (cmd.startsWith("bm")) {
        msg = "setBodyCellMargin " + edges.getCssValue();
        getGrid().setBodyCellMargin(edges);
        redraw = true;
      } else if (cmd.startsWith("bf")) {
        msg = "setColumnBodyFont " + args;
        getGrid().setBodyFont(args);
        redraw = true;

      } else if (cmd.startsWith("hh")) {
        msg = "setHeaderCellHeight " + xp[0];
        getGrid().setHeaderCellHeight(xp[0]);
        redraw = true;
      } else if (cmd.startsWith("hp")) {
        msg = "setHeaderCellPadding " + edges.getCssValue();
        getGrid().setHeaderCellPadding(edges);
        redraw = true;
      } else if (cmd.startsWith("hw")) {
        msg = "setHeaderBorderWidth " + edges.getCssValue();
        getGrid().setHeaderBorderWidth(edges);
        redraw = true;
      } else if (cmd.startsWith("hm")) {
        msg = "setHeaderCellMargin " + edges.getCssValue();
        getGrid().setHeaderCellMargin(edges);
        redraw = true;
      } else if (cmd.startsWith("hf")) {
        msg = "setColumnHeaderFont " + args;
        getGrid().setHeaderFont(args);
        redraw = true;

      } else if (cmd.startsWith("fh")) {
        msg = "setFooterCellHeight " + xp[0];
        getGrid().setFooterCellHeight(xp[0]);
        redraw = true;
      } else if (cmd.startsWith("fp")) {
        msg = "setFooterCellPadding " + edges.getCssValue();
        getGrid().setFooterCellPadding(edges);
        redraw = true;
      } else if (cmd.startsWith("fw")) {
        msg = "setFooterBorderWidth " + edges.getCssValue();
        getGrid().setFooterBorderWidth(edges);
        redraw = true;
      } else if (cmd.startsWith("fm")) {
        msg = "setFooterCellMargin " + edges.getCssValue();
        getGrid().setFooterCellMargin(edges);
        redraw = true;
      } else if (cmd.startsWith("ff")) {
        msg = "setColumnFooterFont " + args;
        getGrid().setFooterFont(args);
        redraw = true;

      } else if (cmd.startsWith("chw") && len > 2) {
        msg = "setColumnHeaderWidth " + colId + " " + xp[1];
        getGrid().setColumnHeaderWidth(colId, xp[1]);
        redraw = true;
      } else if (cmd.startsWith("chf") && len > 2) {
        String font = ArrayUtils.join(sp, " ", 1);
        msg = "setColumnHeaderFont " + colId + " " + font;
        getGrid().setColumnHeaderFont(colId, font);
        redraw = true;

      } else if (cmd.startsWith("cbw") && len > 2) {
        msg = "setColumnBodyWidth " + colId + " " + xp[1];
        getGrid().setColumnBodyWidth(colId, xp[1]);
        redraw = true;
      } else if (cmd.startsWith("cbf") && len > 2) {
        String font = ArrayUtils.join(sp, " ", 1);
        msg = "setColumnBodyFont " + colId + " " + font;
        getGrid().setColumnBodyFont(colId, font);
        redraw = true;

      } else if (cmd.startsWith("cfw") && len > 2) {
        msg = "setColumnFooterWidth " + colId + " " + xp[1];
        getGrid().setColumnFooterWidth(colId, xp[1]);
        redraw = true;
      } else if (cmd.startsWith("cff") && len > 2) {
        String font = ArrayUtils.join(sp, " ", 1);
        msg = "setColumnFooterFont " + colId + " " + font;
        getGrid().setColumnFooterFont(colId, font);
        redraw = true;

      } else if (cmd.startsWith("cw") && len > 2) {
        if (len <= 3) {
          msg = "setColumnWidth " + colId + " " + xp[1];
          getGrid().setColumnWidth(colId, xp[1]);
          redraw = true;
        } else {
          msg = "setColumnWidth " + colId + " " + xp[1] + " " + StyleUtils.parseUnit(sp[2]);
          getGrid().setColumnWidth(colId, xp[1], StyleUtils.parseUnit(sp[2]));
          redraw = true;
        }

      } else if (cmd.startsWith("minw")) {
        msg = "setMinCellWidth " + xp[0];
        getGrid().setMinCellWidth(xp[0]);
      } else if (cmd.startsWith("maxw")) {
        msg = "setMaxCellWidth " + xp[0];
        getGrid().setMaxCellWidth(xp[0]);
      } else if (cmd.startsWith("minh")) {
        msg = "setMinCellHeight " + xp[0];
        getGrid().setMinBodyCellHeight(xp[0]);
      } else if (cmd.startsWith("maxh")) {
        msg = "setMaxCellHeight " + xp[0];
        getGrid().setMaxBodyCellHeight(xp[0]);

      } else if (cmd.startsWith("zm")) {
        msg = "setResizerMoveSensitivityMillis " + xp[0];
        getGrid().setResizerMoveSensitivityMillis(xp[0]);
      } else if (cmd.startsWith("zs")) {
        msg = "setResizerShowSensitivityMillis " + xp[0];
        getGrid().setResizerShowSensitivityMillis(xp[0]);

      } else if (cmd.startsWith("fit")) {
        if (getGrid().contains(colId)) {
          getGrid().autoFitColumn(colId);
          msg = "autoFitColumn " + colId;
        } else {
          getGrid().autoFit();
          msg = "autoFit";
        }

      } else if (cmd.startsWith("ps")) {
        if (xp[0] > 0) {
          updatePageSize(xp[0], false);
          msg = "updatePageSize " + xp[0];
        } else {
          int oldPageSize = getGrid().getPageSize();
          int newPageSize = getGrid().estimatePageSize();
          if (newPageSize > 0) {
            updatePageSize(newPageSize, false);
          }
          msg = "page size: old " + oldPageSize + " new " + newPageSize;
        }
      }

      if (msg == null) {
        BeeKeeper.getLog().warning("unrecognized command", opt[i]);
      } else {
        BeeKeeper.getLog().info(msg);
      }
    }

    if (redraw) {
      getGrid().redraw();
    }
  }

  public void create(List<BeeColumn> dataCols, int rowCount, BeeRowSet rowSet,
      GridDescription gridDescr, boolean hasSearch) {
    boolean hasHeaders = (gridDescr == null) ? true : !BeeUtils.isFalse(gridDescr.hasHeaders());
    boolean hasFooters = hasSearch;
    if (hasFooters && gridDescr != null && BeeUtils.isFalse(gridDescr.hasFooters())) {
      hasFooters = false;
    }

    boolean showColumnWidths = true;
    List<ColumnDescription> columnDescriptions = null;

    if (gridDescr != null) {
      setGridDescription(gridDescr);

      if (hasHeaders && gridDescr.getHeader() != null) {
        getGrid().setHeaderComponent(gridDescr.getHeader());
      }
      if (gridDescr.getBody() != null) {
        getGrid().setBodyComponent(gridDescr.getBody());
      }
      if (hasFooters && gridDescr.getFooter() != null) {
        getGrid().setFooterComponent(gridDescr.getFooter());
      }

      if (BeeUtils.isTrue(gridDescr.isReadOnly())) {
        getGrid().setReadOnly(true);
      }

      if (gridDescr.getMinColumnWidth() != null) {
        getGrid().setMinCellWidth(gridDescr.getMinColumnWidth());
      }
      if (gridDescr.getMaxColumnWidth() != null) {
        getGrid().setMaxCellWidth(gridDescr.getMaxColumnWidth());
      }

      if (BeeUtils.isFalse(gridDescr.showColumnWidths())) {
        showColumnWidths = false;
      }

      if (gridDescr.getRowStyles() != null) {
        List<ConditionalStyle> rowStyles = Lists.newArrayList();
        for (ConditionalStyleDeclaration csd : gridDescr.getRowStyles()) {
          ConditionalStyle conditionalStyle = ConditionalStyle.create(csd);
          if (conditionalStyle != null) {
            rowStyles.add(conditionalStyle);
          }
        }

        if (!rowStyles.isEmpty()) {
          getGrid().setRowStyles(rowStyles);
        }
      }
      
      if (gridDescr.getRowMessage() != null) {
        setRowMessage(Evaluator.<TextValue>create(gridDescr.getRowMessage()));
      }
      if (gridDescr.getRowEditable() != null) {
        setRowEditable(Evaluator.<BooleanValue>create(gridDescr.getRowEditable()));
      }
      
      columnDescriptions = gridDescr.getVisibleColumns();
    }
    
    if (columnDescriptions == null) {
      columnDescriptions = Lists.newArrayList();
    }
    if (columnDescriptions.isEmpty()) {
      ColumnDescription idCol = new ColumnDescription(ColType.ID, "rowId");
      idCol.setCaption("Id");
      idCol.setReadOnly(true);
      idCol.setSortable(true);
      idCol.setVisible(true);
      idCol.setShowWidth(showColumnWidths);
      idCol.setHasFooter(hasFooters);
      columnDescriptions.add(idCol);

      for (int i = 0; i < dataCols.size(); i++) {
        ColumnDescription dataCol = new ColumnDescription(ColType.DATA, dataCols.get(i).getLabel());
        dataCol.setReadOnly(getGrid().isReadOnly());
        dataCol.setSortable(true);
        dataCol.setVisible(true);
        dataCol.setShowWidth(showColumnWidths);
        dataCol.setHasFooter(hasFooters);
        dataCol.setSource(dataCols.get(i).getLabel());
        columnDescriptions.add(dataCol);
      }
      
      ColumnDescription verCol = new ColumnDescription(ColType.VERSION, "rowVersion");
      verCol.setCaption("Version");
      verCol.setReadOnly(true);
      verCol.setSortable(true);
      verCol.setVisible(true);
      verCol.setShowWidth(showColumnWidths);
      verCol.setHasFooter(hasFooters);
      columnDescriptions.add(verCol);
    }

    Column<IsRow, ?> column;
    ColumnHeader header = null;
    ColumnFooter footer = null;
    int dataIndex;

    for (ColumnDescription columnDescription : columnDescriptions) {
      String columnId = columnDescription.getName();
      ColType colType = columnDescription.getType();
      if (BeeUtils.isEmpty(columnId) || colType == null) {
        continue;
      }
      
      String source = columnDescription.getSource();
      dataIndex = BeeConst.UNDEF;
      column = null;
      
      switch (colType) {
        case ID:
          column = new RowIdColumn();
          break;

        case VERSION:
          column = new RowVersionColumn();
          break;

        case DATA:
        case RELATED:
          for (int i = 0; i < dataCols.size(); i++) {
            BeeColumn dataColumn = dataCols.get(i);
            if (BeeUtils.same(columnId, dataColumn.getLabel())) {
              column = GridFactory.createColumn(dataColumn, i);
              getEditableColumns().put(columnId,
                  new EditableColumn(i, dataColumn, columnDescription));
              dataIndex = i;
              break;
            }
          }
          break;
          
        case CALCULATED:
          break;
      }
      
      if (column == null) {
        continue;
      }
      if (columnDescription.isSortable()) {
        column.setSortable(true);
      }
      
      if (hasHeaders) {
        String caption = BeeUtils.ifString(columnDescription.getCaption(), columnId);
        header = new ColumnHeader(caption, showColumnWidths && columnDescription.showWidth());
      }
      if (hasFooters && BeeUtils.isTrue(columnDescription.hasFooter())
          && !BeeUtils.isEmpty(source)) {
        footer = new ColumnFooter(source, filterUpdater); 
      } else {
        footer = null;
      }

      getGrid().addColumn(columnId, dataIndex, column, header, footer);
      getGrid().setColumnInfo(columnId, columnDescription);
    }

    getGrid().setRowCount(rowCount);

    if (rowSet != null) {
      getGrid().estimateColumnWidths(rowSet.getRows().getList(),
          Math.min(rowSet.getNumberOfRows(), 3));
    }
    getGrid().estimateHeaderWidths();

    getGrid().addEditStartHandler(this);

    add(getGrid());
    add(getNotification());
  }

  public int estimatePageSize(int containerWidth, int containerHeight) {
    return getGrid().estimatePageSize(containerWidth, containerHeight, true);
  }

  public RowInfo getActiveRowInfo() {
    return getGrid().getActiveRowInfo();
  }

  public Filter getFilter(List<? extends IsColumn> columns) {
    List<Header<?>> footers = getGrid().getFooters();

    if (footers == null || footers.size() <= 0) {
      return null;
    }
    Filter filter = null;

    for (Header<?> footer : footers) {
      if (!(footer instanceof ColumnFooter)) {
        continue;
      }
      String input = BeeUtils.trim(((ColumnFooter) footer).getValue());
      if (BeeUtils.isEmpty(input)) {
        continue;
      }
      String source = ((ColumnFooter) footer).getSource();
      if (BeeUtils.isEmpty(source)) {
        continue;
      }
      Filter flt = DataUtils.parseExpression(source + " " + input, columns);

      if (flt == null) {
        continue;
      }
      if (filter == null) {
        filter = flt;
      } else {
        filter = CompoundFilter.and(filter, flt);
      }
    }
    return filter;
  }

  public CellGrid getGrid() {
    return grid;
  }

  public Collection<RowInfo> getSelectedRows() {
    return getGrid().getSelectedRows().values();
  }

  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  public String getWidgetId() {
    return getId();
  }

  public boolean isColumnEditable(String columnId) {
    if (BeeUtils.isEmpty(columnId)) {
      return false;
    }
    return getEditableColumns().containsKey(columnId);
  }

  public boolean isRowSelected(long rowId) {
    return getGrid().isRowSelected(rowId);
  }

  public void notifyInfo(String... messages) {
    showNote(Level.INFO, messages);
  }

  public void notifySevere(String... messages) {
    showNote(Level.SEVERE, messages);
  }

  public void notifyWarning(String... messages) {
    showNote(Level.WARNING, messages);
  }

  public void onEditStart(EditStartEvent event) {
    Assert.notNull(event);
    String columnId = event.getColumnId();
    EditableColumn editableColumn = getEditableColumn(columnId);
    if (editableColumn == null) {
      return;
    }

    if (ValueType.BOOLEAN.equals(editableColumn.getDataColumn().getType())
        && BeeUtils.inList(event.getCharCode(), EditorFactory.START_MOUSE_CLICK,
            EditorFactory.START_KEY_ENTER)) {
      IsRow rowValue = event.getRowValue();
      BeeColumn dataColumn = editableColumn.getDataColumn();

      String oldValue = rowValue.getString(editableColumn.getColIndex());
      Boolean b = !BeeUtils.toBoolean(oldValue);
      if (!b && dataColumn.isNullable()) {
        b = null;
      }
      String newValue = BooleanValue.pack(b);

      updateCell(rowValue, dataColumn, oldValue, newValue);
      return;
    }

    getGrid().setEditing(true);

    Editor editor = editableColumn.getEditor();
    if (editor == null) {
      editor = EditorFactory.createEditor(editableColumn.getDataColumn());
      editor.asWidget().addStyleName(STYLE_EDITOR);

      Column<IsRow, ?> gridColumn = getGrid().getColumn(columnId);
      LocaleUtils.copyDateTimeFormat(gridColumn, editor);
      LocaleUtils.copyNumberFormat(gridColumn, editor);

      editor.addKeyDownHandler(editableColumn);
      editor.addBlurHandler(editableColumn);
      editor.addEditStopHandler(editableColumn);

      add(editor);

      editableColumn.setEditor(editor);
    }

    editableColumn.setRowValue(event.getRowValue());
    editableColumn.setState(State.OPEN);

    Element editorElement = editor.asWidget().getElement();
    if (event.getSourceElement() != null) {
      StyleUtils.copyBox(event.getSourceElement(), editorElement);
      StyleUtils.copyFont(event.getSourceElement(), editorElement);

      int x = getGrid().getElement().getScrollLeft();
      int left = StyleUtils.getLeft(editorElement);
      int width = StyleUtils.getWidth(editorElement);
      int margins = event.getSourceElement().getOffsetWidth() - width;
      int maxWidth = getGrid().getElement().getClientWidth();

      if (x > 0 || left + width + margins > maxWidth) {
        left -= x;
        int newWidth = width;
        if (left < 0) {
          newWidth += left;
          left = 0;
        }
        if (left + newWidth + margins > maxWidth) {
          if (left > 0) {
            left = Math.max(0, maxWidth - newWidth - margins);
          }
          if (left + newWidth + margins > maxWidth) {
            newWidth = maxWidth - left - margins;
          }
        }
        StyleUtils.setLeft(editorElement, left);
        if (newWidth > 0 && newWidth != width) {
          StyleUtils.setWidth(editorElement, newWidth);
        }
      }

      int y = getGrid().getElement().getScrollTop();
      if (y > 0) {
        DomUtils.moveVerticalBy(editorElement, -y);
      }
    }

    StyleUtils.setZIndex(editorElement, getGrid().getZIndex() + 1);
    StyleUtils.unhideDisplay(editorElement);
    editor.setFocus(true);

    editor.startEdit(event.getRowValue().getString(editableColumn.getColIndex()),
        BeeUtils.toChar(event.getCharCode()));
  }

  public void refreshCell(long rowId, String columnId) {
    getGrid().refreshCell(rowId, columnId);
  }

  public void setViewPresenter(Presenter presenter) {
    this.viewPresenter = presenter;
  }

  public void setVisibleRange(int start, int length) {
    getGrid().setVisibleRange(start, length);
  }

  public void updatePageSize(int pageSize, boolean init) {
    Assert.isPositive(pageSize);
    int oldSize = getGrid().getPageSize();

    if (oldSize == pageSize) {
      if (init) {
        getGrid().setVisibleRangeAndClearData(getGrid().getVisibleRange(), true);
      }
    } else {
      getGrid().setVisibleRange(getGrid().getPageStart(), pageSize);
    }
  }

  private EditableColumn getEditableColumn(String columnId) {
    if (BeeUtils.isEmpty(columnId)) {
      return null;
    }
    return getEditableColumns().get(columnId);
  }

  private Map<String, EditableColumn> getEditableColumns() {
    return editableColumns;
  }

  private GridDescription getGridDescription() {
    return gridDescription;
  }

  private Notification getNotification() {
    return notification;
  }

  private Evaluator<BooleanValue> getRowEditable() {
    return rowEditable;
  }

  private Evaluator<TextValue> getRowMessage() {
    return rowMessage;
  }

  private void setGridDescription(GridDescription gridDescription) {
    this.gridDescription = gridDescription;
  }

  private void setRowEditable(Evaluator<BooleanValue> rowEditable) {
    this.rowEditable = rowEditable;
  }

  private void setRowMessage(Evaluator<TextValue> rowMessage) {
    this.rowMessage = rowMessage;
  }

  private void showNote(Level level, String... messages) {
    StyleUtils.setZIndex(getNotification(), getGrid().getZIndex() + 1);
    getNotification().show(level, messages);
  }

  private void updateCell(IsRow rowValue, IsColumn dataColumn, String oldValue, String newValue) {
    getGrid().preliminaryUpdate(rowValue.getId(), dataColumn.getLabel(), newValue);
    fireEvent(new EditEndEvent(rowValue, dataColumn, oldValue, newValue));
  }
}
