package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.modules.calendar.event.HasTimeBlockClickHandlers;
import com.butent.bee.client.modules.calendar.event.HasUpdateHandlers;
import com.butent.bee.client.modules.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.modules.calendar.event.UpdateEvent;
import com.butent.bee.client.modules.calendar.view.DayView;
import com.butent.bee.client.modules.calendar.view.MonthView;
import com.butent.bee.client.modules.calendar.view.ResourceView;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CalendarWidget extends Composite implements HasOpenHandlers<Appointment>,
    HasTimeBlockClickHandlers, HasUpdateHandlers, RequiresResize, ProvidesResize {

  private final FlowPanel rootPanel = new FlowPanel();

  private final long calendarId;

  private final JustDate date;

  private final CalendarSettings settings;

  private final AppointmentManager appointmentManager;
  private final List<Long> attendees = Lists.newArrayList();

  private final Map<CalendarView.Type, CalendarView> viewCache = Maps.newHashMap();

  private final Timer resizeTimer = new Timer() {
    @Override
    public void run() {
      doSizing();
      doLayout();
    }
  };

  private CalendarView view = null;
  private int displayedDays = BeeConst.UNDEF;

  private boolean layoutSuspended = false;
  private boolean layoutPending = false;
  private boolean scrollPending = false;

  public CalendarWidget(long calendarId, CalendarSettings settings) {
    this(calendarId, TimeUtils.today(), settings);
  }

  public CalendarWidget(long calendarId, JustDate date, CalendarSettings settings) {
    super();

    this.calendarId = calendarId;
    this.settings = settings;
    this.appointmentManager = new AppointmentManager();
    this.date = date;

    initWidget(rootPanel);

    sinkEvents(Event.ONMOUSEDOWN | Event.ONDBLCLICK);
  }

  public void addAppointment(Appointment appointment, boolean refresh) {
    Assert.notNull(appointment, "Added appointment cannot be null.");
    appointmentManager.addAppointment(appointment);
    if (refresh) {
      refresh(false);
    }
  }

  @Override
  public HandlerRegistration addOpenHandler(OpenHandler<Appointment> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public HandlerRegistration addSelectionHandler(SelectionHandler<Appointment> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }

  @Override
  public HandlerRegistration addTimeBlockClickHandler(TimeBlockClickEvent.Handler handler) {
    return addHandler(handler, TimeBlockClickEvent.getType());
  }

  public void addToRootPanel(Widget widget) {
    rootPanel.add(widget);
  }

  @Override
  public HandlerRegistration addUpdateHandler(UpdateEvent.Handler handler) {
    return addHandler(handler, UpdateEvent.getType());
  }

  public void clearAppointments() {
    appointmentManager.clearAppointments();
    refresh(true);
  }

  public void doLayout() {
    if (view != null) {
      view.doLayout(calendarId);
    }
  }

  public void doScroll() {
    if (view != null) {
      view.doScroll();
    }
  }

  public void doSizing() {
    if (view != null) {
      view.doSizing();
    }
  }

  public List<Appointment> getAppointments() {
    return appointmentManager.getAppointments();
  }

  public List<Long> getAttendees() {
    return attendees;
  }

  public JustDate getDate() {
    return JustDate.copyOf(date);
  }

  public int getDisplayedDays() {
    return displayedDays;
  }

  public String getId() {
    return getElement().getId();
  }

  public CalendarSettings getSettings() {
    return settings;
  }

  public CalendarView.Type getType() {
    if (getView() == null) {
      return null;
    } else {
      return getView().getType();
    }
  }

  public CalendarView getView() {
    return view;
  }

  @Override
  public void onBrowserEvent(Event event) {
    int eventType = event.getTypeInt();

    switch (eventType) {
      case Event.ONDBLCLICK: {
        if (onDoubleClick(EventUtils.getEventTargetElement(event), event)) {
          event.stopPropagation();
          return;
        }
        break;
      }

      case Event.ONMOUSEDOWN: {
        if (event.getButton() == NativeEvent.BUTTON_LEFT
            && EventUtils.isCurrentTarget(event, getElement())) {
          if (onMouseDown(EventUtils.getEventTargetElement(event), event)) {
            event.stopPropagation();
            event.preventDefault();
            return;
          }
        }
        break;
      }
    }

    super.onBrowserEvent(event);
  }

  public void onClock() {
    if (view != null) {
      view.onClock();
    }
  }

  public boolean onDoubleClick(Element element, Event event) {
    if (view != null && settings.isDoubleClick()) {
      return view.onClick(calendarId, element, event);
    } else {
      return false;
    }
  }

  public boolean onMouseDown(Element element, Event event) {
    if (view != null && settings.isSingleClick()) {
      return view.onClick(calendarId, element, event);
    } else {
      return false;
    }
  }

  @Override
  public void onResize() {
    resizeTimer.schedule(100);
  }

  public void refresh(boolean scroll) {
    if (layoutSuspended) {
      layoutPending = true;
      scrollPending = scroll;
      return;
    }

    appointmentManager.sortAppointments();

    doLayout();
    doSizing();

    if (scroll) {
      doScroll();
    }
  }

  public boolean removeAppointment(long id, boolean refresh) {
    boolean removed = appointmentManager.removeAppointment(id);
    if (removed && refresh) {
      refresh(false);
    }
    return removed;
  }

  public void resumeLayout() {
    layoutSuspended = false;
    if (layoutPending) {
      refresh(scrollPending);
    }
  }

  public void setAppointments(Collection<Appointment> appointments) {
    appointmentManager.clearAppointments();
    appointmentManager.addAppointments(appointments);
    refresh(true);
  }

  public void setAttendees(Collection<Long> attendees, boolean refresh) {
    this.attendees.clear();
    this.attendees.addAll(attendees);

    if (refresh) {
      refresh(true);
    }
  }

  public void setDate(JustDate newDate) {
    setDate(newDate, getDisplayedDays());
  }

  public void setDate(JustDate newDate, int days) {
    Assert.notNull(newDate);

    if (newDate.equals(date) && days == getDisplayedDays()) {
      return;
    }

    date.setDate(newDate);
    setDisplayedDays(days);

    refresh(true);
  }

  public void setDays(int days) {
    Assert.isPositive(days);

    if (getDisplayedDays() != days) {
      setDisplayedDays(days);
      refresh(true);
    }
  }

  public void setDisplayedDays(int displayedDays) {
    this.displayedDays = displayedDays;
  }

  public void setType(CalendarView.Type viewType) {
    setType(viewType, getDisplayedDays());
  }

  public void setType(CalendarView.Type viewType, int days) {
    Assert.notNull(viewType);
    CalendarView cached = viewCache.get(viewType);

    switch (viewType) {
      case DAY:
        DayView dayView = (cached instanceof DayView) ? (DayView) cached : new DayView();
        if (!(cached instanceof DayView)) {
          viewCache.put(viewType, dayView);
        }

        if (days > 0) {
          setDisplayedDays(days);
        }
        setView(dayView);
        break;

      case MONTH:
        MonthView monthView = (cached instanceof MonthView) ? (MonthView) cached : new MonthView();
        if (!(cached instanceof MonthView)) {
          viewCache.put(viewType, monthView);
        }

        setView(monthView);
        break;

      case RESOURCE:
        ResourceView resourceView =
            (cached instanceof ResourceView) ? (ResourceView) cached : new ResourceView();
        if (!(cached instanceof ResourceView)) {
          viewCache.put(viewType, resourceView);
        }

        if (days > 0) {
          setDisplayedDays(days);
        }
        setView(resourceView);
        break;
    }
  }

  public void setView(CalendarView view) {
    Assert.notNull(view);

    rootPanel.clear();

    this.view = view;
    this.view.attach(this);

    setStyleName(this.view.getStyleName());
    refresh(true);
  }

  public void suspendLayout() {
    layoutSuspended = true;
  }
}