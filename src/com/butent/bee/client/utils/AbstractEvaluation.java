package com.butent.bee.client.utils;

public abstract class AbstractEvaluation implements Evaluator.Evaluation {

  public abstract String eval(Evaluator.Parameters parameters);

  public String getOptions() {
    return null;
  }

  public void setOptions(String options) {
  }
}
