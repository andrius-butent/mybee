package com.butent.bee.shared.data;

import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.value.Value;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Specifies necessary methods for data containing classes (for example views and tables).
 */

public interface IsData {
  
  Boolean getBoolean(int rowIndex, int colIndex);

  String getColumnId(int colIndex);

  int getColumnIndex(String columnId);

  String getColumnLabel(int colIndex);

  String getColumnLabel(String columnId);

  String getColumnPattern(int colIndex);

  CustomProperties getColumnProperties(int colIndex);

  String getColumnProperty(int colIndex, String name);

  Range getColumnRange(int colIndex);

  ValueType getColumnType(int colIndex);

  JustDate getDate(int rowIndex, int colIndex);

  DateTime getDateTime(int rowIndex, int colIndex);

  BigDecimal getDecimal(int rowIndex, int colIndex);

  List<Value> getDistinctValues(int colIndex);
  
  Double getDouble(int rowIndex, int colIndex);
  
  int[] getFilteredRows(RowFilter... filters);
  
  String getFormattedValue(int rowIndex, int colIndex);

  Integer getInteger(int rowIndex, int colIndex);

  Long getLong(int rowIndex, int colIndex);

  int getNumberOfColumns();

  int getNumberOfRows();

  CustomProperties getProperties(int rowIndex, int colIndex);

  String getProperty(int rowIndex, int colIndex, String name);

  CustomProperties getRowProperties(int rowIndex);

  String getRowProperty(int rowIndex, String name);

  int[] getSortedRows(List<Pair<Integer, Boolean>> sortInfo, Comparator<String> collator);

  String getString(int rowIndex, int colIndex);

  CustomProperties getTableProperties();

  String getTableProperty(String key);

  Value getValue(int rowIndex, int colIndex);
}
