package com.butent.bee.client.modules.calendar.view;

import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.modules.calendar.Appointment;
import com.butent.bee.client.modules.calendar.CalendarKeeper;
import com.butent.bee.client.modules.calendar.CalendarUtils;
import com.butent.bee.client.modules.calendar.AppointmentWidget;
import com.butent.bee.client.modules.calendar.CalendarStyleManager;
import com.butent.bee.client.modules.calendar.CalendarView;
import com.butent.bee.client.modules.calendar.CalendarWidget;
import com.butent.bee.client.modules.calendar.dnd.DayDropController;
import com.butent.bee.client.modules.calendar.dnd.DayDragController;
import com.butent.bee.client.modules.calendar.dnd.ResizeController;
import com.butent.bee.client.modules.calendar.layout.AppointmentAdapter;
import com.butent.bee.client.modules.calendar.layout.CalendarLayoutManager;
import com.butent.bee.client.modules.calendar.layout.AppointmentPanel;
import com.butent.bee.client.modules.calendar.layout.MultiDayPanel;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

import java.util.List;
import java.util.Map;

public class DayView extends CalendarView {

  private final DayViewHeader dayViewHeader = new DayViewHeader();
  private final MultiDayPanel multiDayPanel = new MultiDayPanel();
  private final AppointmentPanel appointmentPanel = new AppointmentPanel();

  private final List<AppointmentWidget> appointmentWidgets = Lists.newArrayList();

  private DayDragController dragController = null;
  private DayDropController dropController = null;

  private ResizeController resizeController = null;

  public DayView() {
    super();
  }

  @Override
  public void attach(CalendarWidget widget) {
    super.attach(widget);

    addWidget(dayViewHeader);
    addWidget(multiDayPanel);
    addWidget(appointmentPanel);

    createDragController();
    createDropController();
    createResizeController();
  }

  @Override
  public void doLayout(long calendarId) {
    JustDate date = getDate();
    int days = getDisplayedDays();

    List<Long> attendees = getCalendarWidget().getAttendees();

    dayViewHeader.setDays(date, days);
    dayViewHeader.setYear(date);

    multiDayPanel.setColumnCount(days);

    int todayColumn = CalendarUtils.getTodayColumn(date, days);
    appointmentPanel.build(days, getSettings(), todayColumn, todayColumn);

    dragController.setDate(JustDate.copyOf(date));

    dropController.setColumns(days);
    dropController.setSettings(getSettings());

    resizeController.setSettings(getSettings());

    appointmentWidgets.clear();
    
    boolean separate = getSettings().separateAttendees();
    Map<Long, String> attColors = CalendarKeeper.getAttendeeColors(calendarId);

    JustDate tmpDate = JustDate.copyOf(date);

    for (int i = 0; i < days; i++) {
      List<Appointment> simple = CalendarUtils.filterSimple(getAppointments(), tmpDate,
          attendees, separate);

      if (!simple.isEmpty()) {
        List<AppointmentAdapter> adapters =
            CalendarLayoutManager.doLayout(simple, i, days, getSettings());
        addAppointmentsToGrid(calendarId, adapters, false, i, separate, attColors);
      }

      TimeUtils.moveOneDayForward(tmpDate);
    }

    List<Appointment> multi = CalendarUtils.filterMulti(getAppointments(), date, days,
        attendees, separate);
    if (!multi.isEmpty()) {
      List<AppointmentAdapter> adapters = Lists.newArrayList();
      for (Appointment appointment : multi) {
        adapters.add(new AppointmentAdapter(appointment));
      }

      int desiredHeight = CalendarLayoutManager.doMultiLayout(adapters, date, days);
      StyleUtils.setHeight(multiDayPanel.getGrid(), desiredHeight);

      addAppointmentsToGrid(calendarId, adapters, true, BeeConst.UNDEF, separate, attColors);
    } else {
      StyleUtils.clearHeight(multiDayPanel.getGrid());
    }
  }

  @Override
  public void doScroll() {
    appointmentPanel.doScroll(getSettings(), appointmentWidgets);
  }

  @Override
  public void doSizing() {
    if (getCalendarWidget().getOffsetHeight() > 0) {
      StyleUtils.setHeight(appointmentPanel, getCalendarWidget().getOffsetHeight()
          - 2 - dayViewHeader.getOffsetHeight() - multiDayPanel.getOffsetHeight());
    }
  }

  @Override
  public List<AppointmentWidget> getAppointmentWidgets() {
    return appointmentWidgets;
  }

  @Override
  public Widget getScrollArea() {
    return appointmentPanel.getScrollArea();
  }

  @Override
  public String getStyleName() {
    return CalendarStyleManager.DAY_VIEW;
  }

  @Override
  public Type getType() {
    return Type.DAY;
  }

  @Override
  public boolean onClick(long calendarId, Element element, Event event) {
    AppointmentWidget widget = CalendarUtils.findWidget(appointmentWidgets, element);

    if (widget != null) {
      if (widget.canClick(element)) {
        openAppointment(widget.getAppointment());
        return true;
      } else {
        return false;
      }

    } else if (appointmentPanel.isGrid(element)) {
      timeBlockClick(event);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void onClock() {
    appointmentPanel.onClock(getSettings());
  }

  private void addAppointmentsToGrid(long calendarId, List<AppointmentAdapter> adapters,
      boolean multi, int columnIndex, boolean separate, Map<Long, String> attColors) {

    for (AppointmentAdapter adapter : adapters) {
      AppointmentWidget widget = new AppointmentWidget(adapter.getAppointment(), multi,
          columnIndex);

      widget.setLeft(adapter.getLeft());
      widget.setWidth(adapter.getWidth());

      widget.setTop(adapter.getTop());
      widget.setHeight(adapter.getHeight());
      
      String bg = (separate && attColors != null) 
          ?  attColors.get(adapter.getAppointment().getSeparatedAttendee()) : null;

      widget.render(calendarId, bg);

      appointmentWidgets.add(widget);

      if (multi) {
        multiDayPanel.getGrid().add(widget);
      } else {
        appointmentPanel.getGrid().add(widget);

        resizeController.makeDraggable(widget.getResizeHandle());
        dragController.makeDraggable(widget, widget.getMoveHandle());
      }
    }
  }

  private void createDragController() {
    if (dragController == null) {
      dragController = new DayDragController(appointmentPanel.getGrid());
      dragController.addDefaultHandler(this);
    }
  }

  private void createDropController() {
    if (dropController == null) {
      dropController = new DayDropController(appointmentPanel.getGrid());
      dragController.registerDropController(dropController);
    }
  }

  private void createResizeController() {
    if (resizeController == null) {
      resizeController = new ResizeController(appointmentPanel.getGrid());
      resizeController.addDefaultHandler(this);
    }
  }

  private void timeBlockClick(Event event) {
    DateTime dateTime = appointmentPanel.getCoordinatesDate(event.getClientX(), event.getClientY(),
        getSettings(), getDate(), getDisplayedDays());
    createAppointment(dateTime, null);
  }
}
