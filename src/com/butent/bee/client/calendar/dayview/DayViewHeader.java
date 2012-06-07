package com.butent.bee.client.calendar.dayview;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasWidgets;

import com.butent.bee.client.calendar.CalendarFormat;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

public class DayViewHeader extends Composite {

  private static final int YEAR_CELL_INDEX = 0;
  private static final int DAY_PANEL_INDEX = 1;

  private final Horizontal header = new Horizontal();

  public DayViewHeader() {
    initWidget(header);
    header.addStyleName(CalendarStyleManager.CALENDAR_HEADER);

    BeeLabel yearLabel = new BeeLabel();
    header.add(yearLabel);
    header.addStyleToCell(yearLabel, CalendarStyleManager.YEAR_CELL);
    
    Flow dayPanel = new Flow();
    dayPanel.addStyleName(CalendarStyleManager.DAY_CELL_CONTAINER);
    header.add(dayPanel);
    
    Html filler = new Html();
    header.add(filler);
    header.setCellWidth(filler, DomUtils.getScrollBarWidth());
  }

  public void setDays(JustDate date, int days) {
    HasWidgets dayPanel = (HasWidgets) header.getWidget(DAY_PANEL_INDEX); 
    dayPanel.clear();

    int dayWidth = 100 / days;
    JustDate tmp = JustDate.copyOf(date);

    for (int i = 0; i < days; i++) {
      BeeLabel dayLabel = new BeeLabel(CalendarFormat.format(tmp));
      dayLabel.addStyleName(CalendarStyleManager.DAY_CELL);

      StyleUtils.setLeft(dayLabel, dayWidth * i, Unit.PCT);
      StyleUtils.setWidth(dayLabel, dayWidth, Unit.PCT);

      if (TimeUtils.isToday(tmp)) {
        dayLabel.addStyleName(CalendarStyleManager.DAY_CELL_TODAY);
      }
      if (TimeUtils.isWeekend(tmp)) {
        dayLabel.addStyleName(CalendarStyleManager.DAY_CELL_WEEKEND);
      }

      dayPanel.add(dayLabel);
      TimeUtils.moveOneDayForward(tmp);
    }
  }

  public void setYear(JustDate date) {
    header.getWidget(YEAR_CELL_INDEX).getElement().setInnerHTML(BeeUtils.toString(date.getYear()));
  }
}
