/*
 * Copyright 2009 Fred Sauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.butent.bee.client.dnd;

import com.google.gwt.user.client.ui.Widget;

/**
 * Convenience interface which reduces {@link DragController#makeDraggable(Widget, Widget)} to
 * {@link DragController#makeDraggable(Widget)} for implementing classes.
 */
public interface HasDragHandle {

  /**
   * Method to return drag handle widget.
   * 
   * @return the drag handle widget
   */
  Widget getDragHandle();
}
