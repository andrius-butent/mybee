package com.butent.bee.client.modules.calendar;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.calendar.CalendarConstants.*;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.value.ValueType;
import com.butent.bee.shared.ui.Captions;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;

class AppointmentRenderer {

  private static final String DEFAULT_SIMPLE_HEADER_TEMPLATE;
  private static final String DEFAULT_SIMPLE_BODY_TEMPLATE;

  private static final String DEFAULT_MULTI_HEADER_TEMPLATE;
  private static final String DEFAULT_MULTI_BODY_TEMPLATE;

  private static final String DEFAULT_COMPACT_TEMPLATE;
  private static final String DEFAULT_TITLE_TEMPLATE;

  private static final String STRING_TEMPLATE;

  private static final Splitter TEMPLATE_SPLITTER =
      Splitter.on(CharMatcher.inRange('\u0000', '\u001f'));

  private static final String HTML_LINE_SEPARATOR = "<br/>";
  private static final String TEXT_LINE_SEPARATOR = String.valueOf('\n');

  private static final String SIMPLE_HTML_SEPARATOR = HTML_LINE_SEPARATOR;
  private static final String MULTI_HTML_SEPARATOR = " ";
  private static final String COMPACT_HTML_SEPARATOR = " ";

  private static final String STRING_SEPARATOR = ", ";
  private static final String CHILD_SEPARATOR = ", ";

  private static final String SUBSTITUTE_PREFIX = "{";
  private static final String SUBSTITUTE_SUFFIX = "}";

  private static final String KEY_RESOURCES = "Resources";
  private static final String KEY_PROPERTIES = "Properties";
  private static final String KEY_REMINDERS = "Reminders";

  private static final String KEY_PERIOD = "Period";

  static {
    DEFAULT_SIMPLE_HEADER_TEMPLATE = wrap(COL_SUMMARY);
    DEFAULT_SIMPLE_BODY_TEMPLATE = BeeUtils.buildLines(wrap(COL_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL)),
        wrap(COL_VEHICLE_NUMBER), wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(COL_DESCRIPTION),
        wrap(KEY_REMINDERS));

