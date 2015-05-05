package com.butent.bee.client.output;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.ChoiceCallback;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.i18n.Format;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.InputDate;
import com.butent.bee.client.widget.InputSpinner;
import com.butent.bee.client.widget.Label;
import com.butent.bee.client.widget.ListBox;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.data.filter.Operator;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.report.DateTimeFunction;
import com.butent.bee.shared.time.HasDateValue;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReportDateItem extends ReportItem {

  private static final String FORMAT = "FORMAT";

  private DateTimeFunction format = DateTimeFunction.DATE;
  private ListBox formatWidget;

  private Operator filterOperator;
  private Long filter;
  private Editor filterWidget;

  public ReportDateItem(String name, String caption) {
    super(name, caption);
  }

  @Override
  public void clearFilter() {
    if (filterWidget != null) {
      filterWidget.clearValue();
    }
    filter = null;
  }

  @Override
  public void deserialize(String data) {
    Map<String, String> map = Codec.deserializeMap(data);

    if (!BeeUtils.isEmpty(map)) {
      setFormat(EnumUtils.getEnumByName(DateTimeFunction.class, map.get(FORMAT)));

      if (map.containsKey(Service.VAR_DATA)) {
        Pair<String, String> pair = Pair.restore(map.get(Service.VAR_DATA));
        filterOperator = EnumUtils.getEnumByName(Operator.class, pair.getA());
        filter = BeeUtils.toLongOrNull(pair.getB());
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReportDateItem)) {
      return false;
    }
    return super.equals(obj) && Objects.equals(getFormat(), ((ReportDateItem) obj).getFormat());
  }

  @Override
  public ReportValue evaluate(SimpleRow row) {
    ReportValue value;
    JustDate date = row.getDate(getName());

    if (date != null) {
      value = evaluate(date);
    } else {
      value = ReportValue.empty();
    }
    return value;
  }

  @Override
  public Long getFilter() {
    return filter;
  }

  public Operator getFilterOperator() {
    return filterOperator;
  }

  @Override
  public Widget getFilterWidget() {
    Flow container = new Flow(getStyle() + "-filter");
    renderFilter(container);
    return container;
  }

  public DateTimeFunction getFormat() {
    return format;
  }

  @Override
  public String getFormatedCaption() {
    String cap = getCaption();

    if (getFormat() != DateTimeFunction.DATE) {
      cap = BeeUtils.joinWords(cap, BeeUtils.parenthesize(getFormat().getCaption()));
    }
    return cap;
  }

  @Override
  public String getOptionsCaption() {
    return Localized.getConstants().dateFormat();
  }

  @Override
  public ListBox getOptionsWidget() {
    if (formatWidget == null) {
      formatWidget = new ListBox();

      for (DateTimeFunction fnc : getSupportedFunctions()) {
        formatWidget.addItem(fnc.getCaption(), fnc.name());
      }
    }
    formatWidget.setValue(getFormat().name());
    return formatWidget;
  }

  @Override
  public String getStyle() {
    return STYLE_DATE;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getFormat());
  }

  @Override
  public String saveOptions() {
    if (formatWidget != null) {
      setFormat(EnumUtils.getEnumByName(DateTimeFunction.class, formatWidget.getValue()));
      filter = null;
      filterWidget = null;
    }
    return super.saveOptions();
  }

  @Override
  public String serialize() {
    Map<String, Object> map = new HashMap<>();
    map.put(FORMAT, getFormat());

    saveFilter();

    if (filterOperator != null) {
      map.put(Service.VAR_DATA, Pair.of(filterOperator, filter));
    }
    return serialize(Codec.beeSerialize(map));
  }

  @Override
  public ReportItem setFilter(String value) {
    if (!BeeUtils.isEmpty(value)) {
      filterOperator = Operator.EQ;
      filter = BeeUtils.toLongOrNull(value);
    } else {
      filter = null;
    }
    if (filterWidget != null) {
      filterWidget.setValue(filter == null ? null : BeeUtils.toString(filter));
    }
    return this;
  }

  public ReportDateItem setFormat(DateTimeFunction dateFormat) {
    this.format = Assert.notNull(dateFormat);
    return this;
  }

  @Override
  public boolean validate(SimpleRow row) {
    if (!row.getRowSet().hasColumn(getName())) {
      return true;
    }
    return validate(row.getDate(getName()));
  }

  protected Editor createFilterEditor() {
    ListBox editor = new ListBox();
    editor.addItem("");

    switch (getFormat()) {
      case DATE:
        return new InputDate();
      case YEAR:
        return new InputSpinner();
      case DAY:
        for (int i = 1; i <= 31; i++) {
          editor.addItem(BeeUtils.toString(i), BeeUtils.toString(i));
        }
        break;
      case DAY_OF_WEEK:
        for (int i = 1; i <= 7; i++) {
          editor.addItem(Format.renderDayOfWeek(i), BeeUtils.toString(i));
        }
        break;
      case MONTH:
        for (int i = 1; i <= 12; i++) {
          editor.addItem(Format.renderMonthFullStandalone(i), BeeUtils.toString(i));
        }
        break;
      case QUATER:
        for (int i = 1; i <= 4; i++) {
          String display = null;

          switch (i) {
            case 1:
              display = "I";
              break;
            case 2:
              display = "II";
              break;
            case 3:
              display = "III";
              break;
            case 4:
              display = "IV";
              break;
          }
          editor.addItem(display, BeeUtils.toString(i));
        }
        break;

      default:
        editor = null;
        break;
    }
    return editor;
  }

  protected ReportValue evaluate(HasDateValue date) {
    String value = null;
    String display = null;
    int val = BeeUtils.toInt(getValue(date));

    switch (getFormat()) {
      case DATE:
        value = BeeUtils.padLeft(BeeUtils.toString(val), 10, BeeConst.CHAR_ZERO);
        display = date.toString();
        break;
      case DAY:
        value = TimeUtils.padTwo(val);
        display = BeeUtils.toString(val);
        break;
      case DAY_OF_WEEK:
        value = BeeUtils.toString(val);
        display = Format.renderDayOfWeek(val);
        break;
      case MONTH:
        value = TimeUtils.padTwo(val);
        display = Format.renderMonthFullStandalone(val);
        break;
      case QUATER:
        switch (val) {
          case 1:
            display = "I";
            break;
          case 2:
            display = "II";
            break;
          case 3:
            display = "III";
            break;
          case 4:
            display = "IV";
            break;
        }
        value = BeeUtils.toString(val);
        break;
      case YEAR:
        value = BeeUtils.toString(val);
        display = value;
        break;
      default:
        Assert.unsupported();
        break;
    }
    return ReportValue.of(value, display);
  }

  protected EnumSet<DateTimeFunction> getSupportedFunctions() {
    return EnumSet.complementOf(EnumSet.of(DateTimeFunction.DATETIME, DateTimeFunction.HOUR,
        DateTimeFunction.MINUTE));
  }

  protected long getValue(HasDateValue date) {
    switch (getFormat()) {
      case DATE:
        return date.getDate().getDays();
      case DAY:
        return date.getDom();
      case DAY_OF_WEEK:
        return date.getDow();
      case MONTH:
        return date.getMonth();
      case QUATER:
        return (date.getMonth() - 1) / 3 + 1;
      case YEAR:
        return date.getYear();
      default:
        Assert.unsupported();
    }
    return BeeConst.UNDEF;
  }

  protected boolean validate(HasDateValue date) {
    saveFilter();

    if (filter == null) {
      return true;
    }
    if (date == null) {
      return filterOperator == Operator.LT;
    }
    long value = getValue(date);

    switch (filterOperator) {
      case EQ:
        return value == filter;
      case GE:
        return value >= filter;
      case GT:
        return value > filter;
      case LE:
        return value <= filter;
      case LT:
        return value < filter;
      default:
        return false;
    }
  }

  private void renderFilter(final Flow container) {
    container.clear();

    container.add(new Button(getFormat().getCaption(), new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Global.inputWidget(getOptionsCaption(), getOptionsWidget(), new InputCallback() {
          @Override
          public void onSuccess() {
            saveOptions();
            renderFilter(container);
          }
        });
      }
    }));
    if (filterOperator == null) {
      filterOperator = Operator.EQ;
    }
    final Label operator = new Label(filterOperator.toTextString());
    operator.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        final List<Operator> operators = Arrays.asList(Operator.LT, Operator.LE, Operator.EQ,
            Operator.GE, Operator.GT);
        List<String> options = new ArrayList<>();

        for (Operator o : operators) {
          options.add(o.toTextString());
        }
        Global.choice(Localized.getConstants().operator(), null, options, new ChoiceCallback() {
          @Override
          public void onSuccess(int value) {
            filterOperator = operators.get(value);
            operator.setText(filterOperator.toTextString());
          }
        });
      }
    });
    container.add(operator);

    if (filterWidget == null) {
      filterWidget = createFilterEditor();
    }
    filterWidget.setValue(filter == null ? null : BeeUtils.toString(filter));
    container.add(filterWidget);
  }

  private void saveFilter() {
    if (filterWidget != null) {
      filter = BeeUtils.toLongOrNull(filterWidget.getNormalizedValue());
    }
  }
}
