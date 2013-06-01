package com.butent.bee.client.widget;

import com.google.gwt.dom.client.Document;

/**
 * Implements a user interface component for inserting passwords.
 */

public class InputPassword extends InputText {

  public InputPassword() {
    super(Document.get().createPasswordInputElement());
  }

  public InputPassword(int maxLength) {
    this();
    setMaxLength(maxLength);
  }
  
  @Override
  public String getIdPrefix() {
    return "pswd";
  }

  @Override
  protected String getDefaultStyleName() {
    return "bee-InputPassword";
  }
}
