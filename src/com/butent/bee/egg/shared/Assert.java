package com.butent.bee.egg.shared;

import com.butent.bee.egg.shared.exceptions.ArgumentCountException;
import com.butent.bee.egg.shared.exceptions.ArgumentTypeException;
import com.butent.bee.egg.shared.exceptions.BeeRuntimeException;
import com.butent.bee.egg.shared.exceptions.KeyNotFoundException;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.Collection;
import java.util.Map;

public class Assert {
  public static final String ASSERTION_FAILED = "[Assertion failed] - ";

  public static void arrayLength(Object arr, int size) {
    notNull(arr);
    int len = BeeUtils.arrayLength(arr);

    if (size > 0 && len != size) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "array length " + len
          + " must be equal to " + size);
    }
  }

  public static void arrayLength(Object arr, int min, int max) {
    notNull(arr);
    int len = BeeUtils.arrayLength(arr);

    if (min > 0 && len < min) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "array length " + len
          + " must be >= " + min);
    }
    if (max > 0 && len > max) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "array length " + len
          + " must be <= " + max);
    }
  }

  public static void arrayLengthMax(Object arr, int max) {
    arrayLength(arr, -1, max);
  }

  public static void arrayLengthMin(Object arr, int min) {
    arrayLength(arr, min, -1);
  }

  public static void betweenExclusive(int x, int min, int max) {
    if (!BeeUtils.betweenExclusive(x, min, max)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "argument " + x
          + " must be >= " + min + " and < " + max);
    }
  }

  public static void betweenInclusive(int x, int min, int max) {
    if (!BeeUtils.betweenInclusive(x, min, max)) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "argument " + x
          + " must be >= " + min + " and <= " + max);
    }
  }

  public static <T> void contains(Map<T, ?> map, T key) {
    notNull(map);
    notNull(key);
    if (!map.containsKey(key)) {
      throw new KeyNotFoundException(key);
    }
  }

  public static void isEven(int x) {
    isEven(x, ASSERTION_FAILED + "(" + x + ") argument must even");
  }

  public static void isEven(int x, String msg) {
    if (x % 2 == 1) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void isIndex(Collection<?> col, int idx) {
    notNull(col);
    nonNegative(idx);

    int n = col.size();

    if (n <= 0) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx
          + ", collection empty");
    } else if (idx >= n) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx
          + " must be < " + n);
    }
  }

  public static void isIndex(Collection<?> col, int idx, String msg) {
    notNull(col);
    if (idx < 0 || idx >= col.size()) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void isIndex(Object obj, int idx) {
    notNull(obj);
    nonNegative(idx);

    int n = BeeUtils.length(obj);

    if (n <= 0) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx
          + " references empty object");
    } else if (idx >= n) {
      throw new BeeRuntimeException(ASSERTION_FAILED + "index " + idx
          + " must be < " + n);
    }
  }

  public static void isIndex(Object obj, int idx, String msg) {
    notNull(obj);
    if (idx < 0 || idx >= BeeUtils.length(obj)) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void isNull(Object object) {
    isNull(object, ASSERTION_FAILED + "the object argument must be null");
  }

  public static void isNull(Object object, String message) {
    if (object != null) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void isOdd(int x) {
    isOdd(x, ASSERTION_FAILED + "(" + x + ") argument must odd");
  }

  public static void isOdd(int x, String msg) {
    if (x % 2 == 0) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void isPositive(int x) {
    isPositive(x, ASSERTION_FAILED + "(" + x + ") argument must be positive");
  }

  public static void isPositive(int x, String msg) {
    if (x <= 0) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void isString(Object obj) {
    notNull(obj);
    if (!(obj instanceof String)) {
      throw new ArgumentTypeException(obj.getClass().getName(),
          String.class.getName());
    }
  }

  public static void isTrue(boolean expression) {
    isTrue(expression, ASSERTION_FAILED + "this expression must be true");
  }

  public static void isTrue(boolean expression, String message) {
    if (!expression) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void nonNegative(int x) {
    nonNegative(x, ASSERTION_FAILED + "(" + x
        + ") argument must be non-negative");
  }

  public static void nonNegative(int x, String msg) {
    if (x < 0) {
      throw new BeeRuntimeException(msg);
    }
  }

  public static void noNullElements(String message, Object... obj) {
    for (int i = 0; i < obj.length; i++) {
      if (obj[i] == null) {
        throw new BeeRuntimeException(BeeUtils.concat(1, message,
            BeeUtils.bracket(i)));
      }
    }
  }

  public static void noNulls(Object... obj) {
    noNullElements(ASSERTION_FAILED + "arguments must not be null", obj);
  }

  public static void notEmpty(Object obj) {
    notEmpty(obj, ASSERTION_FAILED + "argument must not be null or empty");
  }

  public static void notEmpty(Object obj, String message) {
    if (BeeUtils.isEmpty(obj)) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void notNull(Object object) {
    notNull(object, ASSERTION_FAILED
        + "this argument is required; it must not be null");
  }

  public static void notNull(Object object, String message) {
    if (object == null) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void parameterCount(int c, int min) {
    parameterCount(c, min, -1);
  }

  public static void parameterCount(int c, int min, int max) {
    if (min > 0 && c < min || max > 0 && c > max) {
      throw new ArgumentCountException(c, min, max);
    }
  }

  public static void state(boolean expression) {
    state(expression, ASSERTION_FAILED + "this state invariant must be true");
  }

  public static void state(boolean expression, String message) {
    if (!expression) {
      throw new BeeRuntimeException(message);
    }
  }

  public static void unsupported() {
    unsupported("unsupported operation");
  }

  public static void unsupported(String message) {
    throw new BeeRuntimeException(message);
  }

  public static void untouchable() {
    untouchable("can't touch this");
  }

  public static void untouchable(String message) {
    throw new BeeRuntimeException(message);
  }

}
