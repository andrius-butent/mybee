package com.butent.bee.shared.sql;

import com.butent.bee.shared.Assert;

public final class BeeConstants {

  public enum DataType {
    BOOLEAN, INTEGER, LONG, DOUBLE, NUMERIC, CHAR, STRING, DATE, DATETIME;

    public Object getEmptyValue() {
      Object value = null;

      switch (this) {
        case BOOLEAN:
        case INTEGER:
        case LONG:
        case DOUBLE:
        case NUMERIC:
        case DATE:
        case DATETIME:
          value = 0;
          break;
        case CHAR:
        case STRING:
          value = "";
          break;
        default:
          Assert.unsupported();
          break;
      }
      return value;
    }
  }

  public enum Keyword {
    DB_NAME, DB_SCHEMA, DB_TABLES, DB_FOREIGNKEYS,
    DROP_TABLE, DROP_FOREIGNKEY, CREATE_INDEX, ADD_CONSTRAINT, PRIMARYKEY, FOREIGNKEY,
    TEMPORARY, TEMPORARY_NAME, NOT_NULL, CASCADE, SET_NULL,
    BITAND, IF, CASE, CAST
  }

  public static final String FK_NAME = "fkName";
  public static final String FK_TABLE = "fkTable";
  public static final String FK_REF_TABLE = "fkRefTable";

  private BeeConstants() {
  }
}
