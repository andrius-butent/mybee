package com.butent.bee.shared.modules.calendar;

import com.google.common.collect.Maps;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.modules.calendar.CalendarConstants.TimeBlockClick;
import com.butent.bee.shared.modules.calendar.CalendarConstants.View;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumMap;
import java.util.List;

public class CalendarSettings {

  public static CalendarSettings create(IsRow row, List<? extends IsColumn> columns) {
    CalendarSettings settings = new CalendarSettings();
    if (row != null && !BeeUtils.isEmpty(columns)) {
      settings.loadFrom(row, columns);
    }
    return settings;
  }
  
  private long id;

  private int pixelsPerInterval;
  private int intervalsPerHour;

  private int workingHourStart;
  private int workingHourEnd;
  private int scrollToHour;

  private int defaultDisplayedDays;

  private TimeBlockClick timeBlockClickNumber;
  
  private boolean separateAttendees;
  
  private final EnumMap<View, Boolean> views;
  
  private View activeView;

  private CalendarSettings() {
    this.views = Maps.newEnumMap(View.class);
    
    for (View view : View.values()) {
      this.views.put(view, true);
    }
  }

  public View getActiveView() {
    return activeView;
  }
  
  public int getDefaultDisplayedDays() {
    return defaultDisplayedDays;
  }

  public int getHourHeight() {
    return getIntervalsPerHour() * getPixelsPerInterval(); 
  }

  public long getId() {
    return id;
  }

  public int getIntervalsPerHour() {
    return intervalsPerHour;
  }

  public int getPixelsPerInterval() {
    return pixelsPerInterval;
  }

  public int getScrollToHour() {
    return scrollToHour;
  }

  public TimeBlockClick getTimeBlockClickNumber() {
    return timeBlockClickNumber;
  }

  public int getWorkingHourEnd() {
    return workingHourEnd;
  }
  
  public int getWorkingHourStart() {
    return workingHourStart;
  }
  
  public boolean isAnyVisible() {
    for (View view : View.values()) {
      if (isVisible(view)) {
        return true;
      }
    }
    return false;
  }

  public boolean isDoubleClick() {
    return TimeBlockClick.DOUBLE.equals(getTimeBlockClickNumber());
  }

  public boolean isSingleClick() {
    return TimeBlockClick.SINGLE.equals(getTimeBlockClickNumber());
  }

  public boolean isVisible(View view) {
    return BeeUtils.isTrue(views.get(view));
  }

  public void loadFrom(IsRow row, List<? extends IsColumn> columns) {
    setId(row.getId());

    setPixelsPerInterval(getInt(row, columns, CalendarConstants.COL_PIXELS_PER_INTERVAL));
    setIntervalsPerHour(getInt(row, columns, CalendarConstants.COL_INTERVALS_PER_HOUR));

    setWorkingHourStart(getInt(row, columns, CalendarConstants.COL_WORKING_HOUR_START));
    setWorkingHourEnd(getInt(row, columns, CalendarConstants.COL_WORKING_HOUR_END));
    setScrollToHour(getInt(row, columns, CalendarConstants.COL_SCROLL_TO_HOUR));

    setDefaultDisplayedDays(getInt(row, columns, CalendarConstants.COL_DEFAULT_DISPLAYED_DAYS));

    int tbcn = getInt(row, columns, CalendarConstants.COL_TIME_BLOCK_CLICK_NUMBER);
    setTimeBlockClickNumber(BeeUtils.getConstant(TimeBlockClick.class, tbcn));
    
    setSeparateAttendees(getBool(row, columns, CalendarConstants.COL_SEPARATE_ATTENDEES));
    
    for (View view : View.values()) {
      views.put(view, getBool(row, columns, view.getColumnId()));
    }
    
    int av;
    if (DataUtils.contains(columns, CalendarConstants.COL_ACTIVE_VIEW)) {
      av = getInt(row, columns, CalendarConstants.COL_ACTIVE_VIEW);
    } else {
      av = BeeConst.UNDEF;
    }
    setActiveView(BeeUtils.getConstant(View.class, av));
  }

  public boolean separateAttendees() {
    return separateAttendees;
  }

  public void setActiveView(View activeView) {
    this.activeView = activeView;
  }

  public void setDefaultDisplayedDays(int defaultDisplayedDays) {
    this.defaultDisplayedDays = defaultDisplayedDays;
  }

  private boolean getBool(IsRow row, List<? extends IsColumn> columns, String columnId) {
    Boolean value = row.getBoolean(DataUtils.getColumnIndex(columnId, columns));
    return (value == null) ? false : value;
  }

  private int getInt(IsRow row, List<? extends IsColumn> columns, String columnId) {
    Integer value = row.getInteger(DataUtils.getColumnIndex(columnId, columns));
    return (value == null) ? BeeConst.UNDEF : value;
  }

  private void setId(long id) {
    this.id = id;
  }

  private void setIntervalsPerHour(int intervals) {
    intervalsPerHour = intervals;
  }

  private void setPixelsPerInterval(int px) {
    pixelsPerInterval = px;
  }
  
  private void setScrollToHour(int hour) {
    scrollToHour = hour;
  }

  private void setSeparateAttendees(boolean separateAttendees) {
    this.separateAttendees = separateAttendees;
  }

  private void setTimeBlockClickNumber(TimeBlockClick timeBlockClickNumber) {
    this.timeBlockClickNumber = timeBlockClickNumber;
  }

  private void setWorkingHourEnd(int end) {
    workingHourEnd = end;
  }

  private void setWorkingHourStart(int start) {
    workingHourStart = start;
  }
}
