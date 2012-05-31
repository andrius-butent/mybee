package com.butent.bee.client.calendar.dayview;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import com.butent.bee.client.calendar.CalendarFormat;
import com.butent.bee.client.calendar.HasSettings;
import com.butent.bee.client.dom.StyleUtils;

public class DayViewTimeline extends Composite {

  private static final String TIME_LABEL_STYLE = "hour-label";

  private final AbsolutePanel timelinePanel = new AbsolutePanel();
  private final HasSettings settings;

  public DayViewTimeline(HasSettings settings) {
    initWidget(timelinePanel);
    timelinePanel.setStylePrimaryName("time-strip");

    this.settings = settings;

    prepare();
  }

  public void prepare() {
    timelinePanel.clear();
    
    int labelHeight = settings.getSettings().getIntervalsPerHour()
        * settings.getSettings().getPixelsPerInterval();

    int i = 0;
    if (settings.getSettings().offsetHourLabels()) {
      i = 1;
      SimplePanel sp = new SimplePanel();
      StyleUtils.setHeight(sp, labelHeight / 2);
      timelinePanel.add(sp);
    }

    while (i < 24) {
      String hour = CalendarFormat.getHourLabels()[i];
      i++;

      SimplePanel hourWrapper = new SimplePanel();
      hourWrapper.setStylePrimaryName(TIME_LABEL_STYLE);
      StyleUtils.setHeight(hourWrapper, labelHeight - 1);

      FlowPanel flowPanel = new FlowPanel();
      flowPanel.setStyleName("hour-layout");

      Label hourLabel = new Label(hour);
      hourLabel.setStylePrimaryName("hour-text");
      flowPanel.add(hourLabel);
      
      hourWrapper.add(flowPanel);
      timelinePanel.add(hourWrapper);
    }
  }
}
