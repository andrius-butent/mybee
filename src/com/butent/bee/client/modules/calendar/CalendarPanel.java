package com.butent.bee.client.modules.calendar;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.calendar.Attendee;
import com.butent.bee.client.calendar.Calendar;
import com.butent.bee.client.calendar.CalendarView.Type;
import com.butent.bee.client.calendar.event.CreateEvent;
import com.butent.bee.client.calendar.event.CreateHandler;
import com.butent.bee.client.calendar.event.DateRequestEvent;
import com.butent.bee.client.calendar.event.DateRequestHandler;
import com.butent.bee.client.calendar.event.DeleteEvent;
import com.butent.bee.client.calendar.event.DeleteHandler;
import com.butent.bee.client.calendar.event.TimeBlockClickEvent;
import com.butent.bee.client.calendar.event.TimeBlockClickHandler;
import com.butent.bee.client.calendar.event.UpdateEvent;
import com.butent.bee.client.calendar.event.UpdateHandler;
import com.butent.bee.client.calendar.monthview.MonthView;
import com.butent.bee.client.calendar.resourceview.ResourceView;
import com.butent.bee.client.composite.TabBar;
import com.butent.bee.client.data.Provider;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.datepicker.DatePicker;
import com.butent.bee.client.dom.Edges;
import com.butent.bee.client.dom.StyleUtils;
import com.butent.bee.client.layout.Complex;
import com.butent.bee.client.layout.Horizontal;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.widget.BeeButton;
import com.butent.bee.client.widget.BeeImage;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.RowInfo;
import com.butent.bee.shared.modules.calendar.CalendarSettings;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.GridDescription;
import com.butent.bee.shared.ui.ColumnDescription.ColType;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CalendarPanel extends Complex implements NewAppointmentEvent.Handler {
  
  private final long calendarId; 

  private final Calendar calendar;
  private final DatePicker datePicker;
  
  private final Complex gridPanel = new Complex();
  private GridPresenter gridPresenter = null;
  
  private int resourceNameIndex = BeeConst.UNDEF;

  private HandlerRegistration newAppointmentRegistration;

  public CalendarPanel(long calendarId, CalendarSettings settings) {
    this.calendarId = calendarId;
    this.calendar = new Calendar(settings);
    configureCalendar();

    calendar.suspendLayout();
    calendar.setType(Type.DAY, calendar.getSettings().getDefaultDisplayedDays());
    
    datePicker = new DatePicker(calendar.getDate());
    datePicker.addValueChangeHandler(new ValueChangeHandler<JustDate>() {
      public void onValueChange(ValueChangeEvent<JustDate> event) {
        calendar.setDate(event.getValue());
      }
    });

    BeeButton todayButton = new BeeButton("Today");
    todayButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        calendar.setDate(TimeUtils.today());
        datePicker.setDate(calendar.getDate());
      }
    });

    BeeButton leftWeekButton = new BeeButton("<");
    leftWeekButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        navigate(false);
      }
    });

    BeeButton rightWeekButton = new BeeButton(">");
    rightWeekButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        navigate(true);
      }
    });

    Horizontal panel = new Horizontal();
    panel.add(todayButton);
    panel.add(leftWeekButton);
    panel.add(rightWeekButton);

    BeeButton createButton = new BeeButton("CREATE");
    createButton.setStyleName("bee-CreateAppointment");
    createButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        CalendarKeeper.createAppointment(false);
      }
    });

    BeeButton refreshButton = new BeeButton("Refresh");
    refreshButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        refresh();
      }
    });
    
    addLeftTop(createButton, 30, 40);
    addLeftTop(datePicker, 10, 100);
    
    addLeftWidthTopBottom(gridPanel, 10, 180, 360, 80);
    addLeftBottom(refreshButton, 30, 40);

    addLeftTop(panel, 220, 10);

    BeeImage config = new BeeImage(Global.getImages().settings(), new Scheduler.ScheduledCommand() {
      public void execute() {
        CalendarKeeper.editSettings(CalendarPanel.this.calendarId, CalendarPanel.this.calendar);
      }
    });
    addRightTop(config, 10, 10);
    
    addRightTop(createViews(), 50, 10);

    add(calendar, new Edges(40, 10, 10, 220));
    
    loadResources();
    loadAppointments(null);
    
    setNewAppointmentRegistration(NewAppointmentEvent.register(this));
  }

  @Override
  public void onNewAppointment(NewAppointmentEvent event) {
    refresh();
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        calendar.resumeLayout();
        calendar.scrollToHour(calendar.getSettings().getScrollToHour());
      }
    });
  }
  
  @Override
  protected void onUnload() {
    super.onUnload();
    
    if (!BeeKeeper.getScreen().isTemporaryDetach() && getNewAppointmentRegistration() != null) {
      getNewAppointmentRegistration().removeHandler();
      setNewAppointmentRegistration(null);
    }
  }

  private void configureCalendar() {
    calendar.addDeleteHandler(new DeleteHandler<Appointment>() {
      public void onDelete(DeleteEvent<Appointment> event) {
        BeeKeeper.getLog().debug("Appointment deleted");
      }
    });

    calendar.addUpdateHandler(new UpdateHandler<Appointment>() {
      public void onUpdate(UpdateEvent<Appointment> event) {
        BeeKeeper.getLog().debug("Appointment updated");
      }
    });

    calendar.addOpenHandler(new OpenHandler<Appointment>() {
      public void onOpen(OpenEvent<Appointment> event) {
        CalendarKeeper.openAppointment(event.getTarget(), calendar);
      }
    });

    calendar.addCreateHandler(new CreateHandler<Appointment>() {
      public void onCreate(CreateEvent<Appointment> event) {
        BeeKeeper.getLog().debug("Appointment created");
      }
    });

    calendar.addTimeBlockClickHandler(new TimeBlockClickHandler<DateTime>() {
      public void onTimeBlockClick(TimeBlockClickEvent<DateTime> event) {
        CalendarKeeper.createAppointment(event.getTarget(), true);
      }
    });

    calendar.addDateRequestHandler(new DateRequestHandler<HasDateValue>() {
      public void onDateRequested(DateRequestEvent<HasDateValue> event) {
        BeeKeeper.getLog().debug("Requested", event.getTarget(),
            ((Element) event.getClicked()).getInnerText());
      }
    });
  }
  
  private GridDescription createGridDescription(BeeRowSet rowSet, List<String> columnNames) {
    String viewName = rowSet.getViewName();
    GridDescription gridDescription = new GridDescription(viewName, viewName);

    gridDescription.setCaption("Resources");
    gridDescription.setReadOnly(true);

    gridDescription.setHasHeaders(false);
    gridDescription.setHasFooters(true);

    gridDescription.setSearchThreshold(DataUtils.getDefaultSearchThreshold());

    gridDescription.addColumn(new ColumnDescription(ColType.SELECTION,
        NameUtils.createUniqueName("select-")));

    for (String colName : columnNames) {
      ColumnDescription columnDescription = new ColumnDescription(ColType.DATA, colName);
      columnDescription.setSource(colName);
      columnDescription.setSortable(true);
      columnDescription.setHasFooter(true);

      gridDescription.addColumn(columnDescription);
    }
    return gridDescription;
  }

  private void createGridPresenter(BeeRowSet rowSet, List<String> columnNames) {
    GridPresenter gp = new GridPresenter(createGridDescription(rowSet, columnNames),
        rowSet.getNumberOfRows(), rowSet, Provider.Type.LOCAL, EnumSet.of(UiOption.CHILD));
    setGridPresenter(gp);
    gp.setEventSource(getId());
    
    StyleUtils.makeAbsolute(gp.getWidget());
    gp.getWidget().addStyleName(StyleUtils.NAME_OCCUPY);

    gridPanel.add(gp.getWidget());
  }

  private Widget createViews() {
    TabBar tabBar = new TabBar();
    tabBar.addItem("1 Day");
    tabBar.addItem(BeeUtils.toString(calendar.getSettings().getDefaultDisplayedDays()) + " Days");
    tabBar.addItem("Work Week");
    tabBar.addItem("Week");
    tabBar.addItem("Month");
    tabBar.addItem("Resources");
    tabBar.selectTab(1, false);

    tabBar.addSelectionHandler(new SelectionHandler<Integer>() {
      public void onSelection(SelectionEvent<Integer> event) {
        int tabIndex = event.getSelectedItem();
        switch (tabIndex) {
          case 0:
            calendar.setType(Type.DAY, 1);
            break;
          case 1:
            calendar.setType(Type.DAY, calendar.getSettings().getDefaultDisplayedDays());
            break;
          case 2:
            calendar.setDate(TimeUtils.startOfWeek(calendar.getDate()));
            calendar.setType(Type.DAY, 5);
            datePicker.setDate(calendar.getDate());
            break;
          case 3:
            calendar.setType(Type.DAY, 7);
            break;
          case 4:
            calendar.setType(Type.MONTH);
            break;
          case 5:
            calendar.setType(Type.RESOURCE);
            refresh();
            break;
        }
      }
    });
    return tabBar;
  }
  
  private GridPresenter getGridPresenter() {
    return gridPresenter;
  }

  private HandlerRegistration getNewAppointmentRegistration() {
    return newAppointmentRegistration;
  }

  private int getResourceNameIndex() {
    return resourceNameIndex;
  }
  
  private void loadAppointmentAttendees(final BeeRowSet appRowSet, final Set<Long> attIds) {
    Queries.getRowSet(VIEW_APPOINTMENT_ATTENDEES, null, new Queries.RowSetCallback() {
      public void onSuccess(BeeRowSet result) {
        setAppointments(appRowSet, result, attIds);
      }
    }); 
  }
  
  private void loadAppointments(final Set<Long> attIds) {
    Queries.getRowSet(VIEW_APPOINTMENTS, null, new Queries.RowSetCallback() {
      public void onSuccess(BeeRowSet result) {
        loadAppointmentAttendees(result, attIds);
      }
    }); 
  }

  private void loadResources() {
    Queries.getRowSet(VIEW_ATTENDEES, null, new Queries.RowSetCallback() {
      public void onSuccess(BeeRowSet result) {
        setResourceNameIndex(DataUtils.getColumnIndex(COL_NAME, result.getColumns()));
        createGridPresenter(result, Lists.newArrayList(COL_NAME));
      }
    }); 
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
      int days = (calendar.getView() instanceof ResourceView) ? 1 : Math.max(calendar.getDays(), 1);
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
    
    calendar.setDate(newDate);
    datePicker.setDate(newDate);
  }

  private void refresh() {
    if (getGridPresenter() == null) {
      return;
    }

    Collection<RowInfo> selectedRows = getGridPresenter().getView().getContent().getSelectedRows();
    if (selectedRows.isEmpty()) {
      return;
    }

    List<Attendee> lst = Lists.newArrayList();
    Set<Long> attIds = Sets.newHashSet();

    for (RowInfo rowInfo : selectedRows) {
      IsRow row = getGridPresenter().getView().getContent().getGrid().getRowById(rowInfo.getId());
      if (row != null) {
        lst.add(new Attendee(row.getId(), row.getString(getResourceNameIndex())));
        attIds.add(row.getId());
      }
    }
    
    if (!lst.isEmpty()) {
      calendar.setAttendees(lst);
      loadAppointments(attIds);
    }
  }

  private void setAppointments(BeeRowSet apprs, BeeRowSet aars, Set<Long> attIds) {
    if (apprs == null || apprs.isEmpty()) {
      return;
    }
    
    List<Appointment> lst = Lists.newArrayList();
    
    int startIndex = DataUtils.getColumnIndex(COL_START_DATE_TIME, apprs.getColumns());
    int endIndex = DataUtils.getColumnIndex(COL_END_DATE_TIME, apprs.getColumns());

    int summaryIndex = DataUtils.getColumnIndex(COL_SUMMARY, apprs.getColumns());
    int descrIndex = DataUtils.getColumnIndex(COL_DESCRIPTION, apprs.getColumns());
    
    int appIndex = BeeConst.UNDEF;
    int attIndex = BeeConst.UNDEF;
    int nameIndex = BeeConst.UNDEF;
    
    if (aars != null && !aars.isEmpty()) {
      appIndex = DataUtils.getColumnIndex(COL_APPOINTMENT, aars.getColumns());
      attIndex = DataUtils.getColumnIndex(COL_ATTENDEE, aars.getColumns());
      nameIndex = DataUtils.getColumnIndex(COL_ATTENDEE_NAME, aars.getColumns());
    }
    
    for (IsRow row : apprs.getRows()) {
      DateTime start = row.getDateTime(startIndex);
      DateTime end = row.getDateTime(endIndex);
      if (start == null || end == null || start.getTime() >= end.getTime()) {
        continue;
      }

      Appointment appt = new Appointment();
      appt.setId(row.getId());
      
      appt.setTitle(row.getString(summaryIndex));
      appt.setDescription(row.getString(descrIndex));

      appt.setStart(start);
      appt.setEnd(end);

      if (nameIndex >= 0) {
        for (IsRow aa : aars.getRows()) {
          if (attIds != null && !attIds.isEmpty()) {
            if (!attIds.contains(aa.getLong(attIndex))) {
              continue;
            }
          }
          if (aa.getLong(appIndex) == row.getId()) {
            appt.getAttendees().add(new Attendee(aa.getLong(attIndex), aa.getString(nameIndex)));
          }
        }
      }
      
      if (attIds != null && !attIds.isEmpty() && appt.getAttendees().isEmpty()) {
        continue;
      }
      lst.add(appt);
    }
    
    calendar.setAppointments(lst);
  }

  private void setGridPresenter(GridPresenter gridPresenter) {
    this.gridPresenter = gridPresenter;
  }

  private void setNewAppointmentRegistration(HandlerRegistration newAppointmentRegistration) {
    this.newAppointmentRegistration = newAppointmentRegistration;
  }

  private void setResourceNameIndex(int resourceNameIndex) {
    this.resourceNameIndex = resourceNameIndex;
  }
}
