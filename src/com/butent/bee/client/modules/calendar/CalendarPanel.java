package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.dialog.Popup.OutsideClick;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.modules.calendar.CalendarView.Type;
import com.butent.bee.client.modules.calendar.event.AppointmentEvent;
import com.butent.bee.client.modules.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.modules.calendar.event.UpdateEvent;
import com.butent.bee.client.modules.calendar.view.MonthView;
import com.butent.bee.client.modules.calendar.view.ResourceView;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.screen.Domain;
import com.butent.bee.client.screen.HandlesStateChange;
import com.butent.bee.client.screen.HasDomain;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasWidgetSupplier;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderSilverImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.widget.Html;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.State;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.event.HandlesDeleteEvents;
import com.butent.bee.shared.data.event.MultiDeleteEvent;
import com.butent.bee.shared.data.event.RowDeleteEvent;
import com.butent.bee.shared.data.event.RowUpdateEvent;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.value.LongValue;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.EnumSet;
import java.util.List;

public class CalendarPanel extends Complex implements AppointmentEvent.Handler, Presenter, View,
    Printable, VisibilityChangeEvent.Handler, HasWidgetSupplier, HandlesStateChange, HasDomain,
    HandlesDeleteEvents {

  private static final BeeLogger logger = LogUtils.getLogger(CalendarPanel.class);

  private static final String STYLE_PANEL = "bee-cal-Panel";
  private static final String STYLE_CONTROLS = "bee-cal-Panel-controls";

  private static final String STYLE_TODAY = "bee-cal-Panel-today";

  private static final String STYLE_NAV_CONTAINER = "bee-cal-Panel-navContainer";
  private static final String STYLE_NAV_ITEM = "bee-cal-Panel-navItem";
  private static final String STYLE_NAV_PREV = "bee-cal-Panel-navPrev";
  private static final String STYLE_NAV_NEXT = "bee-cal-Panel-navNext";

  private static final String STYLE_DATE = "bee-cal-Panel-date";

  private static final String STYLE_VIEW_PREFIX = "bee-cal-Panel-view-";

  private static final String STYLE_CALENDAR = "bee-cal-Panel-calendar";

  private static final DateTimeFormat DATE_FORMAT =
      DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL);

  private final long calendarId;

  private final HeaderView header;

  private final CalendarWidget calendar;
  private final Html dateBox;

  private final TabBar viewTabs;
  private final List<ViewType> views = Lists.newArrayList();

  private final Timer timer;

  private final List<HandlerRegistration> registry = Lists.newArrayList();

  private boolean enabled = true;

  public CalendarPanel(long calendarId, String caption, CalendarSettings settings,
      BeeRowSet ucAttendees) {
    super();
    addStyleName(STYLE_PANEL);

    this.calendarId = calendarId;

    this.calendar = new CalendarWidget(calendarId, settings);

    calendar.addOpenHandler(new OpenHandler<Appointment>() {
      @Override
      public void onOpen(OpenEvent<Appointment> event) {
        CalendarKeeper.openAppointment(event.getTarget(), getCalendarId());
      }
    });

    calendar.addTimeBlockClickHandler(new TimeBlockClickEvent.Handler() {
      @Override
      public void onTimeBlockClick(TimeBlockClickEvent event) {
        CalendarKeeper.createAppointment(getCalendarId(), event.getStart(), event.getAttendeeId());
      }
    });

    calendar.addUpdateHandler(new UpdateEvent.Handler() {
      @Override
      public void onUpdate(UpdateEvent event) {
        if (!updateAppointment(event.getAppointment(), event.getNewStart(), event.getNewEnd(),
            event.getOldColumnIndex(), event.getNewColumnIndex())) {
          event.setCanceled(true);
        }
      }
    });

    this.header = new HeaderSilverImpl();
    header.create(caption, false, true, EnumSet.of(UiOption.ROOT),
        EnumSet.of(Action.REFRESH, Action.CONFIGURE), Action.NO_ACTIONS, Action.NO_ACTIONS);
    header.setViewPresenter(this);

    this.dateBox = new Html();
    dateBox.addStyleName(STYLE_DATE);

    dateBox.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        pickDate();
      }
    });

    this.viewTabs = new TabBar(STYLE_VIEW_PREFIX, Orientation.HORIZONTAL);
    viewTabs.setKeyboardNavigationEnabled(false);
    
    viewTabs.addSelectionHandler(new SelectionHandler<Integer>() {
      @Override
      public void onSelection(SelectionEvent<Integer> event) {
        Integer index = event.getSelectedItem();
        if (BeeUtils.isIndex(views, index)) {
          activateView(views.get(index));
        }
      }
    });

    this.timer = new Timer() {
      @Override
      public void run() {
        calendar.onClock();
      }
    };
    timer.scheduleRepeating(TimeUtils.MILLIS_PER_MINUTE);

    add(header);

    Html today = new Html("Šiandien");
    today.addStyleName(STYLE_TODAY);

    today.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        setDate(TimeUtils.today(), true);
      }
    });

    Html prev = new Html("<");
    prev.addStyleName(STYLE_NAV_ITEM);
    prev.addStyleName(STYLE_NAV_PREV);

    prev.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        navigate(false);
      }
    });

    Html next = new Html(">");
    next.addStyleName(STYLE_NAV_ITEM);
    next.addStyleName(STYLE_NAV_NEXT);

    next.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        navigate(true);
      }
    });

    Flow controls = new Flow();
    controls.addStyleName(STYLE_CONTROLS);

    controls.add(today);

    Horizontal nav = new Horizontal();
    nav.addStyleName(STYLE_NAV_CONTAINER);

    nav.add(prev);
    nav.add(dateBox);
    nav.add(next);
    controls.add(nav);

    controls.add(viewTabs);
    add(controls);

    Simple container = new Simple();
    container.addStyleName(STYLE_CALENDAR);
    container.setWidget(calendar);
    add(container);

    registry.add(AppointmentEvent.register(this));
    registry.add(VisibilityChangeEvent.register(this));
    registry.addAll(BeeKeeper.getBus().registerDeleteHandler(this, false));

    updateUcAttendees(ucAttendees, false);

    int viewIndex = updateViews(settings);
    activateView(views.get(viewIndex));
  }

  public long getCalendarId() {
    return calendarId;
  }

  @Override
  public String getCaption() {
    return header.getCaption();
  }

  @Override
  public Domain getDomain() {
    return Domain.CALENDAR;
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return header;
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public String getSupplierKey() {
    return CalendarKeeper.getCalendarSupplierKey(getCalendarId());
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public IdentifiableWidget getWidget() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh();
        break;

      case CONFIGURE:
        CalendarKeeper.editSettings(getCalendarId(), this);
        break;

      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      case PRINT:
        Printer.print(this);
        break;

      default:
        logger.info(action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onAppointment(AppointmentEvent event) {
    if (event.isRelevant(calendar)) {
      if (!DomUtils.isVisible(getElement())) {
        calendar.suspendLayout();
      }
      if (event.isUpdated()) {
        calendar.removeAppointment(event.getAppointment().getId(), false);
      }
      calendar.addAppointment(event.getAppointment(), true);
    }
  }

  @Override
  public void onMultiDelete(MultiDeleteEvent event) {
    if (VIEW_APPOINTMENTS.equals(event.getViewName())) {
      boolean removed = false;
      for (RowInfo rowInfo : event.getRows()) {
        removed |= calendar.removeAppointment(rowInfo.getId(), false);
      }

      if (removed) {
        refreshCalendar(false);
      }
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;
    String id = source.getId();

    if (getId().equals(id)) {
      int height = source.getClientHeight() + getPrintHeightAdjustment();
      StyleUtils.setSize(target, source.getClientWidth(), height);
      ok = true;

    } else if (calendar.getElement().isOrHasChild(source)) {
      if (StyleUtils.hasClassName(source, CalendarStyleManager.SCROLL_AREA)) {
        int height = source.getClientHeight() + getPrintHeightAdjustment();
        StyleUtils.setHeight(target, height);

        int start = CalendarUtils.getStartPixels(getSettings(),
            calendar.getView().getAppointmentWidgets());
        if (start > 0) {
          target.setScrollTop(start);
        }
      }
      ok = true;

    } else if (dateBox.getId().equals(id)) {
      ok = true;

    } else if (viewTabs.getId().equals(id)) {
      ok = false;

    } else if (StyleUtils.hasAnyClass(source, Sets.newHashSet(STYLE_TODAY, STYLE_NAV_ITEM))) {
      ok = false;

    } else if (header.asWidget().getElement().isOrHasChild(source)) {
      ok = header.onPrint(source, target);

    } else {
      ok = true;
    }

    return ok;
  }

  @Override
  public void onRowDelete(RowDeleteEvent event) {
    if (VIEW_APPOINTMENTS.equals(event.getViewName())) {
      boolean removed = calendar.removeAppointment(event.getRowId(), false);

      if (removed) {
        refreshCalendar(false);
      }
    }
  }

  @Override
  public void onStateChange(State state) {
    if (State.ACTIVATED.equals(state)) {
      CalendarKeeper.onActivatePanel(this);
    } else if (State.REMOVED.equals(state)) {
      CalendarKeeper.onRemovePanel(getId(), getCalendarId());
    }
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      calendar.resumeLayout();
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  protected void onUnload() {
    for (HandlerRegistration hr : registry) {
      hr.removeHandler();
    }
    registry.clear();

    CalendarKeeper.saveActiveView(getSettings());
    onViewUnload();

    super.onUnload();
  }

  CalendarSettings getSettings() {
    return calendar.getSettings();
  }

  void setDate(JustDate date, boolean sync) {
    if (date != null && !date.equals(calendar.getDate())) {
      calendar.update(calendar.getType(), date, calendar.getDisplayedDays());
      if (sync) {
        CalendarKeeper.synchronizeDate(calendarId, date, false);
      }
      refreshDateBox();
    }
  }

  void updateSettings(BeeRow row, List<BeeColumn> columns) {
    getSettings().loadFrom(row, columns);

    int viewIndex = updateViews(getSettings());
    boolean updated = activateView(views.get(viewIndex));
    if (!updated) {
      refreshCalendar(true);
    }
  }

  void updateUcAttendees(BeeRowSet ucAttendees, boolean refresh) {
    List<Long> attIds = Lists.newArrayList();
    if (!DataUtils.isEmpty(ucAttendees)) {
      for (BeeRow row : ucAttendees.getRows()) {
        if (BeeUtils.isTrue(DataUtils.getBoolean(ucAttendees, row, COL_ENABLED))) {
          attIds.add(DataUtils.getLong(ucAttendees, row, COL_ATTENDEE));
        }
      }
    }
    calendar.setAttendees(attIds, refresh);
  }

  private boolean activateView(ViewType view) {
    if (view == null) {
      return false;
    }

    Type type = null;

    JustDate date = calendar.getDate();
    int days = calendar.getDisplayedDays();

    switch (view) {
      case DAY:
        type = Type.DAY;
        days = 1;
        break;

      case DAYS:
        type = Type.DAY;
        days = getSettings().getDefaultDisplayedDays();
        break;

      case WORK_WEEK:
        type = Type.DAY;
        date = TimeUtils.startOfWeek(calendar.getDate());
        days = 5;
        break;

      case WEEK:
        type = Type.DAY;
        days = 7;
        break;

      case MONTH:
        type = Type.MONTH;
        days = BeeConst.UNDEF;
        break;

      case RESOURCES:
        type = Type.RESOURCE;
        days = 1;
        break;
    }

    boolean changed = calendar.update(type, date, days);
    if (changed) {
      CalendarKeeper.synchronizeDate(calendarId, date, false);
      refreshDateBox();
    }

    getSettings().setActiveView(view);
    return changed;
  }

  private int getPrintHeightAdjustment() {
    Widget scrollArea = calendar.getView().getScrollArea();
    if (scrollArea == null) {
      return 0;
    }

    int start = CalendarUtils.getStartPixels(getSettings(),
        calendar.getView().getAppointmentWidgets());
    int end = CalendarUtils.getEndPixels(getSettings(), calendar.getView().getAppointmentWidgets());

    int height;
    if (end > start) {
      height = end - start + 2;
    } else if (start > 0) {
      height = scrollArea.getElement().getScrollHeight() - start;
    } else {
      height = scrollArea.getElement().getScrollHeight();
    }

    return height - scrollArea.getElement().getClientHeight();
  }

  private void navigate(boolean forward) {
    JustDate oldDate = calendar.getDate();
    JustDate newDate;

    if (calendar.getView() instanceof MonthView) {
      if (forward) {
        newDate = TimeUtils.startOfNextMonth(oldDate);
      } else {
        newDate = TimeUtils.startOfPreviousMonth(oldDate);
      }

    } else {
      int days = (calendar.getView() instanceof ResourceView)
          ? 1 : Math.max(calendar.getDisplayedDays(), 1);
      int shift = days;
      if (days == 5) {
        shift = 7;
      }
      if (!forward) {
        shift = -shift;
      }

      newDate = TimeUtils.nextDay(oldDate, shift);
      if (days == 5) {
        newDate = TimeUtils.startOfWeek(newDate);
      }
    }
    setDate(newDate, true);
  }

  private void pickDate() {
    final Popup popup = new Popup(OutsideClick.CLOSE);
    DatePicker datePicker = new DatePicker(calendar.getDate(), MIN_DATE, MAX_DATE);

    datePicker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      @Override
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        popup.close();
        setDate(event.getValue(), true);
      }
    });

    popup.setWidget(datePicker);
    popup.showRelativeTo(dateBox.getElement());
  }

  private void refresh() {
    calendar.loadAppointments(true);
  }

  private void refreshCalendar(boolean scroll) {
    if (!DomUtils.isVisible(getElement())) {
      calendar.suspendLayout();
    }
    calendar.refresh(scroll);
  }

  private void refreshDateBox() {
    Type type = calendar.getType();

    JustDate date = calendar.getDate();
    int days = calendar.getDisplayedDays();

    String html;
    if (date == null) {
      html = BeeConst.STRING_EMPTY;

    } else if (Type.MONTH.equals(type)) {
      html = BeeUtils.joinWords(date.getYear(), Format.renderMonthFullStandalone(date));

    } else if (type == null || Type.RESOURCE.equals(type) || days <= 1) {
      html = DATE_FORMAT.format(date);

    } else {
      String from = BeeUtils.joinWords(date.getYear(), Format.renderMonthFull(date), date.getDom());

      JustDate end = TimeUtils.nextDay(date, days - 1);
      String to;
      if (TimeUtils.sameMonth(date, end)) {
        to = BeeUtils.toString(end.getDom());
      } else if (date.getYear() == end.getYear()) {
        to = BeeUtils.joinWords(Format.renderMonthFull(end), end.getDom());
      } else {
        to = BeeUtils.joinWords(end.getYear(), Format.renderMonthFull(end), end.getDom());
      }

      html = from + " - " + to;
    }

    dateBox.setHTML(html);
  }

  private boolean updateAppointment(Appointment appointment, DateTime newStart, DateTime newEnd,
      int oldColumnIndex, int newColumnIndex) {
    boolean changed = false;

    if (Type.RESOURCE.equals(calendar.getView().getType())
        && oldColumnIndex != newColumnIndex
        && BeeUtils.isIndex(calendar.getAttendees(), oldColumnIndex)
        && BeeUtils.isIndex(calendar.getAttendees(), newColumnIndex)) {

      long oldAttendee = calendar.getAttendees().get(oldColumnIndex);
      long newAttendee = calendar.getAttendees().get(newColumnIndex);

      List<Long> attendees = Lists.newArrayList(appointment.getAttendees());

      boolean add = !attendees.contains(newAttendee);

      attendees.remove(oldAttendee);
      if (add) {
        attendees.add(newAttendee);
      }

      appointment.updateAttendees(attendees);

      String viewName = VIEW_APPOINTMENT_ATTENDEES;
      long appId = appointment.getId();

      Queries.delete(viewName, Filter.and(
          ComparisonFilter.isEqual(COL_APPOINTMENT, new LongValue(appId)),
          ComparisonFilter.isEqual(COL_ATTENDEE, new LongValue(oldAttendee))), null);

      if (add) {
        List<BeeColumn> columns = Lists.newArrayList(Data.getColumn(viewName, COL_APPOINTMENT),
            Data.getColumn(viewName, COL_ATTENDEE));
        List<String> values = Lists.newArrayList(Long.toString(appId), Long.toString(newAttendee));

        Queries.insert(viewName, columns, values);
      }
      changed = true;
    }

    if (appointment.getStart().equals(newStart) && appointment.getEnd().equals(newEnd)) {
      return changed;
    }

    String viewName = VIEW_APPOINTMENTS;
    final BeeRow row = appointment.getRow();

    List<BeeColumn> columns = Lists.newArrayList(Data.getColumn(viewName, COL_START_DATE_TIME),
        Data.getColumn(viewName, COL_END_DATE_TIME));

    List<String> oldValues = Lists.newArrayList(Data.getString(viewName, row, COL_START_DATE_TIME),
        Data.getString(viewName, row, COL_END_DATE_TIME));
    List<String> newValues = Lists.newArrayList(BeeUtils.toString(newStart.getTime()),
        BeeUtils.toString(newEnd.getTime()));

    Queries.update(viewName, row.getId(), row.getVersion(), columns, oldValues, newValues, null,
        new RowCallback() {
          @Override
          public void onSuccess(BeeRow result) {
            row.setVersion(result.getVersion());
            BeeKeeper.getBus().fireEvent(new RowUpdateEvent(VIEW_APPOINTMENTS, result));
          }
        });

    appointment.setStart(newStart);
    appointment.setEnd(newEnd);

    return true;
  }

  private int updateViews(CalendarSettings settings) {
    if (!views.isEmpty()) {
      viewTabs.clear();
      views.clear();
    }

    boolean anyVisible = settings.isAnyVisible();
    String caption;

    for (ViewType view : ViewType.values()) {
      if (!anyVisible || settings.isVisible(view)) {
        if (ViewType.DAYS.equals(view)) {
          caption = view.getCaption(settings.getDefaultDisplayedDays());
        } else {
          caption = view.getCaption();
        }

        viewTabs.addItem(caption);
        views.add(view);
      }
    }

    int index;
    if (settings.getActiveView() != null && views.contains(settings.getActiveView())) {
      index = views.indexOf(settings.getActiveView());
    } else {
      index = 0;
    }

    viewTabs.selectTab(index, false);
    return index;
  }
}
