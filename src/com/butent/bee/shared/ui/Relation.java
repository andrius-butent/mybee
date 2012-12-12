package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.HasItems;
import com.butent.bee.shared.Holder;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.data.view.ViewColumn;
import com.butent.bee.shared.menu.MenuConstants;
import com.butent.bee.shared.utils.ArrayUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Relation implements BeeSerializable, HasInfo, HasViewName {

  public enum Caching {
    NONE, QUERY, LOCAL, GLOBAL
  }

  public enum RenderMode {
    SOURCE, TARGET
  }

  private enum Serial {
    ATTRIBUTES, SELECTOR_COLUMNS, ROW_RENDERER_DESCR, ROW_RENDER, ROW_RENDER_TOKENS
  }

  public static final String TAG_ROW_RENDERER = "rowRenderer";
  public static final String TAG_ROW_RENDER = "rowRender";
  public static final String TAG_ROW_RENDER_TOKEN = "rowRenderToken";

  public static final String TAG_SELECTOR_COLUMN = "selectorColumn";

  public static final String ATTR_CACHING = "caching";
  public static final String ATTR_OPERATOR = "operator";

  public static final String ATTR_CHOICE_COLUMNS = "choiceColumns";
  public static final String ATTR_SEARCHABLE_COLUMNS = "searchableColumns";

  public static final String ATTR_ITEM_TYPE = "itemType";

  public static Relation create() {
    return new Relation();
  }

  public static Relation create(Map<String, String> attributes,
      List<SelectorColumn> selectorColumns, RendererDescription rowRendererDescription,
      Calculation rowRender, List<RenderableToken> rowRenderTokens) {
    Relation relation = new Relation();
    relation.setAttributes(attributes);

    if (!BeeUtils.isEmpty(selectorColumns)) {
      relation.setSelectorColumns(selectorColumns);
    }

    if (rowRendererDescription != null) {
      relation.setRowRendererDescription(rowRendererDescription);
    }
    if (rowRender != null) {
      relation.setRowRender(rowRender);
    }
    if (!BeeUtils.isEmpty(rowRenderTokens)) {
      relation.setRowRenderTokens(rowRenderTokens);
    }

    return relation;
  }

  public static Relation create(String viewName, List<String> columns) {
    Relation relation = new Relation();

    if (!BeeUtils.isEmpty(viewName)) {
      relation.setViewName(viewName);
    }
    if (!BeeUtils.isEmpty(columns)) {
      relation.getChoiceColumns().addAll(columns);
      relation.getSearchableColumns().addAll(columns);
    }

    return relation;
  }

  public static Relation restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    Relation relation = new Relation();
    relation.deserialize(s);
    return relation;
  }

  private final Map<String, String> attributes = Maps.newHashMap();

  private String viewName = null;

  private Filter filter = null;
  private Order order = null;

  private Caching caching = null;
  private Operator operator = null;

  private RendererDescription rowRendererDescription = null;
  private Calculation rowRender = null;
  private List<RenderableToken> rowRenderTokens = null;

  private String itemKey = null;

  private final List<SelectorColumn> selectorColumns = Lists.newArrayList();

  private final List<String> choiceColumns = Lists.newArrayList();
  private final List<String> searchableColumns = Lists.newArrayList();

  private MenuConstants.ITEM_TYPE itemType = null;
  private Integer visibleLines = null;

  private String originalTarget = null;
  private final List<String> originalRenderColumns = Lists.newArrayList();

  private RenderMode renderMode = null;

  private Relation() {
  }

  @Override
  public void deserialize(String s) {
    String[] arr = Codec.beeDeserializeCollection(s);
    Serial[] members = Serial.values();
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      Serial member = members[i];
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case ATTRIBUTES:
          String[] attr = Codec.beeDeserializeCollection(value);
          if (!ArrayUtils.isEmpty(attr)) {
            getAttributes().clear();
            for (int j = 0; j < attr.length; j += 2) {
              getAttributes().put(attr[j], attr[j + 1]);
            }
          }
          break;

        case SELECTOR_COLUMNS:
          String[] cols = Codec.beeDeserializeCollection(value);
          if (!ArrayUtils.isEmpty(cols)) {
            getSelectorColumns().clear();
            for (int j = 0; j < cols.length; j++) {
              BeeUtils.addNotNull(getSelectorColumns(), SelectorColumn.restore(cols[j]));
            }
          }
          break;

        case ROW_RENDERER_DESCR:
          setRowRendererDescription(RendererDescription.restore(value));
          break;

        case ROW_RENDER:
          setRowRender(Calculation.restore(value));
          break;

        case ROW_RENDER_TOKENS:
          setRowRenderTokens(RenderableToken.restoreList(value));
          break;
      }
    }
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }
  
  public Caching getCaching() {
    return caching;
  }

  public List<String> getChoiceColumns() {
    return choiceColumns;
  }

  public String getEditForm() {
    return getAttribute(UiConstants.ATTR_EDIT_FORM);
  }

  public Integer getEditKey() {
    return BeeUtils.toIntOrNull(getAttribute(UiConstants.ATTR_EDIT_KEY));
  }
  
  public Filter getFilter() {
    return filter;
  }
  
  @Override
  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(getAttributes());
    PropertyUtils.addProperties(info,
        "View Name", getViewName(),
        "Filter", getFilter(),
        "Order", getOrder(),
        "Caching", getCaching(),
        "Operator", getOperator(),
        "Item Type", getItemType(),
        "Visible Lines", getVisibleLines(),
        "Item Key", getItemKey(),
        "Render Mode", getRenderMode());

    if (!getChoiceColumns().isEmpty()) {
      PropertyUtils.addProperties(info, "Choice Columns", getChoiceColumns());
    }
    if (!getSearchableColumns().isEmpty()) {
      PropertyUtils.addProperties(info, "Searchable Columns", getSearchableColumns());
    }

    if (!getSelectorColumns().isEmpty()) {
      PropertyUtils.appendWithIndex(info, "Selector Columns", null, getSelectorColumns());
    }

    if (getRowRendererDescription() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Row Renderer",
          getRowRendererDescription().getInfo());
    }
    if (getRowRender() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Row Render", getRowRender().getInfo());
    }
    if (getRowRenderTokens() != null) {
      PropertyUtils.appendWithIndex(info, "Row Render Tokens", "token", getRowRenderTokens());
    }

    return info;
  }

  public String getItemKey() {
    return itemKey;
  }

  public MenuConstants.ITEM_TYPE getItemType() {
    return itemType;
  }

  public String getNewRowCaption() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_CAPTION);
  }

  public String getNewRowColumns() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_COLUMNS);
  }

  public String getNewRowForm() {
    return getAttribute(UiConstants.ATTR_NEW_ROW_FORM);
  }

  public Operator getOperator() {
    return operator;
  }

  public Order getOrder() {
    return order;
  }

  public List<String> getOriginalRenderColumns() {
    return Lists.newArrayList(originalRenderColumns);
  }

  public String getOriginalTarget() {
    return originalTarget;
  }

  public Calculation getRowRender() {
    return rowRender;
  }

  public RendererDescription getRowRendererDescription() {
    return rowRendererDescription;
  }

  public List<RenderableToken> getRowRenderTokens() {
    return rowRenderTokens;
  }

  public List<String> getSearchableColumns() {
    return searchableColumns;
  }

  public List<SelectorColumn> getSelectorColumns() {
    return selectorColumns;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  public Integer getVisibleLines() {
    return visibleLines;
  }

  public boolean hasRowRenderer() {
    return getRowRendererDescription() != null || getRowRender() != null
        || !BeeUtils.isEmpty(getRowRenderTokens()) || !BeeUtils.isEmpty(getItemKey());
  }

  public void initialize(DataInfo.Provider provider, String targetView, Holder<String> target,
      Holder<List<String>> renderColumns, RenderMode mode) {

    setOriginalTarget(target.get());
    setOriginalRenderColumns(renderColumns.get());

    if (mode != null) {
      setRenderMode(mode);
    }

    String sourceView = getAttribute(UiConstants.ATTR_VIEW_NAME);
    if (!BeeUtils.isEmpty(sourceView)) {
      setViewName(sourceView);
    }

    String cache = getAttribute(ATTR_CACHING);
    if (!BeeUtils.isEmpty(cache)) {
      setCaching(NameUtils.getEnumByName(Caching.class, cache));
    }
    String op = getAttribute(ATTR_OPERATOR);
    if (!BeeUtils.isEmpty(op)) {
      setOperator(NameUtils.getEnumByName(Operator.class, op));
    }

    String it = getAttribute(ATTR_ITEM_TYPE);
    if (!BeeUtils.isEmpty(it)) {
      setItemType(NameUtils.getEnumByName(MenuConstants.ITEM_TYPE.class, it));
    }
    String lines = getAttribute(HasVisibleLines.ATTR_VISIBLE_LINES);
    if (BeeUtils.isPositiveInt(lines)) {
      setVisibleLines(BeeUtils.toInt(lines));
    }

    String key = getAttribute(HasItems.ATTR_ITEM_KEY);
    if (!BeeUtils.isEmpty(key)) {
      setItemKey(key);
    }

    String flt = getAttribute(UiConstants.ATTR_FILTER);
    String ord = getAttribute(UiConstants.ATTR_ORDER);

    List<String> displCols = NameUtils.toList(getAttribute(ATTR_CHOICE_COLUMNS));
    List<String> searchCols = NameUtils.toList(getAttribute(ATTR_SEARCHABLE_COLUMNS));

    List<String> selectorColumnNames = getSelectorColumnNames();

    DataInfo targetInfo = BeeUtils.isEmpty(targetView)
        ? null : provider.getDataInfo(targetView, true);

    if (BeeUtils.isEmpty(getViewName()) && targetInfo != null) {
      sourceView = deduceViewName(provider, targetInfo, target.get(), renderColumns.get(),
          displCols, searchCols, selectorColumnNames);
      if (!BeeUtils.isEmpty(sourceView)) {
        setViewName(sourceView);
      }
    }

    if (targetInfo != null) {
      String rt = resolveTarget(targetInfo, target.get(), renderColumns.get());
      if (BeeUtils.isEmpty(renderColumns.get()) && renderTarget()) {
        renderColumns.set(deriveRenderColumns(targetInfo, target.get(), rt));
      }
      if (!BeeUtils.isEmpty(rt)) {
        target.set(rt);
      }
    }

    DataInfo sourceInfo = BeeUtils.isEmpty(getViewName())
        ? null : provider.getDataInfo(getViewName(), true);

    if (sourceInfo != null && !BeeUtils.isEmpty(flt)) {
      setFilter(sourceInfo.parseFilter(flt));
    }
    if (sourceInfo != null && !BeeUtils.isEmpty(ord)) {
      setOrder(sourceInfo.parseOrder(ord));
    }

    if (!BeeUtils.isEmpty(displCols)) {
      if (sourceInfo == null) {
        setChoiceColumns(displCols);
      } else {
        setChoiceColumns(sourceInfo.parseColumns(displCols));
      }
    }

    if (!BeeUtils.isEmpty(searchCols)) {
      if (sourceInfo == null) {
        setSearchableColumns(searchCols);
      } else {
        setSearchableColumns(sourceInfo.parseColumns(searchCols));
      }
    }

    if (getChoiceColumns().isEmpty() && !selectorColumnNames.isEmpty()) {
      if (sourceInfo == null) {
        setChoiceColumns(selectorColumnNames);
      } else {
        setChoiceColumns(sourceInfo.parseColumns(selectorColumnNames));
      }
    }

    if (BeeUtils.isEmpty(renderColumns.get()) && renderSource()) {
      if (!getChoiceColumns().isEmpty()) {
        renderColumns.set(getChoiceColumns());
      } else if (!getSearchableColumns().isEmpty()) {
        renderColumns.set(getSearchableColumns());
      } else if (sourceInfo != null) {
        renderColumns.set(getDefaultColumnNames(sourceInfo));
      }
    }

    if (getChoiceColumns().isEmpty() && getSearchableColumns().isEmpty()) {
      List<String> colNames = Lists.newArrayList();

      if (!BeeUtils.isEmpty(renderColumns.get())) {
        if (sourceInfo != null && targetInfo != null && renderTarget()) {
          List<String> fields = Lists.newArrayList();
          for (String columnId : renderColumns.get()) {
            ViewColumn vc = targetInfo.getViewColumn(columnId);

            if (vc != null) {
              if (sourceInfo == null) {
                fields.add(vc.getField());
              } else {
                int index = sourceInfo.getColumnIndexBySource(vc.getTable(), vc.getField(),
                    vc.getLevel() - 1);
                if (!BeeConst.isUndef(index)) {
                  fields.add(sourceInfo.getColumnId(index));
                }
              }
            }
          }
          if (!fields.isEmpty()) {
            colNames.addAll(sourceInfo.parseColumns(fields));
          }
        } else if (renderSource()) {
          colNames.addAll(renderColumns.get());
        }
      }

      if (sourceInfo != null && colNames.isEmpty()) {
        colNames.addAll(getDefaultColumnNames(sourceInfo));
      }

      if (!colNames.isEmpty()) {
        setChoiceColumns(colNames);
        setSearchableColumns(colNames);
      }

    } else if (getChoiceColumns().isEmpty()) {
      setChoiceColumns(getSearchableColumns());
    } else if (getSearchableColumns().isEmpty()) {
      setSearchableColumns(getChoiceColumns());
    }
  }

  public boolean isEditEnabled() {
    return BeeConst.isTrue(getAttribute(UiConstants.ATTR_EDIT_ENABLED));
  }

  public Boolean isEditModal() {
    return BeeUtils.toBooleanOrNull(getAttribute(UiConstants.ATTR_EDIT_POPUP));
  }
  
  public boolean isNewRowEnabled() {
    return !BeeConst.isFalse(getAttribute(UiConstants.ATTR_NEW_ROW_ENABLED));
  }

  public boolean renderSource() {
    return RenderMode.SOURCE.equals(getRenderMode());
  }

  public boolean renderTarget() {
    return RenderMode.TARGET.equals(getRenderMode());
  }

  @Override
  public String serialize() {
    Serial[] members = Serial.values();
    Object[] arr = new Object[members.length];
    int i = 0;

    for (Serial member : members) {
      switch (member) {
        case ATTRIBUTES:
          arr[i++] = getAttributes();
          break;
        case SELECTOR_COLUMNS:
          arr[i++] = getSelectorColumns();
          break;
        case ROW_RENDERER_DESCR:
          arr[i++] = getRowRendererDescription();
          break;
        case ROW_RENDER:
          arr[i++] = getRowRender();
          break;
        case ROW_RENDER_TOKENS:
          arr[i++] = getRowRenderTokens();
          break;
      }
    }
    return Codec.beeSerialize(arr);
  }

  public void setAttributes(Map<String, String> attributes) {
    getAttributes().clear();
    if (BeeUtils.isEmpty(attributes)) {
      return;
    }
    getAttributes().putAll(attributes);
  }

  public void setCaching(Caching caching) {
    this.caching = caching;
  }

  public void setChoiceColumns(List<String> choiceColumns) {
    getChoiceColumns().clear();
    if (!BeeUtils.isEmpty(choiceColumns)) {
      getChoiceColumns().addAll(choiceColumns);
    }
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  public void setItemKey(String itemKey) {
    this.itemKey = itemKey;
  }

  public void setItemType(MenuConstants.ITEM_TYPE itemType) {
    this.itemType = itemType;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public void setRowRender(Calculation rowRender) {
    this.rowRender = rowRender;
  }

  public void setRowRendererDescription(RendererDescription rowRendererDescription) {
    this.rowRendererDescription = rowRendererDescription;
  }

  public void setRowRenderTokens(List<RenderableToken> rowRenderTokens) {
    this.rowRenderTokens = rowRenderTokens;
  }

  public void setSearchableColumns(List<String> searchableColumns) {
    getSearchableColumns().clear();
    if (!BeeUtils.isEmpty(searchableColumns)) {
      getSearchableColumns().addAll(searchableColumns);
    }
  }

  public void setSelectorColumns(List<SelectorColumn> selectorColumns) {
    getSelectorColumns().clear();
    if (selectorColumns != null) {
      getSelectorColumns().addAll(selectorColumns);
    }
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  public void setVisibleLines(Integer visibleLines) {
    this.visibleLines = visibleLines;
  }

  private String deduceViewName(DataInfo dataInfo, List<String> columns) {
    if (!BeeUtils.isEmpty(columns)) {
      for (String colName : columns) {
        String result = deduceViewName(dataInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          return result;
        }
      }
    }
    return null;
  }

  private String deduceViewName(DataInfo dataInfo, String colName) {
    return dataInfo.getRelationView(colName);
  }

  private String deduceViewName(DataInfo.Provider provider, DataInfo targetInfo,
      String target, List<String> renderColumns, List<String> displCols, List<String> searchCols,
      List<String> selectorColumnNames) {
    String result = null;

    if (BeeUtils.isEmpty(target) && BeeUtils.isEmpty(renderColumns) && BeeUtils.isEmpty(displCols)
        && BeeUtils.isEmpty(searchCols) && BeeUtils.isEmpty(selectorColumnNames)) {
      List<String> columnNames = targetInfo.getColumnNames(false);
      for (String colName : columnNames) {
        result = deduceViewName(targetInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          break;
        }
      }
      return result;
    }

    if (!BeeUtils.isEmpty(target)) {
      result = deduceViewName(targetInfo, target);
      return result;
    }

    result = deduceViewName(targetInfo, renderColumns);
    if (!BeeUtils.isEmpty(result)) {
      return result;
    }

    Set<String> columns = BeeUtils.union(displCols, searchCols, selectorColumnNames);
    if (columns.isEmpty()) {
      return result;
    }

    List<String> tables = targetInfo.getRelatedTables();
    if (tables.isEmpty()) {
      return result;
    }

    for (String table : tables) {
      DataInfo tableInfo = provider.getDataInfo(table, false);
      if (tableInfo != null && tableInfo.containsAllViewColumns(columns)) {
        result = table;
        break;
      }
    }
    return result;
  }

  private List<String> deriveRenderColumns(DataInfo targetInfo, String original, String resolved) {
    if (targetInfo.containsColumn(original)) {
      ViewColumn vc = targetInfo.getViewColumn(original);
      if (vc != null && vc.getLevel() > 0) {
        return Lists.newArrayList(original);
      }
    }
    
    return RelationUtils.getRenderColumns(targetInfo, resolved);
  }

  private String getAttribute(String name) {
    return getAttributes().get(name);
  }

  private List<String> getDefaultColumnNames(DataInfo dataInfo) {
    List<String> result = Lists.newArrayList();

    for (BeeColumn column : dataInfo.getColumns()) {
      if (ValueType.TEXT.equals(column.getType()) && !column.isText()) {
        result.add(column.getId());
      }
    }

    return result.isEmpty() ? DataUtils.getColumnNames(dataInfo.getColumns()) : result;
  }

  private RenderMode getRenderMode() {
    return renderMode;
  }

  private List<String> getSelectorColumnNames() {
    List<String> result = Lists.newArrayList();
    for (SelectorColumn selectorColumn : getSelectorColumns()) {
      BeeUtils.addNotEmpty(result, selectorColumn.getSource());
    }
    return result;
  }

  private String resolveTarget(DataInfo dataInfo, String colName) {
    return dataInfo.getRelationSource(colName);
  }

  private String resolveTarget(DataInfo targetInfo, String target, List<String> renderColumns) {
    String result = null;

    if (!BeeUtils.isEmpty(target)) {
      result = resolveTarget(targetInfo, target);
      if (!BeeUtils.isEmpty(result)) {
        return result;
      }
    }

    if (!BeeUtils.isEmpty(renderColumns)) {
      for (String colName : renderColumns) {
        result = resolveTarget(targetInfo, colName);
        if (!BeeUtils.isEmpty(result)) {
          break;
        }
      }
    }
    return result;
  }

  private void setOriginalRenderColumns(List<String> originalRenderColumns) {
    if (!this.originalRenderColumns.isEmpty()) {
      this.originalRenderColumns.clear();
    }
    if (!BeeUtils.isEmpty(originalRenderColumns)) {
      this.originalRenderColumns.addAll(originalRenderColumns);
    }
  }

  private void setOriginalTarget(String originalTarget) {
    this.originalTarget = originalTarget;
  }

  private void setRenderMode(RenderMode renderMode) {
    this.renderMode = renderMode;
  }
}
