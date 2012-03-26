package com.butent.bee.client.calendar.dayview;

import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.calendar.Appointment;

import java.util.Date;

public class AppointmentWidget extends FlowPanel {

  class Div extends ComplexPanel implements HasAllMouseHandlers {

    public Div() {
      setElement(DOM.createDiv());
    }

    public void add(Widget w) {
      super.add(w, getElement());
    }

    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
      return addDomHandler(handler, MouseDownEvent.getType());
    }

    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
      return addDomHandler(handler, MouseMoveEvent.getType());
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
      return addDomHandler(handler, MouseOutEvent.getType());
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
      return addDomHandler(handler, MouseOverEvent.getType());
    }

    public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
      return addDomHandler(handler, MouseUpEvent.getType());
    }

    public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
      return addDomHandler(handler, MouseWheelEvent.getType());
    }
  }

  private String title;
  private String description;
  private Date start;
  private Date end;
  private boolean selected;
  private double top;
  private double left;
  private double width;
  private double height;

  private Widget headerPanel = new Div();
  private Panel bodyPanel = new SimplePanel();
  private Widget footerPanel = new Div();
  private Panel timelinePanel = new SimplePanel();
  private Panel timelineFillPanel = new SimplePanel();
  private boolean multiDay = false;
  private Appointment appointment;

  public AppointmentWidget() {
    this.setStylePrimaryName("bee-appointment");
    headerPanel.setStylePrimaryName("header");
    bodyPanel.setStylePrimaryName("body");
    footerPanel.setStylePrimaryName("footer");
    timelinePanel.setStylePrimaryName("timeline");
    timelineFillPanel.setStylePrimaryName("timeline-fill");

    this.add(headerPanel);
    this.add(bodyPanel);
    this.add(footerPanel);
    this.add(timelinePanel);
    timelinePanel.add(timelineFillPanel);

    DOM.setStyleAttribute(this.getElement(), "position", "absolute");
  }

  public int compareTo(AppointmentWidget appt) {
    int compare = this.getStart().compareTo(appt.getStart());
    if (compare == 0) {
      compare = appt.getEnd().compareTo(this.getEnd());
    }
    return compare;
  }

  public void formatTimeline(double t, double h) {
    timelineFillPanel.setHeight(h + "%");
    DOM.setStyleAttribute(timelineFillPanel.getElement(), "top", t + "%");
  }

  public Appointment getAppointment() {
    return appointment;
  }

  public Widget getBody() {
    return this.bodyPanel;
  }

  public String getDescription() {
    return description;
  }

  public Date getEnd() {
    return end;
  }

  public Widget getHeader() {
    return this.headerPanel;
  }

  public double getHeight() {
    return height;
  }

  public double getLeft() {
    return left;
  }

  public Widget getMoveHandle() {
    return headerPanel;
  }

  public Widget getResizeHandle() {
    return footerPanel;
  }

  public Date getStart() {
    return start;
  }

  public String getTitle() {
    return title;
  }

  public double getTop() {
    return top;
  }

  public double getWidth() {
    return width;
  }

  public boolean isMultiDay() {
    return multiDay;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setAppointment(Appointment appointment) {
    this.appointment = appointment;

    if (appointment.isReadOnly()) {
      this.remove(footerPanel);
    }
  }

  public void setDescription(String description) {
    this.description = description;
    DOM.setInnerHTML(bodyPanel.getElement(), description);
  }

  public void setEnd(Date end) {
    this.end = end;
  }

  public void setHeight(double height) {
    this.height = height;
    DOM.setStyleAttribute(this.getElement(), "height", height + "px");
  }

  public void setLeft(double left) {
    this.left = left;
    DOM.setStyleAttribute(this.getElement(), "left", left + "%");
  }

  public void setMultiDay(boolean isMultiDay) {
    this.multiDay = isMultiDay;
  }

  public void setStart(Date start) {
    this.start = start;
  }

  public void setTitle(String title) {
    this.title = title;
    DOM.setInnerHTML(headerPanel.getElement(), title);
  }

  public void setTop(double top) {
    this.top = top;
    DOM.setStyleAttribute(this.getElement(), "top", top + "px");
  }

  public void setWidth(double width) {
    this.width = width;
    DOM.setStyleAttribute(this.getElement(), "width", width + "%");
  }
}
