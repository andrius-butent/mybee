package com.butent.bee.shared.data.filter;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeSerializable;
import com.butent.bee.shared.Transformable;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.RowFilter;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Collection;
import java.util.List;

/**
 * Is an abstract class for all specific filter applying classes, determines which type of filter to
 * apply.
 */

public abstract class Filter implements BeeSerializable, Transformable, RowFilter {

  public static CompoundFilter and(Filter... filters) {
    return new CompoundFilter(CompoundType.AND, filters);
  }

  public static Filter and(Collection<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return null;
    } else if (filters.size() == 1) {
      for (Filter filter : filters) {
        return filter;
      }
      return null;
    } else {
      return and(filters.toArray(new Filter[0]));
    }
  }

  public static Filter isEmpty(String column) {
    Assert.notEmpty(column);
    return new ColumnIsEmptyFilter(column);
  }

  public static Filter isNot(Filter filter) {
    Assert.notNull(filter);
    return new CompoundFilter(CompoundType.NOT, filter);
  }

  public static Filter notEmpty(String column) {
    Assert.notEmpty(column);
    return new ColumnNotEmptyFilter(column);
  }

  public static CompoundFilter or(Filter... filters) {
    return new CompoundFilter(CompoundType.OR, filters);
  }

  public static Filter or(Collection<Filter> filters) {
    if (filters == null || filters.isEmpty()) {
      return null;
    } else if (filters.size() == 1) {
      for (Filter filter : filters) {
        return filter;
      }
      return null;
    } else {
      return or(filters.toArray(new Filter[0]));
    }
  }

  public static Filter restore(String s) {
    if (BeeUtils.isEmpty(s)) {
      return null;
    }

    String[] arr = Codec.beeDeserializeCollection(s);
    Assert.lengthEquals(arr, 2);
    String clazz = arr[0];
    String data = arr[1];
    Filter flt = null;

    if (data != null) {
      flt = Filter.getFilter(clazz);
      flt.deserialize(data);
    }
    return flt;
  }

  private static Filter getFilter(String clazz) {
    Filter flt = null;

    if (NameUtils.getClassName(ColumnValueFilter.class).equals(clazz)) {
      flt = new ColumnValueFilter();

    } else if (NameUtils.getClassName(ColumnColumnFilter.class).equals(clazz)) {
      flt = new ColumnColumnFilter();

    } else if (NameUtils.getClassName(ColumnIsEmptyFilter.class).equals(clazz)) {
      flt = new ColumnIsEmptyFilter();

    } else if (NameUtils.getClassName(ColumnNotEmptyFilter.class).equals(clazz)) {
      flt = new ColumnNotEmptyFilter();

    } else if (NameUtils.getClassName(CompoundFilter.class).equals(clazz)) {
      flt = new CompoundFilter();

    } else if (NameUtils.getClassName(IdFilter.class).equals(clazz)) {
      flt = new IdFilter();

    } else if (NameUtils.getClassName(VersionFilter.class).equals(clazz)) {
      flt = new VersionFilter();

    } else {
      Assert.unsupported("Unsupported class name: " + clazz);
    }
    return flt;
  }

  private boolean safe = true;

  protected Filter() {
    this.safe = false;
  }

  public abstract boolean involvesColumn(String colName);

  public abstract boolean isMatch(List<? extends IsColumn> columns, IsRow row);

  public String transform() {
    return toString();
  }

  protected int getColumnIndex(String colName, List<? extends IsColumn> columns) {
    return DataUtils.getColumnIndex(colName, columns);
  }

  protected String serialize(Object obj) {
    return Codec.beeSerialize(new Object[] {NameUtils.getClassName(this.getClass()), obj});
  }

  protected void setSafe() {
    Assert.isFalse(safe);
    this.safe = true;
  }
}
