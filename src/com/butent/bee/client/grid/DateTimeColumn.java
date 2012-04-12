package com.butent.bee.client.grid;

import com.google.gwt.cell.client.Cell;

import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.i18n.HasDateTimeFormat;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.time.DateTime;

/**
 * Implements DateTime type column, enables to get value for a specified row or index point.
 */

public class DateTimeColumn extends DataColumn<DateTime> implements HasDateTimeFormat {

  public DateTimeColumn(int index, IsColumn dataColumn) {
    this(Format.getDefaultDateTimeFormat(), index, dataColumn);
  }

  public DateTimeColumn(DateTimeFormat format, int index, IsColumn dataColumn) {
    this(new DateTimeCell(format), index, dataColumn);
  }

  public DateTimeColumn(Cell<DateTime> cell, int index, IsColumn dataColumn) {
    super(cell, index, dataColumn);
  }

  public DateTimeFormat getDateTimeFormat() {
    if (getCell() instanceof HasDateTimeFormat) {
      return ((HasDateTimeFormat) getCell()).getDateTimeFormat();
    } else {
      return null;
    }
  }
  
  @Override
  public DateTime getValue(IsRow row) {
    if (row == null) {
      return null;
    }
    return row.getDateTime(getIndex());
  }

  public void setDateTimeFormat(DateTimeFormat format) {
    if (getCell() instanceof HasDateTimeFormat) {
      ((HasDateTimeFormat) getCell()).setDateTimeFormat(format);
    }
  }
}
