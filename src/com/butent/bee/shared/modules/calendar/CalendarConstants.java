package com.butent.bee.shared.modules.calendar;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.ui.HasCaption;
import com.butent.bee.shared.utils.BeeUtils;

public class CalendarConstants {
  
  public static enum AppointmentStatus implements HasCaption {
    TENTATIVE("Planuojamas"),
    CONFIRMED("Patvirtintas"),
    DELAYED("Atidėtas"),
    CANCELED("Atšauktas");
    
    private final String caption;
    
    private AppointmentStatus(String caption) {
      this.caption = caption;
    }

    public String getCaption() {
      return caption;
    }
  }

  public static enum ReminderMethod implements HasCaption {
    EMAIL, SMS, POPUP;

    public String getCaption() {
      return this.name().toLowerCase();
    } 
  }

  public static enum ResponseStatus implements HasCaption {
    NEEDS_ACTION, DECLINED, TENTATIVE, ACCEPTED;

    public String getCaption() {
      return BeeUtils.proper(this.name(), BeeConst.CHAR_UNDER);
    }
  }
  
  public static enum Transparency implements HasCaption {
    OPAQUE, TRANSPARENT;

    public String getCaption() {
      return this.name().toLowerCase();
    }
  }

  public static enum Visibility implements HasCaption {
    DEFAULT, PUBLIC, PRIVATE, CONFIDENTIAL;

    public String getCaption() {
      return this.name().toLowerCase();
    }
  }

  public enum TimeBlockClick implements HasCaption {
    Double, Single, Drag;

    public String getCaption() {
      return this.name();
    }
  }
  
  public static final String CALENDAR_MODULE = "Calendar";
  public static final String CALENDAR_METHOD = CALENDAR_MODULE + "Method";

  public static final String SVC_GET_CONFIGURATION = "get_configuration"; 
  public static final String SVC_GET_USER_CALENDAR = "get_user_calendar"; 

  public static final String PARAM_CALENDAR_ID = "calendar_id";
  
  public static final String TBL_USER_CALENDARS = "UserCalendars";
  public static final String TBL_CONFIGURATION = "Configuration";

  public static final String VIEW_CALENDARS = "Calendars";
  public static final String VIEW_USER_CALENDARS = "UserCalendars";
  public static final String VIEW_CONFIGURATION = "Configuration";

  public static final String VIEW_PROPERTY_GROUPS = "PropertyGroups";
  public static final String VIEW_EXTENDED_PROPERTIES = "ExtendedProperties";

  public static final String VIEW_ATTENDEES = "Attendees";
  public static final String VIEW_ATTENDEE_PROPS = "AttendeeProps";

  public static final String VIEW_APPOINTMENTS = "Appointments";
  public static final String VIEW_APPOINTMENT_PROPS = "AppointmentProps";
  public static final String VIEW_APPOINTMENT_ATTENDEES = "AppointmentAttendees";

  public static final String VIEW_THEME_COLORS = "ThemeColors";

  public static final String GRID_CALENDARS = "Calendars";
  public static final String GRID_APPOINTMENTS = "Appointments";

  public static final String FORM_CALENDAR_SETTINGS = "CalendarSettings";
  public static final String FORM_CONFIGURATION = "Configuration";
  public static final String FORM_NEW_APPOINTMENT = "NewAppointment";
  
  public static final String COL_USER = "User";
  public static final String COL_CALENDAR = "Calendar";
  public static final String COL_CALENDAR_NAME = "CalendarName";
  public static final String COL_NAME = "Name";
  
  public static final String COL_DEFAULT_DISPLAYED_DAYS = "DefaultDisplayedDays";

  public static final String COL_PIXELS_PER_INTERVAL = "PixelsPerInterval";
  public static final String COL_INTERVALS_PER_HOUR = "IntervalsPerHour";

  public static final String COL_WORKING_HOUR_START = "WorkingHourStart";
  public static final String COL_WORKING_HOUR_END = "WorkingHourEnd";
  public static final String COL_SCROLL_TO_HOUR = "ScrollToHour";
  public static final String COL_OFFSET_HOUR_LABELS = "OffsetHourLabels";

  public static final String COL_ENABLE_DRAG_DROP = "EnableDragDrop";
  public static final String COL_DRAG_DROP_CREATION = "DragDropCreation";
  public static final String COL_TIME_BLOCK_CLICK_NUMBER = "TimeBlockClickNumber";
  
  public static final String COL_FAVORITE = "Favorite";

  public static final String COL_COMPANY = "Company";
  public static final String COL_COMPANY_NAME = "CompanyName";

  public static final String COL_APPOINTMENT = "Appointment";
  public static final String COL_APPOINTMENT_TYPE = "AppointmentType";
  public static final String COL_TYPE_NAME = "TypeName";

  public static final String COL_ATTENDEE = "Attendee";
  public static final String COL_ATTENDEE_NAME = "AttendeeName";

  public static final String COL_TIME_ZONE = "TimeZone";
  public static final String COL_THEME = "Theme";

  public static final String COL_PROPERTY = "Property";
  public static final String COL_PROPERTY_GROUP = "PropertyGroup";
  public static final String COL_GROUP_NAME = "GroupName";
  public static final String COL_DEFAULT_PROPERTY = "DefaultProperty";

  public static final String COL_HOURS = "Hours";
  public static final String COL_MINUTES = "Minutes";
  
  public static final String COL_START_DATE = "StartDate";
  public static final String COL_START_DATE_TIME = "StartDateTime";

  public static final String COL_END_DATE = "EndDate";
  public static final String COL_END_DATE_TIME = "EndDateTime";
  
  public static final String COL_EFFECTIVE_START = "EffectiveStart";
  public static final String COL_EFFECTIVE_END = "EffectiveEnd";

  public static final String COL_VEHICLE = "Vehicle";
  public static final String COL_VEHICLE_OWNER = "VehicleOwner";

  public static final String COL_COLOR = "Color";
  public static final String COL_BACKGROUND = "Background";
  public static final String COL_FOREGROUND = "Foreground";

  public static final String COL_SUMMARY = "Summary";
  public static final String COL_DESCRIPTION = "Description";
  
  public static final String NAME_START = "Start";
  public static final String NAME_END = "End";

  private CalendarConstants() {
  }
}
