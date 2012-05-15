package com.butent.bee.client.validation;

import com.butent.bee.client.dialog.NotificationListener;
import com.butent.bee.client.utils.Evaluator;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.value.ValueType;

public class CellValidation {

  private String oldValue;
  private String newValue;
  
  private Evaluator evaluator;
  
  private IsRow row;
  private BeeColumn column;
  private int colIndex;
  
  private ValueType type;
  private boolean nullable;
  
  private String minValue;
  private String maxValue;
  
  private String caption;
  private NotificationListener notificationListener;
  
  public CellValidation(String oldValue, String newValue, Evaluator evaluator, IsRow row,
      BeeColumn column, int colIndex, ValueType type, boolean nullable, String minValue,
      String maxValue, String caption, NotificationListener notificationListener) {
    super();
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.evaluator = evaluator;
    this.row = row;
    this.column = column;
    this.colIndex = colIndex;
    this.type = type;
    this.nullable = nullable;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.caption = caption;
    this.notificationListener = notificationListener;
  }

  public String getCaption() {
    return caption;
  }

  public int getColIndex() {
    return colIndex;
  }

  public BeeColumn getColumn() {
    return column;
  }

  public Evaluator getEvaluator() {
    return evaluator;
  }

  public String getMaxValue() {
    return maxValue;
  }

  public String getMinValue() {
    return minValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public NotificationListener getNotificationListener() {
    return notificationListener;
  }

  public String getOldValue() {
    return oldValue;
  }

  public IsRow getRow() {
    return row;
  }
  
  public ValueType getType() {
    return type;
  }
  
  public boolean hasDefaults() {
    return getColumn() != null && getColumn().hasDefaults();
  }

  public boolean isAdding() {
    return DataUtils.isNewRow(getRow());
  }

  public boolean isEditing() {
    return DataUtils.hasId(getRow());
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setColIndex(int colIndex) {
    this.colIndex = colIndex;
  }

  public void setColumn(BeeColumn column) {
    this.column = column;
  }

  public void setEvaluator(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  public void setMaxValue(String maxValue) {
    this.maxValue = maxValue;
  }

  public void setMinValue(String minValue) {
    this.minValue = minValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public void setNotificationListener(NotificationListener notificationListener) {
    this.notificationListener = notificationListener;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public void setRow(IsRow row) {
    this.row = row;
  }

  public void setType(ValueType type) {
    this.type = type;
  }
}
