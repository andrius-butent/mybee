package com.butent.bee.client.view.search;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.view.edit.Editor;
import com.butent.bee.client.widget.InputLong;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.NotificationListener;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.filter.ComparisonFilter;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

public class IdFilterSupplier extends AbstractFilterSupplier {
  
  private static final int MIN_EDITOR_WIDTH = 60;
  private static final int MAX_EDITOR_WIDTH = 100;
  
  private final Editor editor;
  private int lastWidth = BeeConst.UNDEF;
  
  private Long oldValue = null;

  public IdFilterSupplier(String viewName, final BeeColumn column, String options) {
    super(viewName, column, options);
    
    this.editor = new InputLong();

    editor.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
          IdFilterSupplier.this.onSave();
        }
      }
    });
  }

  @Override
  public String getLabel() {
    return editor.getValue();
  }
  
  @Override
  public String getValue() {
    return Strings.emptyToNull(BeeUtils.trim(editor.getValue()));
  }

  @Override
  public void onRequest(Element target, NotificationListener notificationListener,
      final Callback<Boolean> callback) {
    int width = BeeUtils.clamp(target.getOffsetWidth(), MIN_EDITOR_WIDTH, MAX_EDITOR_WIDTH);
    if (width != getLastWidth()) {
      StyleUtils.setWidth(editor.asWidget(), width);
      setLastWidth(width);
    }
    
    setOldValue(BeeUtils.toLongOrNull(getValue()));
    
    openDialog(target, editor.asWidget(), callback);
    editor.setFocus(true);
  }

  @Override
  public Filter parse(String value) {
    return BeeUtils.isLong(value) ? ComparisonFilter.compareId(BeeUtils.toLong(value)) : null;
  }
  
  @Override
  public boolean reset() {
    editor.clearValue();
    return super.reset();
  }

  @Override
  public void setValue(String value) {
    editor.setValue(value);
  }

  @Override
  protected List<SupplierAction> getActions() {
    return Lists.newArrayList();
  }
  
  private int getLastWidth() {
    return lastWidth;
  }

  private Long getOldValue() {
    return oldValue;
  }

  private void onSave() {
    String value = BeeUtils.trim(editor.getValue());

    if (BeeUtils.isEmpty(value)) {
      update(getOldValue() != null);

    } else {
      Long id = BeeUtils.toLongOrNull(value);
      if (id == null) {
        Global.showError(Lists.newArrayList("Neteisinga ID reikšmė", value));
      } else {
        update(!id.equals(getOldValue()));
      }
    }
  }

  private void setLastWidth(int lastWidth) {
    this.lastWidth = lastWidth;
  }

  private void setOldValue(Long oldValue) {
    this.oldValue = oldValue;
  }
}
