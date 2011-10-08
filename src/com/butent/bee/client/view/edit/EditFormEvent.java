package com.butent.bee.client.view.edit;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import com.butent.bee.shared.State;

public class EditFormEvent extends GwtEvent<EditFormEvent.Handler> {

  public interface Handler extends EventHandler {
    void onEditForm(EditFormEvent event);
  }

  private static final Type<Handler> TYPE = new Type<Handler>();

  public static Type<Handler> getType() {
    return TYPE;
  }

  private final State state;

  public EditFormEvent(State state) {
    this.state = state;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public State getState() {
    return state;
  }

  public boolean isClosing() {
    return State.CLOSED.equals(getState());
  }

  public boolean isOpening() {
    return State.OPEN.equals(getState());
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onEditForm(this);
  }
}
