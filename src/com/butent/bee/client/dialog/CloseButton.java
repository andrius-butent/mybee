package com.butent.bee.client.dialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.widget.Button;

/**
 * Implements a button with "close" function.
 */
public class CloseButton extends Button {

  public CloseButton(String html) {
    super(html);
    addStyleName("bee-CloseButton");
    
    addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        UiHelper.closeDialog(CloseButton.this);
      }
    });
  }
}
