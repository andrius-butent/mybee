package com.butent.bee.client.calendar;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.DateTime;

public abstract class CalendarView implements HasSettings {

  public enum Type {
    DAY, MONTH, AGENDA, RESOURCE
  }
  
  protected CalendarWidget calendarWidget = null;

  private int displayedDays = BeeConst.UNDEF;

  public void attach(CalendarWidget widget) {
    Assert.notNull(widget);
    this.calendarWidget = widget;
  }

  public void createAppointment(Appointment appt) {
    createAppointment(appt.getStart());
  }

  public void createAppointment(DateTime start) {
    Assert.notNull(calendarWidget);
    calendarWidget.fireTimeBlockClickEvent(start);
  }

  public final void deleteAppointment(Appointment appt) {
    Assert.notNull(calendarWidget);
    calendarWidget.fireDeleteEvent(appt);
  }

  public void detatch() {
    calendarWidget = null;
  }

  public abstract void doLayout();

  public void doSizing() {
  }

  public int getDisplayedDays() {
    return displayedDays;
  }

  public CalendarSettings getSettings() {
    Assert.notNull(calendarWidget);
    return calendarWidget.getSettings();
  }

  public abstract String getStyleName();

  public abstract void onAppointmentSelected(Appointment appt);

  public void onDeleteKeyPressed() {
  }

  public abstract void onDoubleClick(Element element, Event event);

  public void onDownArrowKeyPressed() {
  }

  public void onLeftArrowKeyPressed() {
  }

  public abstract void onMouseOver(Element element, Event event);

  public void onRightArrowKeyPressed() {
  }

  public abstract void onSingleClick(Element element, Event event);

  public void onUpArrowKeyPressed() {
  }

  public void openAppointment(Appointment appt) {
    Assert.notNull(calendarWidget);
    calendarWidget.fireOpenEvent(appt);
  }

  public abstract void scrollToHour(int hour);

  public void selectAppointment(Appointment appt) {
    Assert.notNull(calendarWidget);
    calendarWidget.setSelectedAppointment(appt, true);
  }

  public void selectNextAppointment() {
    Assert.notNull(calendarWidget);
    calendarWidget.selectNextAppointment();
  }

  public void selectPreviousAppointment() {
    Assert.notNull(calendarWidget);
    calendarWidget.selectPreviousAppointment();
  }

  public void setDisplayedDays(int displayedDays) {
    this.displayedDays = displayedDays;
  }

  public final void updateAppointment(Appointment toAppt) {
    Assert.notNull(calendarWidget);
    calendarWidget.fireUpdateEvent(toAppt);
  }
}