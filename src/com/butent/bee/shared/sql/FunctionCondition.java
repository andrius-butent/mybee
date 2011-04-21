package com.butent.bee.shared.sql;

import com.butent.bee.shared.Assert;

import java.util.Collection;
import java.util.List;

class FunctionCondition implements IsCondition {

  private final String function;
  private final IsExpression expression;
  private final IsExpression[] values;

  public FunctionCondition(String func, IsExpression expr, IsExpression... vals) {
    Assert.notEmpty(func);
    Assert.notEmpty(expr);
    Assert.minLength(vals, 1);

    function = func;
    expression = expr;
    values = vals;
  }

  @Override
  public Collection<String> getSources() {
    return null;
  }

  @Override
  public List<Object> getSqlParams() {
    List<Object> paramList = null;

    for (IsExpression value : values) {
      paramList = (List<Object>) SqlUtils.addCollection(paramList, value.getSqlParams());
    }
    return paramList;
  }

  @Override
  public String getSqlString(SqlBuilder builder, boolean paramMode) {
    StringBuilder sb = new StringBuilder();

    sb.append(function).append("(").append(
        expression.getSqlString(builder, false));

    for (IsExpression value : values) {
      sb.append(", ").append(value.getSqlString(builder, paramMode));
    }
    return sb.append(")").toString();
  }
}
