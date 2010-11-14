package com.butent.bee.egg.shared.sql;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.sql.BeeConstants.DataTypes;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class SqlCreate extends SqlQuery<SqlCreate> {

  public class SqlField {
    private final IsExpression name;
    private final DataTypes type;
    private final int precission;
    private final int scale;
    private final IsSql[] options;

    private SqlField(String name, DataTypes type, int precission, int scale, IsSql... options) {
      Assert.notEmpty(name);
      Assert.notEmpty(type);

      this.name = SqlUtils.field(name);
      this.type = type;
      this.precission = precission;
      this.scale = scale;

      List<IsSql> opts = new ArrayList<IsSql>();

      for (IsSql opt : options) {
        if (!BeeUtils.isEmpty(opt)) {
          opts.add(opt);
        }
      }
      this.options = opts.toArray(new IsSql[0]);
    }

    public IsExpression getName() {
      return name;
    }

    public IsSql[] getOptions() {
      return options;
    }

    public int getPrecission() {
      return precission;
    }

    public int getScale() {
      return scale;
    }

    public DataTypes getType() {
      return type;
    }
  }

  private final IsFrom target;
  private List<SqlField> fieldList = new ArrayList<SqlField>();
  private SqlSelect source;

  public SqlCreate(String target) {
    this.target = new FromSingle(target);
  }

  public SqlCreate addBoolean(String field, IsSql... options) {
    return addField(field, DataTypes.BOOLEAN, 0, 0, options);
  }

  public SqlCreate addChar(String field, int precission, IsSql... options) {
    Assert.nonNegative(precission);
    return addField(field, DataTypes.CHAR, precission, 0, options);
  }

  public SqlCreate addDouble(String field, int precission, int scale, IsSql... options) {
    Assert.nonNegative(precission);
    Assert.nonNegative(scale);
    return addField(field, DataTypes.DOUBLE, precission, scale, options);
  }

  public SqlCreate addField(String field, DataTypes type, int precission, int scale,
      IsSql... options) {
    Assert.state(BeeUtils.isEmpty(source));
    Assert.notEmpty(field);
    Assert.state(!hasField(field), "Field " + field + " already exist");

    fieldList.add(new SqlField(field, type, precission, scale, options));

    return getReference();
  }

  public SqlCreate addInt(String field, IsSql... options) {
    return addField(field, DataTypes.INTEGER, 0, 0, options);
  }

  public SqlCreate addLong(String field, IsSql... options) {
    return addField(field, DataTypes.LONG, 0, 0, options);
  }

  public SqlCreate addNumeric(String field, int precission, int scale, IsSql... options) {
    Assert.nonNegative(precission);
    Assert.nonNegative(scale);
    return addField(field, DataTypes.NUMERIC, precission, scale, options);
  }

  public SqlCreate addString(String field, int precission, IsSql... options) {
    Assert.nonNegative(precission);
    return addField(field, DataTypes.STRING, precission, 0, options);
  }

  public SqlField getField(String field) {
    for (SqlField fld : fieldList) {
      if (BeeUtils.same(((FieldExpression) fld.getName()).getField(), field)) {
        return fld;
      }
    }
    return null;
  }

  public List<SqlField> getFields() {
    return fieldList;
  }

  public SqlSelect getSource() {
    return source;
  }

  @Override
  public List<Object> getSqlParams() {
    Assert.state(!isEmpty());

    List<Object> paramList = null;

    if (!BeeUtils.isEmpty(source)) {
      SqlUtils.addParams(paramList, source.getSqlParams());
    }
    return paramList;
  }

  @Override
  public String getSqlString(SqlBuilder builder, boolean paramMode) {
    Assert.notEmpty(builder);
    return builder.getCreate(this, paramMode);
  }

  public IsFrom getTarget() {
    return target;
  }

  public boolean hasField(String field) {
    return !BeeUtils.isEmpty(getField(field));
  }

  @Override
  public boolean isEmpty() {
    return BeeUtils.isEmpty(target) ||
        (BeeUtils.isEmpty(fieldList) && BeeUtils.isEmpty(source));
  }

  public SqlCreate setSource(SqlSelect query) {
    Assert.notNull(query);
    Assert.state(!query.isEmpty());
    Assert.state(BeeUtils.isEmpty(fieldList));

    source = query;

    return getReference();
  }

  @Override
  protected SqlCreate getReference() {
    return this;
  }
}
