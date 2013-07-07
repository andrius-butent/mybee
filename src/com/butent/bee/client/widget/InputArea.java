package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBoxBase;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.event.Binder;
import com.butent.bee.client.event.HasInputHandlers;
import com.butent.bee.client.event.InputHandler;
import com.butent.bee.client.ui.FormWidget;
import com.butent.bee.client.ui.HandlesAfterSave;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.edit.EditStopEvent;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.view.edit.EditorAssistant;
import com.butent.bee.client.view.edit.HasTextBox;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Resource;
import com.butent.bee.shared.State;
import com.butent.bee.shared.ui.EditorAction;
import com.butent.bee.shared.ui.HasTextDimensions;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

import java.util.Collections;
import java.util.List;

/**
 * Implements a text area that allows multiple lines of text to be entered.
 */

public class InputArea extends TextArea implements Editor, HandlesAfterSave, HasTextDimensions,
    HasInputHandlers, HasTextBox {

  private Resource resource;

  private String digest;

  private boolean nullable = true;

  private boolean editing;

  private boolean editorInitialized;

  private String options;

  private boolean handlesTabulation;
  
  public InputArea() {
    super();
    init();
  }

  public InputArea(Element element) {
    super(element);
    init();
  }
  
  public InputArea(Resource resource) {
    this();
    this.resource = resource;

    setValue(resource.getContent());
    if (resource.isReadOnly()) {
      setReadOnly(true);
    }
  }

  @Override
  public HandlerRegistration addEditStopHandler(EditStopEvent.Handler handler) {
    return addHandler(handler, EditStopEvent.getType());
  }
  
  @Override
  public HandlerRegistration addInputHandler(InputHandler handler) {
    return Binder.addInputHandler(this, handler);
  }

  @Override
  public void clearValue() {
    setValue(BeeConst.STRING_EMPTY);
  }

  @Override
  public EditorAction getDefaultFocusAction() {
    return null;
  }

  public String getDigest() {
    return digest;
  }

  @Override
  public String getId() {
    return DomUtils.getId(this);
  }

  @Override
  public String getIdPrefix() {
    return "area";
  }
  
  @Override
  public String getNormalizedValue() {
    String v = getValue();
    if (BeeUtils.isEmpty(v) && isNullable()) {
      return null;
    } else {
      return BeeUtils.trimRight(v);
    }
  }

  @Override
  public String getOptions() {
    return options;
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public TextBoxBase getTextBox() {
    return this;
  }

  @Override
  public FormWidget getWidgetType() {
    return FormWidget.INPUT_AREA;
  }

  @Override
  public boolean handlesKey(int keyCode) {
    return !BeeUtils.inList(keyCode, KeyCodes.KEY_ESCAPE, KeyCodes.KEY_TAB);
  }

  @Override
  public boolean handlesTabulation() {
    return handlesTabulation;
  }

  @Override
  public boolean isEditing() {
    return editing;
  }

  @Override
  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isOrHasPartner(Node node) {
    return getElement().equals(node);
  }

  public boolean isValueChanged() {
    String v = getValue();
    String d = getDigest();

    if (BeeUtils.isEmpty(v)) {
      return !BeeUtils.isEmpty(d);
    } else if (BeeUtils.isEmpty(d)) {
      return true;
    } else {
      return !d.equals(Codec.md5(v));
    }
  }

  @Override
  public void normalizeDisplay(String normalizedValue) {
  }

  @Override
  public void onAfterSave(String opt) {
    if (BeeUtils.isEmpty(opt)) {
      updateDigest();
    } else {
      setDigest(opt);
    }
  }
  
  @Override
  public void onBrowserEvent(Event event) {
    if (isEditing() && UiHelper.isSave(event)) {
      event.preventDefault();
      fireEvent(new EditStopEvent(State.CHANGED));
      return;
    }
    super.onBrowserEvent(event);
  }

  public void setDigest(String digest) {
    this.digest = digest;
  }

  @Override
  public void setEditing(boolean editing) {
    this.editing = editing;
  }

  @Override
  public void setHandlesTabulation(boolean handlesTabulation) {
    this.handlesTabulation = handlesTabulation;
  }

  @Override
  public void setId(String id) {
    DomUtils.setId(this, id);
  }

  @Override
  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  @Override
  public void setValue(String value) {
    super.setValue(value);
    updateDigest(getValue());
  }

  @Override
  public void startEdit(String oldValue, char charCode, EditorAction onEntry,
      Element sourceElement) {
    if (!isEditorInitialized()) {
      initEditor();
      setEditorInitialized(true);
    }

    EditorAction action = (onEntry == null) ? EditorAction.ADD_LAST : onEntry;
    EditorAssistant.doEditorAction(this, oldValue, charCode, action);
  }

  public String updateDigest() {
    return updateDigest(getValue());
  }

  public String updateDigest(String value) {
    if (BeeUtils.isEmpty(value)) {
      setDigest(null);
    } else {
      setDigest(Codec.md5(value));
    }
    return getDigest();
  }

  @Override
  public List<String> validate(boolean checkForNull) {
    return Collections.emptyList();
  }

  @Override
  public List<String> validate(String normalizedValue, boolean checkForNull) {
    return Collections.emptyList();
  }
  
  private void init() {
    DomUtils.createId(this, getIdPrefix());
    setStyleName("bee-InputArea");
  }

  private void initEditor() {
    sinkEvents(Event.ONKEYDOWN);
  }

  private boolean isEditorInitialized() {
    return editorInitialized;
  }

  private void setEditorInitialized(boolean editorInitialized) {
    this.editorInitialized = editorInitialized;
  }
}
