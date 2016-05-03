package com.butent.bee.client.render;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.css.Colors;
import com.butent.bee.shared.css.values.TextAlign;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XFont;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.export.XStyle;
import com.butent.bee.shared.html.builder.elements.Div;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public abstract class AbstractSlackRenderer extends AbstractCellRenderer {

  private enum Kind {
    LATE, OPENING, ENDGAME, SCHEDULED;

    private String getStyleName() {
      return STYLE_PREFIX + name().toLowerCase();
    }
  }

  private static final String STYLE_PREFIX = TaskConstants.CRM_STYLE_PREFIX + "Slack-";

  private static final String STYLE_BAR = STYLE_PREFIX + "bar";
  private static final String STYLE_LABEl = STYLE_PREFIX + "label";

  private static String format(Kind kind, String label) {
    Div bar = new Div().addClass(STYLE_BAR).addClass(kind.getStyleName());
    if (!BeeUtils.isEmpty(label)) {
      bar.appendChild(new Div().addClass(STYLE_LABEl).text(label));
    }

    return bar.toString();
  }

  private static Kind getKind(DateTime start, DateTime finish) {

    DateTime now = TimeUtils.nowMinutes();

    if (finish != null && TimeUtils.isLess(finish, now)) {
      return Kind.LATE;
    } else if (start != null && TimeUtils.isMore(finish, start) && TimeUtils.isMeq(now, start)
        && TimeUtils.isLess(now, finish)) {
      if (now.getTime() - start.getTime() < (finish.getTime() - start.getTime()) / 2) {
        return Kind.OPENING;
      } else {
        return Kind.ENDGAME;
      }

    } else {
      return null;
    }
  }

  private static String getLabel(long minutes) {
    if (minutes <= 0L) {
      return BeeConst.STRING_ZERO;

    } else if (minutes < TimeUtils.MINUTES_PER_DAY) {
      return TimeUtils.renderMinutes(BeeUtils.toInt(minutes), false);

    } else {
      return BeeUtils.toString(minutes / TimeUtils.MINUTES_PER_DAY);
    }
  }

  private static long getMinutes(Kind kind, DateTime start, DateTime finish) {
    DateTime now = TimeUtils.nowMinutes();

    switch (kind) {
      case LATE:
        return (now.getTime() - finish.getTime()) / TimeUtils.MILLIS_PER_MINUTE;

      case OPENING:
      case ENDGAME:
        return (finish.getTime() - now.getTime()) / TimeUtils.MILLIS_PER_MINUTE;

      case SCHEDULED:
        return TimeUtils.dayDiff(now, start) * TimeUtils.MINUTES_PER_DAY;

      default:
        Assert.untouchable();
        return 0L;
    }
  }

  private final List<? extends IsColumn> isColumns;

  protected AbstractSlackRenderer(List<? extends IsColumn> columns) {
    super(null);

    this.isColumns = columns;
  }

  /**
   * This method must return start DateTime which will be used in
   * {@code export(IsRow, int, Integer, XSheet}, {@code render(IsRow)} and
   * {@code getKind(DateTime, DateTime)} methods.
   * 
   * If null is returned then the calculations won't work.
   * 
   * @param columns are IsColumn columns.
   * @param row is provided current IsRow.
   * @return start DateTime which will be used for calculations.
   */
  public abstract DateTime getStartDateTime(List<? extends IsColumn> columns, IsRow row);

  /**
   * This method must return end DateTime which will be used in
   * {@code export(IsRow, int, Integer, XSheet}, {@code render(IsRow)} and
   * {@code getKind(DateTime, DateTime)} methods.
   * 
   * If null is returned then the calculations won't work.
   * 
   * @param columns are IsColumn columns.
   * @param row is provided current IsRow.
   * @return end DateTime which will be used for calculations.
   */
  public abstract DateTime getFinishDateTime(List<? extends IsColumn> columns, IsRow row);

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    if (row == null || sheet == null) {
      return null;
    }

    DateTime start = getStartDateTime(isColumns, row);
    DateTime finish = getFinishDateTime(isColumns, row);

    Kind kind = getKind(start, finish);
    if (kind == null) {
      return null;
    }

    long minutes = getMinutes(kind, start, finish);
    String text = (minutes == 0L) ? BeeConst.STRING_EMPTY : getLabel(minutes);

    XStyle style = new XStyle();
    XFont font;

    switch (kind) {
      case LATE:
        style.setColor(Colors.RED);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case OPENING:
        style.setColor(Colors.GREEN);
        style.setTextAlign(TextAlign.CENTER);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case ENDGAME:
        style.setColor(Colors.ORANGE);
        style.setTextAlign(TextAlign.CENTER);

        font = XFont.bold();
        font.setColor(Colors.WHITE);
        style.setFontRef(sheet.registerFont(font));
        break;

      case SCHEDULED:
        style.setColor(Colors.YELLOW);
        style.setTextAlign(TextAlign.RIGHT);
        break;
    }

    return new XCell(cellIndex, text, sheet.registerStyle(style));
  }

  @Override
  public String render(IsRow row) {
    if (row == null) {
      return null;
    }

    DateTime start = getStartDateTime(isColumns, row);
    DateTime finish = getFinishDateTime(isColumns, row);

    Kind kind = getKind(start, finish);
    if (kind == null) {
      return BeeConst.STRING_EMPTY;
    }

    long minutes = getMinutes(kind, start, finish);
    if (minutes == 0L) {
      return BeeConst.STRING_EMPTY;
    }

    String label = getLabel(minutes);
    return format(kind, label);
  }
}
