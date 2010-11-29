package com.butent.bee.egg.shared.sql;

import com.butent.bee.egg.shared.sql.BeeConstants.DataTypes;
import com.butent.bee.egg.shared.sql.BeeConstants.Keywords;

class OracleSqlBuilder extends SqlBuilder {

  protected String sqlKeyword(Keywords option, Object... params) {
    switch (option) {
      case DB_TABLES:
        return new SqlSelect()
          .addFields("USER_TABLES", "TABLE_NAME")
          .addFrom("USER_TABLES").getQuery(this);
      default:
        return super.sqlKeyword(option, params);
    }
  }

  @Override
  protected String sqlQuote(String value) {
    return "\"" + value + "\"";
  }

  @Override
  protected Object sqlType(DataTypes type, int precision, int scale) {
    switch (type) {
      case BOOLEAN:
        return "NUMERIC(1)";
      case INTEGER:
        return "NUMERIC(10)";
      case LONG:
        return "NUMERIC(19)";
      case DOUBLE:
        return "BINARY_DOUBLE";
      case STRING:
        return "VARCHAR2(" + precision + ")";
      default:
        return super.sqlType(type, precision, scale);
    }
  }
}