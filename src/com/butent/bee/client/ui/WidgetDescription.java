package com.butent.bee.client.ui;

import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.ui.Calculation;
import com.butent.bee.shared.ui.ConditionalStyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WidgetDescription implements HasInfo {

  private static final String ATTR_CAPTION = "caption";
  private static final String ATTR_READ_ONLY = "readOnly";

  private static final String ATTR_SOURCE = "source";

  private static final String ATTR_MIN_VALUE = "minValue";
  private static final String ATTR_MAX_VALUE = "maxValue";

  private static final String ATTR_REL_SOURCE = "relSource";
  private static final String ATTR_REL_VIEW = "relView";
  private static final String ATTR_REL_COLUMN = "relColumn";

  private final FormWidget widgetType;
  private final String widgetId;
  
  private Collection<ConditionalStyleDeclaration> dynStyles = null;

  private String source = null;
  private Calculation calculation = null;

  private String caption = null;
  private Boolean readOnly = null;
  
  private String relSource = null;
  private String relView = null;
  private String relColumn = null;

  private Calculation validation = null;
  private Calculation editable = null;
  private Calculation carry = null;

  private String minValue = null;
  private String maxValue = null;

  public WidgetDescription(FormWidget widgetType, String widgetId) {
    this.widgetType = widgetType;
    this.widgetId = widgetId;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WidgetDescription) {
      return BeeUtils.equalsTrim(getWidgetId(), ((WidgetDescription) obj).getWidgetId());
    } else {
      return false;
    }
  }

  public Calculation getCalculation() {
    return calculation;
  }

  public String getCaption() {
    return caption;
  }

  public Calculation getCarry() {
    return carry;
  }

  public Collection<ConditionalStyleDeclaration> getDynStyles() {
    return dynStyles;
  }

  public Calculation getEditable() {
    return editable;
  }

  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
        "Widget Type", getWidgetType(),
        "Widget Id", getWidgetId(),
        "Caption", getCaption(),
        "Read Only", isReadOnly(),
        "Source", getSource(),
        "Rel Source", getRelSource(),
        "Rel View", getRelView(),
        "Rel Column", getRelColumn(),
        "Min Value", getMinValue(),
        "Max Value", getMaxValue());

    if (getValidation() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Validation", getValidation().getInfo());
    }
    if (getEditable() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Editable", getEditable().getInfo());
    }
    if (getCarry() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Carry", getCarry().getInfo());
    }
    if (getCalculation() != null) {
      PropertyUtils.appendChildrenToProperties(info, "Calculation", getCalculation().getInfo());
    }

    if (getDynStyles() != null && !getDynStyles().isEmpty()) {
      int cnt = getDynStyles().size();
      info.add(new Property("Dyn Styles", BeeUtils.bracket(cnt)));
      int i = 0;
      for (ConditionalStyleDeclaration conditionalStyle : getDynStyles()) {
        i++;
        if (conditionalStyle != null) {
          PropertyUtils.appendChildrenToProperties(info, "Dyn Style " + BeeUtils.progress(i, cnt),
              conditionalStyle.getInfo());
        }
      }
    }

    PropertyUtils.addWhenEmpty(info, getClass());
    return info;
  }

  public String getMaxValue() {
    return maxValue;
  }

  public String getMinValue() {
    return minValue;
  }

  public String getRelColumn() {
    return relColumn;
  }

  public String getRelSource() {
    return relSource;
  }

  public String getRelView() {
    return relView;
  }

  public String getSource() {
    return source;
  }

  public Calculation getValidation() {
    return validation;
  }

  public String getWidgetId() {
    return widgetId;
  }

  public FormWidget getWidgetType() {
    return widgetType;
  }

  @Override
  public int hashCode() {
    return (getWidgetId() == null) ? 0 : getWidgetId().trim().hashCode();
  }
  
  public Boolean isReadOnly() {
    return readOnly;
  }

  public void setAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return;
    }

    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String key = attribute.getKey();
      String value = attribute.getValue();
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      if (BeeUtils.same(key, ATTR_CAPTION)) {
        setCaption(value.trim());
      } else if (BeeUtils.same(key, ATTR_READ_ONLY)) {
        setReadOnly(BeeUtils.toBooleanOrNull(value));
      } else if (BeeUtils.same(key, ATTR_SOURCE)) {
        setSource(value.trim());

      } else if (BeeUtils.same(key, ATTR_MIN_VALUE)) {
        setMinValue(value.trim());
      } else if (BeeUtils.same(key, ATTR_MAX_VALUE)) {
        setMaxValue(value.trim());

      } else if (BeeUtils.same(key, ATTR_REL_SOURCE)) {
        setRelSource(value.trim());
      } else if (BeeUtils.same(key, ATTR_REL_VIEW)) {
        setRelView(value.trim());
      } else if (BeeUtils.same(key, ATTR_REL_COLUMN)) {
        setRelColumn(value.trim());
      }
    }
  }

  public void setCalculation(Calculation calculation) {
    this.calculation = calculation;
  }

  public void setCaption(String caption) {
    this.caption = caption;
  }

  public void setCarry(Calculation carry) {
    this.carry = carry;
  }

  public void setDynStyles(Collection<ConditionalStyleDeclaration> dynStyles) {
    this.dynStyles = dynStyles;
  }

  public void setEditable(Calculation editable) {
    this.editable = editable;
  }

  public void setMaxValue(String maxValue) {
    this.maxValue = maxValue;
  }

  public void setMinValue(String minValue) {
    this.minValue = minValue;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void setRelColumn(String relColumn) {
    this.relColumn = relColumn;
  }

  public void setRelSource(String relSource) {
    this.relSource = relSource;
  }

  public void setRelView(String relView) {
    this.relView = relView;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setValidation(Calculation validation) {
    this.validation = validation;
  }
}
