package com.butent.bee.client.modules.calendar.layout;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.google.common.collect.Table;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.AppointmentUtils;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class CalendarLayoutManager {
  
  private static final int MULTI_ROW_HEIGHT = 17;
  private static final int MULTI_MARGIN_TOP = 4;
  private static final int MULTI_MARGIN_BOTTOM = 4;

  private static final double MULTI_MARGIN_LEFT = 0.5;
  private static final double MULTI_MARGIN_RIGHT = 0.5;

  private static final int MULTI_PERCENT_SCALE = 3;
  
  public static void addColumnSeparators(HasWidgets container, int columnCount) {
    if (columnCount <= 0) {
      return;
    }

    int width = 100 / columnCount;
    for (int i = 0; i < columnCount; i++) {
      Html separator = new Html();

      separator.setStyleName(CalendarStyleManager.COLUMN_SEPARATOR);
      StyleUtils.setLeft(separator, width * i, Unit.PCT);

      container.add(separator);
    }
  }

  public static List<AppointmentAdapter> doLayout(List<Appointment> appointments, int columnIndex,
      int columnCount, CalendarSettings settings) {

    int intervalsPerHour = settings.getIntervalsPerHour();
    int intervalSize = settings.getPixelsPerInterval();

    int minutesPerInterval = TimeUtils.MINUTES_PER_HOUR / intervalsPerHour;
    int numberOfBlocks = TimeUtils.MINUTES_PER_HOUR / minutesPerInterval * TimeUtils.HOURS_PER_DAY;

    TimeBlock[] timeBlocks = new TimeBlock[numberOfBlocks];

    for (int i = 0; i < numberOfBlocks; i++) {
      TimeBlock timeBlock = new TimeBlock();
      timeBlock.setOrder(i);

      timeBlock.setStart(i * minutesPerInterval);
      timeBlock.setEnd(timeBlock.getStart() + minutesPerInterval);

      timeBlock.setTop(i * intervalSize);
      timeBlock.setBottom(timeBlock.getTop() + intervalSize);

      timeBlocks[i] = timeBlock;
    }

    List<AppointmentAdapter> appointmentCells = Lists.newArrayList();

    int groupMaxColumn = 0;
    int groupStartIndex = -1;
    int groupEndIndex = -2;

    for (Appointment appointment : appointments) {
      TimeBlock startBlock = null;
      TimeBlock endBlock = null;

      AppointmentAdapter apptCell = new AppointmentAdapter(appointment);
      appointmentCells.add(apptCell);

      for (TimeBlock block : timeBlocks) {
        if (block.intersectsWith(apptCell)) {
          startBlock = block;

          if (groupEndIndex < startBlock.getOrder()) {
            for (int i = groupStartIndex; i <= groupEndIndex; i++) {
              TimeBlock tb = timeBlocks[i];
              tb.setTotalColumns(groupMaxColumn + 1);
            }
            groupStartIndex = startBlock.getOrder();
            groupMaxColumn = 0;
          }
          break;
        }
      }

      startBlock.getAppointments().add(apptCell);
      apptCell.getIntersectingBlocks().add(startBlock);

      int column = startBlock.getFirstAvailableColumn();
      apptCell.setColumnStart(column);
      apptCell.setColumnSpan(1);

      startBlock.getOccupiedColumns().put(column, column);

      apptCell.setCellStart(startBlock.getOrder());

      for (int i = startBlock.getOrder() + 1; i < timeBlocks.length; i++) {
        TimeBlock nextBlock = timeBlocks[i];

        if (nextBlock.intersectsWith(apptCell)) {
          nextBlock.getAppointments().add(apptCell);
          nextBlock.getOccupiedColumns().put(column, column);
          endBlock = nextBlock;

          apptCell.getIntersectingBlocks().add(nextBlock);
        }
      }

      endBlock = (endBlock == null) ? startBlock : endBlock;
      if (column > groupMaxColumn) {
        groupMaxColumn = column;
      }

      if (groupEndIndex < endBlock.getOrder()) {
        groupEndIndex = endBlock.getOrder();
      }

      apptCell.setCellSpan(endBlock.getOrder() - startBlock.getOrder() + 1);
    }

    for (int i = groupStartIndex; i <= groupEndIndex; i++) {
      TimeBlock tb = timeBlocks[i];
      tb.setTotalColumns(groupMaxColumn + 1);
    }

    double dayWidth = 100 / columnCount;
    double widthAdj = dayWidth / 100;

    double paddingLeft = 0.5d;
    double paddingRight = 0.5d;
    double paddingBottom = 2;

    for (AppointmentAdapter apptCell : appointmentCells) {
      int totalColumns = apptCell.getIntersectingBlocks().get(0).getTotalColumns();
      double width = 1d / totalColumns * 100;
      double left = (double) apptCell.getColumnStart() / totalColumns * 100;

      apptCell.setLeft(widthAdj * 100 * columnIndex + left * widthAdj + paddingLeft);
      apptCell.setWidth(width * widthAdj - paddingLeft - paddingRight);

      apptCell.setTop(apptCell.getCellStart() * intervalSize);
      apptCell.setHeight(apptCell.getIntersectingBlocks().size() * intervalSize - paddingBottom);

      double apptStart = apptCell.getAppointmentStart();
      double apptEnd = apptCell.getAppointmentEnd();
      double apptDuration = apptEnd - apptStart;

      double blockStart = timeBlocks[apptCell.getCellStart()].getStart();
      double blockEnd = timeBlocks[apptCell.getCellStart() + apptCell.getCellSpan() - 1].getEnd();
      double blockDuration = blockEnd - blockStart;

      double timeFillHeight = apptDuration / blockDuration * 100d;
      double timeFillStart = (apptStart - blockStart) / blockDuration * 100d;

      apptCell.setCellPercentFill(timeFillHeight);
      apptCell.setCellPercentStart(timeFillStart);
    }
    return appointmentCells;
  }

  public static int doMultiLayout(List<AppointmentAdapter> adapters, JustDate date, int days) {
    return doMultiLayout(adapters, date, days, 0, days);
  }

  public static int doMultiLayout(List<AppointmentAdapter> adapters, JustDate date,
      int columnIndex, int columnCount) {
    return doMultiLayout(adapters, date, 1, columnIndex, columnCount);
  }
  
  private static int doMultiLayout(List<AppointmentAdapter> adapters, JustDate date, int days,
      int columnIndex, int columnCount) {
    if (adapters.isEmpty()) {
      return 0;
    }

    Table<Integer, Integer, Range<DateTime>> slots = HashBasedTable.create();
    
    List<Range<DateTime>> dateRanges = Lists.newArrayList(); 
    for (int i = 0; i < days; i++) {
      dateRanges.add(Ranges.closedOpen(TimeUtils.startOfDay(date, i),
          TimeUtils.startOfDay(date, i + 1)));
    }
    
    int columnWidth = 100 / columnCount;
    double minuteWidth = (double) columnWidth / TimeUtils.MINUTES_PER_DAY;    

    for (AppointmentAdapter adapter : adapters) {
      int columnStart = BeeConst.UNDEF;
      int columnSpan = 0;
      
      Range<DateTime> appointmenRange = AppointmentUtils.getRange(adapter.getAppointment());

      for (int i = 0; i < days; i++) {
        if (BeeUtils.intersects(appointmenRange, dateRanges.get(i))) {
          if (BeeConst.isUndef(columnStart)) {
            columnStart = i;
          }
          columnSpan++;
        }
      }

      if (BeeConst.isUndef(columnStart) || columnSpan <= 0) {
        BeeKeeper.getLog().warning("cannot lay out multi day appointment", date, days,
            adapter.getAppointment().getId());
        continue;
      }
      
      adapter.setColumnStart(columnStart);
      adapter.setColumnSpan(columnSpan);
      
      DateTime startDate = adapter.getAppointment().getStart();
      DateTime endDate = adapter.getAppointment().getEnd();

      for (int r = 0; r < adapters.size(); r++) {
        boolean isRowOccupied = false;

        for (int c = columnStart; c < columnStart + columnSpan; c++) {
          if (BeeUtils.intersects(slots.get(r, c), appointmenRange)) {
            isRowOccupied = true;
            break;
          }
        }

        if (!isRowOccupied) {
          for (int c = columnStart; c < columnStart + columnSpan; c++) {
            Range<DateTime> range = appointmenRange.intersection(dateRanges.get(c));

            if (slots.contains(r, c)) {
              slots.put(r, c, range.span(slots.get(r, c)));
            } else {
              slots.put(r, c, range);
            }
          }

          adapter.setCellStart(r);
          
          int minutes;
          if (TimeUtils.dayDiff(date, startDate) == columnStart) {
            minutes = TimeUtils.minutesSinceDayStarted(startDate);
          } else {
            minutes = 0;
          }

          double marginLeft;
          if (minutes > 0) {
            marginLeft = minutes * minuteWidth;
          } else {
            marginLeft = MULTI_MARGIN_LEFT;
          }
          
          if (TimeUtils.dayDiff(date, endDate) == columnStart + columnSpan - 1) {
            minutes = TimeUtils.minutesSinceDayStarted(endDate);
          } else {
            minutes = 0;
          }

          double marginRight;
          if (minutes > 0) {
            marginRight = (TimeUtils.MINUTES_PER_DAY - minutes) * minuteWidth;
          } else {
            marginRight = MULTI_MARGIN_RIGHT;
          }
          
          double left = BeeUtils.round((columnStart + columnIndex) * columnWidth + marginLeft,
              MULTI_PERCENT_SCALE);
          double width = BeeUtils.round(columnSpan * columnWidth - marginLeft - marginRight,
              MULTI_PERCENT_SCALE);

          double top = r * (MULTI_ROW_HEIGHT + MULTI_MARGIN_TOP) + MULTI_MARGIN_TOP;

          adapter.setWidth(width);
          adapter.setLeft(left);
          adapter.setTop(top);
          adapter.setHeight(MULTI_ROW_HEIGHT);

          break;
        }
      }
    }
    
    return slots.rowKeySet().size() * (MULTI_ROW_HEIGHT + MULTI_MARGIN_TOP) + MULTI_MARGIN_BOTTOM;
  }
  
  private CalendarLayoutManager() {
  }
}
