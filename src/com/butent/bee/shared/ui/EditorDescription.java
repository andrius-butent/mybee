package com.butent.bee.shared.ui;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.HasInfo;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.Property;
import com.butent.bee.shared.utils.PropertyUtils;

import java.util.List;
import java.util.Map;

public class EditorDescription implements BeeSerializable, HasInfo {
  
  private enum SerializationMember {
    TYPE, STEP_VALUE, CHARACTER_WIDTH, VISIBLE_LINES, FORMAT, ITEMS
  }
  
  private static final String ATTR_STEP_VALUE = "stepValue";
  private static final String ATTR_CHARACTER_WIDTH = "characterWidth";
  private static final String ATTR_VISIBLE_LINES = "visibleLines";
  private static final String ATTR_FORMAT = "format";
  
  public static EditorDescription restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }
    EditorDescription editor = new EditorDescription();
    editor.deserialize(s);
    if (editor.isEmpty()) {
      return null;
    }
    return editor;
  }
  
  private EditorType type;
  
  private Integer stepValue = null;
  
  private Integer characterWidth = null;
  private Integer visibleLines = null;
  
  private String format = null;
  
  private List<String> items = null;
  
  public EditorDescription(EditorType type) {
    this.type = type;
  }
  
  private EditorDescription() {
  }

  public void deserialize(String s) {
    SerializationMember[] members = SerializationMember.values();
    String[] arr = Codec.beeDeserialize(s);
    Assert.lengthEquals(arr, members.length);

    for (int i = 0; i < members.length; i++) {
      SerializationMember member = members[i];
      String value = arr[i];
      if (BeeUtils.isEmpty(value)) {
        continue;
      }

      switch (member) {
        case TYPE:
          setType(EditorType.getByTypeCode(value));
          break;
        case STEP_VALUE:
          setStepValue(BeeUtils.toIntOrNull(value));
          break;
        case CHARACTER_WIDTH:  
          setCharacterWidth(BeeUtils.toIntOrNull(value));
          break;
        case VISIBLE_LINES:
          setVisibleLines(BeeUtils.toIntOrNull(value));
          break;
        case FORMAT:
          setFormat(value.trim());
          break;
        case ITEMS:
          if (BeeUtils.isEmpty(value)) {
            setItems(null);
          } else {
            List<String> lst = Lists.newArrayList(Codec.beeDeserialize(value));
            setItems(lst);
          }
          break;
      }
    }
  }

  public Integer getCharacterWidth() {
    return characterWidth;
  }

  public String getFormat() {
    return format;
  }

  public List<Property> getInfo() {
    List<Property> info = PropertyUtils.createProperties(
      "Type", getType(),
      "Step Value", getStepValue(),
      "Character Width", getCharacterWidth(),
      "Visible Lines", getVisibleLines(),
      "Format", getFormat());
    
    if (getItems() != null) {
      info.add(new Property("Items", BeeUtils.bracket(getItems().size())));
      for (int i = 0; i < getItems().size(); i++) {
        info.add(new Property(BeeUtils.concat(1, "Item", i + 1), getItems().get(i)));
      }
    }

    if (isEmpty()) {
      PropertyUtils.addWhenEmpty(info, getClass());
      return info;
    }
    return info;
  }
  
  public List<String> getItems() {
    return items;
  }

  public Integer getStepValue() {
    return stepValue;
  }

  public EditorType getType() {
    return type;
  }

  public Integer getVisibleLines() {
    return visibleLines;
  }

  public String serialize() {
    SerializationMember[] members = SerializationMember.values();
    Object[] arr = new Object[members.length];

    for (int i = 0; i < members.length; i++) {
      switch (members[i]) {
        case TYPE:
          arr[i] = (getType() == null) ? null : getType().getTypeCode();
          break;
        case STEP_VALUE:
          arr[i] = getStepValue();
          break;
        case CHARACTER_WIDTH:
          arr[i] = getCharacterWidth();
          break;
        case VISIBLE_LINES:
          arr[i] = getVisibleLines();
          break;
        case FORMAT:
          arr[i] = getFormat();
          break;
        case ITEMS:
          arr[i] = getItems();
          break;
      }
    }
    return Codec.beeSerializeAll(arr);
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
        
      if (BeeUtils.same(key, ATTR_STEP_VALUE)) {
        setStepValue(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_CHARACTER_WIDTH)) {
        setCharacterWidth(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_VISIBLE_LINES)) {
        setVisibleLines(BeeUtils.toIntOrNull(value));
      } else if (BeeUtils.same(key, ATTR_FORMAT)) {
        setFormat(value.trim());
      }
    }
  }

  public void setItems(List<String> items) {
    this.items = items;
  }

  private boolean isEmpty() {
    return getType() == null;
  }

  private void setCharacterWidth(Integer characterWidth) {
    this.characterWidth = characterWidth;
  }

  private void setFormat(String format) {
    this.format = format;
  }

  private void setStepValue(Integer stepValue) {
    this.stepValue = stepValue;
  }

  private void setType(EditorType type) {
    this.type = type;
  }

  private void setVisibleLines(Integer visibleLines) {
    this.visibleLines = visibleLines;
  }
}
