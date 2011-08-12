package com.butent.bee.client.i18n;

import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.text.shared.AbstractRenderer;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

/**
 * Handles specific regional formatting of datetime values.
 */

public class DateTimeRenderer extends AbstractRenderer<DateTime> implements HasDateTimeFormat {

  private DateTimeFormat format;

  public DateTimeRenderer() {
    this(Format.getDefaultDateTimeFormat());
  }

  public DateTimeRenderer(DateTimeFormat format) {
    this.format = format;
  }

  public DateTimeRenderer(String pattern) {
    this.format = BeeUtils.isEmpty(pattern) ? null : Format.getDateTimeFormat(pattern);
  }

  public DateTimeFormat getDateTimeFormat() {
    return format;
  }

  public String render(DateTime object) {
    if (object == null) {
      return BeeConst.STRING_EMPTY;
    } else if (getDateTimeFormat() == null) {
      return object.toString();
    } else {
      return getDateTimeFormat().format(object.getJava());
    }
  }

  public void setDateTimeFormat(DateTimeFormat format) {
    this.format = format;
  }
}
