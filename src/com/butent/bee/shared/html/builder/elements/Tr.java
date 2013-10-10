package com.butent.bee.shared.html.builder.elements;

import com.butent.bee.shared.html.builder.FertileElement;
import com.butent.bee.shared.html.builder.Node;

import java.util.List;

public class Tr extends FertileElement {

  public Tr() {
    super();
  }

  public Tr addClass(String value) {
    super.addClassName(value);
    return this;
  }

  public Tr append(List<Node> nodes) {
    super.appendChildren(nodes);
    return this;
  }

  public Tr append(Node... nodes) {
    super.appendChildren(nodes);
    return this;
  }

  

  public Tr id(String value) {
    setId(value);
    return this;
  }

  public Tr insert(int index, Node child) {
    super.insertChild(index, child);
    return this;
  }

  public Tr lang(String value) {
    setLang(value);
    return this;
  }

  public Tr remove(Node child) {
    super.removeChild(child);
    return this;
  }

  public Tr text(String text) {
    super.appendText(text);
    return this;
  }

  public Tr title(String value) {
    setTitle(value);
    return this;
  }
}