    DEFAULT_MULTI_HEADER_TEMPLATE = BeeUtils.joinWords(wrap(KEY_PERIOD), wrap(COL_SUMMARY));
    DEFAULT_MULTI_BODY_TEMPLATE = BeeUtils.joinWords(wrap(COL_COMPANY_NAME),
        wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL), wrap(COL_VEHICLE_NUMBER),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES));

    DEFAULT_COMPACT_TEMPLATE = BeeUtils.joinWords(wrap(COL_SUMMARY),
        wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL), wrap(COL_VEHICLE_NUMBER));

    DEFAULT_TITLE_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        BeeConst.STRING_EMPTY, wrap(COL_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL),
            wrap(COL_VEHICLE_NUMBER)), BeeConst.STRING_EMPTY,
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), BeeConst.STRING_EMPTY, wrap(COL_DESCRIPTION),
        BeeConst.STRING_EMPTY, BeeUtils.joinWords(wrap(COL_ORGANIZER_FIRST_NAME),
            wrap(COL_ORGANIZER_LAST_NAME)), wrap(KEY_REMINDERS));

    STRING_TEMPLATE = BeeUtils.buildLines(wrap(KEY_PERIOD), wrap(COL_STATUS),
        wrap(COL_COMPANY_NAME),
        BeeUtils.joinWords(wrap(COL_VEHICLE_PARENT_MODEL), wrap(COL_VEHICLE_MODEL),
            wrap(COL_VEHICLE_NUMBER)), wrap(COL_SUMMARY),
        wrap(KEY_PROPERTIES), wrap(KEY_RESOURCES), wrap(COL_DESCRIPTION),
        BeeUtils.joinWords(wrap(COL_ORGANIZER_FIRST_NAME), wrap(COL_ORGANIZER_LAST_NAME)),
        wrap(KEY_REMINDERS));
  }

  private static String wrap(String s) {
    return SUBSTITUTE_PREFIX + s.trim() + SUBSTITUTE_SUFFIX;
  }

  AppointmentRenderer() {
    super();
  }

  void render(long calendarId, AppointmentWidget appointmentWidget, String headerTemplate,
      String bodyTemplate, String titleTemplate, boolean multi) {

    Map<String, String> substitutes = getSubstitutes(calendarId,
        appointmentWidget.getAppointment());
    String separator = multi ? MULTI_HTML_SEPARATOR : SIMPLE_HTML_SEPARATOR;

    String template = BeeUtils.notEmpty(headerTemplate,
        multi ? DEFAULT_MULTI_HEADER_TEMPLATE : DEFAULT_SIMPLE_HEADER_TEMPLATE);
    String header = parseTemplate(template, substitutes, separator);
    appointmentWidget.setHeaderHtml(header);

    template = BeeUtils.notEmpty(bodyTemplate,
        multi ? DEFAULT_MULTI_BODY_TEMPLATE : DEFAULT_SIMPLE_BODY_TEMPLATE);
    String body = parseTemplate(template, substitutes, separator);
    if (BeeUtils.allEmpty(header, body) || !multi && BeeUtils.isEmpty(body)) {
      body = renderEmpty(appointmentWidget.getAppointment());
    }
    appointmentWidget.setBodyHtml(body);

    template = BeeUtils.notEmpty(titleTemplate, DEFAULT_TITLE_TEMPLATE);
    String title = parseTemplate(template, substitutes, TEXT_LINE_SEPARATOR);
    appointmentWidget.setTitleText(title);
  }

  void renderCompact(long calendarId, Appointment appointment, String compactTemplate,
      Widget htmlWidget, String titleTemplate, Widget titleWidget) {

    Map<String, String> substitutes = getSubstitutes(calendarId, appointment);

    String template = BeeUtils.notEmpty(compactTemplate, DEFAULT_COMPACT_TEMPLATE);
    String html = parseTemplate(template, substitutes, COMPACT_HTML_SEPARATOR);
    if (BeeUtils.isEmpty(html)) {
      html = renderEmpty(appointment);
    }
    if (!BeeUtils.isEmpty(html) && htmlWidget != null) {
      htmlWidget.getElement().setInnerHTML(BeeUtils.trim(html));
    }

    template = BeeUtils.notEmpty(titleTemplate, DEFAULT_TITLE_TEMPLATE);
    String title = parseTemplate(template, substitutes, TEXT_LINE_SEPARATOR);
    if (!BeeUtils.isEmpty(title) && titleWidget != null) {
      titleWidget.setTitle(BeeUtils.trim(title));
    }
  }

  void renderMulti(long calendarId, AppointmentWidget appointmentWidget) {
    render(calendarId, appointmentWidget, DEFAULT_MULTI_HEADER_TEMPLATE,
        DEFAULT_MULTI_BODY_TEMPLATE, DEFAULT_TITLE_TEMPLATE, true);
  }

  void renderSimple(long calendarId, AppointmentWidget appointmentWidget) {
    render(calendarId, appointmentWidget, DEFAULT_SIMPLE_HEADER_TEMPLATE,
        DEFAULT_SIMPLE_BODY_TEMPLATE, DEFAULT_TITLE_TEMPLATE, false);
  }

  String renderString(long calendarId, Appointment appointment) {
    return parseTemplate(STRING_TEMPLATE, getSubstitutes(calendarId, appointment),
        STRING_SEPARATOR);
  }

  private static Map<String, String> getSubstitutes(long calendarId, Appointment appointment) {
    Map<String, String> result = Maps.newHashMap();

    BeeRow row = appointment.getRow();
    List<BeeColumn> columns = CalendarKeeper.getAppointmentViewColumns();

    for (int i = 0; i < columns.size(); i++) {
      String key = columns.get(i).getId();
      String value = row.getString(i);

      if (key.equals(COL_STATUS) && BeeUtils.isInt(value)) {
        value = Captions.getCaption(AppointmentStatus.class, BeeUtils.toInt(value));
      } else if (value != null && ValueType.DATE_TIME.equals(columns.get(i).getType())) {
        value = CalendarUtils.renderDateTime(row.getDateTime(i));
      }

      result.put(wrap(key), BeeUtils.trim(value));
    }

    String attNames = BeeConst.STRING_EMPTY;
    String propNames = BeeConst.STRING_EMPTY;
    String remindNames = BeeConst.STRING_EMPTY;

    if (!appointment.getAttendees().isEmpty()) {
      for (Long id : appointment.getAttendees()) {
        attNames = BeeUtils.join(CHILD_SEPARATOR, attNames,
            CalendarKeeper.getAttendeeCaption(calendarId, id));
      }
    }
    if (!appointment.getProperties().isEmpty()) {
      for (Long id : appointment.getProperties()) {
        propNames = BeeUtils.join(CHILD_SEPARATOR, propNames, CalendarKeeper.getPropertyName(id));
      }
    }
    if (!appointment.getReminders().isEmpty()) {
      for (Long id : appointment.getReminders()) {
        remindNames = BeeUtils.join(CHILD_SEPARATOR, remindNames,
            CalendarKeeper.getReminderTypeName(id));
      }
    }

    result.put(wrap(KEY_RESOURCES), attNames);
    result.put(wrap(KEY_PROPERTIES), propNames);
    result.put(wrap(KEY_REMINDERS), remindNames);

    result.put(wrap(KEY_PERIOD), CalendarUtils.renderPeriod(appointment.getStart(), appointment
        .getEnd()));

    return result;
  }

  private static String parseLine(String line, Map<String, String> substitutes) {
    if (BeeUtils.isEmpty(line) || substitutes.isEmpty() || !line.contains(SUBSTITUTE_PREFIX)) {
      return line;
    }

    String result = line;
    for (Map.Entry<String, String> entry : substitutes.entrySet()) {
      if (entry.getValue() != null) {
        result = result.replace(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private static String parseTemplate(String template, Map<String, String> substitutes,
      String separator) {
    if (BeeUtils.isEmpty(template)) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    for (String line : TEMPLATE_SPLITTER.split(template.trim())) {
      String s = parseLine(line, substitutes);
      if (!BeeUtils.isEmpty(s)) {
        if (sb.length() > 0) {
          sb.append(separator);
        }
        sb.append(s);
      }
    }
    return sb.toString();
  }

  private static String renderEmpty(Appointment appointment) {
    return CalendarUtils.renderPeriod(appointment.getStart(), appointment.getEnd());
  }
}
