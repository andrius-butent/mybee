package com.butent.bee.egg.server.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.butent.bee.egg.shared.Assert;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.LogUtils;
import com.butent.bee.egg.shared.utils.PropUtils;
import com.butent.bee.egg.shared.utils.StringProp;

public class BeeConnection {
  private static final Logger logger = Logger.getLogger(BeeConnection.class
      .getName());

  private boolean autoCommit;
  private String catalog;
  private Properties clientInfo;
  private int holdability;
  private int transactionIsolation;
  private Map<String, Class<?>> typeMap;
  private boolean readOnly;

  private int state = BeeConst.STATE_UNKNOWN;
  private List<SQLException> errors = new ArrayList<SQLException>();

  public static List<StringProp> getInfo(Connection conn) {
    Assert.notNull(conn);
    List<StringProp> lst = new ArrayList<StringProp>();

    try {
      PropUtils.addString(lst, "Auto Commit", conn.getAutoCommit(), "Catalog",
          conn.getCatalog(), "Holdability", JdbcUtils.holdabilityAsString(conn
              .getHoldability()), "Transaction Isolation", JdbcUtils
              .transactionIsolationAsString(conn.getTransactionIsolation()),
          "Read Only", conn.isReadOnly());

      Properties prp = conn.getClientInfo();
      if (!BeeUtils.isEmpty(prp))
        for (String p : prp.stringPropertyNames())
          PropUtils.addString(lst, "Client Info",
              BeeUtils.addName(p, prp.getProperty(p)));

      Map<String, Class<?>> tm = conn.getTypeMap();
      if (!BeeUtils.isEmpty(tm))
        for (Map.Entry<String, Class<?>> me : tm.entrySet())
          PropUtils.addString(lst, "Type Map",
              BeeUtils.addName(me.getKey(), me.getValue().toString()));

      SQLWarning warn = conn.getWarnings();
      if (warn != null) {
        List<String> wLst = JdbcUtils.unchain(warn);
        for (String w : wLst)
          PropUtils.addString(lst, "Warning", w);
      }
    } catch (SQLException ex) {
      LogUtils.warning(logger, ex);
    }

    return lst;
  }

  protected BeeConnection() {
    super();
  }

  public BeeConnection(Connection conn) {
    setConnectionInfo(conn);
  }

  public void setConnectionInfo(Connection conn) {
    try {
      autoCommit = conn.getAutoCommit();
      catalog = conn.getCatalog();
      clientInfo = conn.getClientInfo();
      holdability = conn.getHoldability();
      transactionIsolation = conn.getTransactionIsolation();
      typeMap = conn.getTypeMap();
      readOnly = conn.isReadOnly();
      state = BeeConst.STATE_INITIALIZED;
    } catch (SQLException ex) {
      handleError(ex);
    }
  }

  public boolean isAutoCommit() {
    return autoCommit;
  }

  public void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  public String getCatalog() {
    return catalog;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public Properties getClientInfo() {
    return clientInfo;
  }

  public void setClientInfo(Properties clientInfo) {
    this.clientInfo = clientInfo;
  }

  public int getHoldability() {
    return holdability;
  }

  public void setHoldability(int holdability) {
    this.holdability = holdability;
  }

  public int getTransactionIsolation() {
    return transactionIsolation;
  }

  public void setTransactionIsolation(int transactionIsolation) {
    this.transactionIsolation = transactionIsolation;
  }

  public Map<String, Class<?>> getTypeMap() {
    return typeMap;
  }

  public void setTypeMap(Map<String, Class<?>> typeMap) {
    this.typeMap = typeMap;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public List<SQLException> getErrors() {
    return errors;
  }

  public void setErrors(List<SQLException> errors) {
    this.errors = errors;
  }

  public Statement createStatement(Connection conn, Object... opt) {
    Assert.notNull(conn);
    Statement stmt = null;

    int n = opt.length;
    if (n == 0) {
      try {
        stmt = conn.createStatement();
      } catch (SQLException ex) {
        handleError(ex);
      }
      return stmt;
    }

    int type = JdbcConst.UNKNOWN_RESULT_SET_TYPE;
    int concur = JdbcConst.UNKNOWN_CONCURRENCY;
    int hold = JdbcConst.UNKNOWN_HOLDABILITY;

    int z;
    String s;

    for (int i = 0; i < n; i++) {
      if (opt[i] instanceof Number) {
        z = ((Number) opt[i]).intValue();

        if (JdbcUtils.validRsType(z))
          type = z;
        else if (JdbcUtils.validConcurrency(z))
          concur = z;
        else if (JdbcUtils.validHoldability(z))
          hold = z;
      } else if (opt[i] instanceof String) {
        s = ((String) opt[i]).trim();
        if (BeeUtils.isEmpty(s) || BeeConst.isDefault(s))
          continue;

        if (JdbcUtils.validRsType(s))
          type = JdbcUtils.rsTypeFromString(s);
        else if (JdbcUtils.validConcurrency(s))
          concur = JdbcUtils.concurrencyFromString(s);
        else if (JdbcUtils.validHoldability(s))
          hold = JdbcUtils.holdabilityFromString(s);
      }
    }

    try {
      if (type == JdbcConst.UNKNOWN_RESULT_SET_TYPE
          && concur == JdbcConst.UNKNOWN_CONCURRENCY
          && hold == JdbcConst.UNKNOWN_HOLDABILITY)
        stmt = conn.createStatement();
      else {
        if (type == JdbcConst.UNKNOWN_RESULT_SET_TYPE)
          type = JdbcConst.DEFAULT_RESULT_SET_TYPE;
        if (concur == JdbcConst.UNKNOWN_CONCURRENCY)
          concur = JdbcConst.DEFAULT_CONCURRENCY;

        if (hold == JdbcConst.UNKNOWN_HOLDABILITY)
          stmt = conn.createStatement(type, concur);
        else
          stmt = conn.createStatement(type, concur, hold);
      }
    } catch (SQLException ex) {
      handleError(ex);
    }

    return stmt;
  }

  public boolean isModified(Connection conn) {
    Assert.notNull(conn);
    Assert.state(validState());

    if (!checkAutoCommit(conn))
      return true;
    else if (!checkHoldability(conn))
      return true;
    else if (!checkReadOnly(conn))
      return true;
    else if (!checkTransactionIsolation(conn))
      return true;
    else
      return false;
  }

  public void revert(Connection conn) {
    Assert.notNull(conn);
    Assert.state(validState());

    if (!hasState(BeeConst.STATE_CHANGED))
      return;

    try {
      if (conn.getAutoCommit() != isAutoCommit())
        conn.setAutoCommit(isAutoCommit());
      if (conn.getHoldability() != getHoldability())
        conn.setHoldability(getHoldability());
      if (conn.getTransactionIsolation() != getTransactionIsolation())
        conn.setTransactionIsolation(getTransactionIsolation());
      if (conn.isReadOnly() != isReadOnly())
        conn.setReadOnly(isReadOnly());
    } catch (SQLException ex) {
      handleError(ex);
    }
  }

  public boolean updateAutoCommit(Connection conn, String s) {
    Assert.notNull(conn);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    if (!BeeUtils.isBoolean(s)) {
      LogUtils.warning(logger, "unrecognized auto commit value", s);
      return false;
    }

    return updateAutoCommit(conn, BeeUtils.toBoolean(s));
  }

  public boolean updateHoldability(Connection conn, String s) {
    Assert.notNull(conn);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    int hold = JdbcUtils.holdabilityFromString(s);
    if (!JdbcUtils.validHoldability(hold)) {
      LogUtils.warning(logger, "unrecognized holdability", s);
      return false;
    }

    return updateHoldability(conn, hold);
  }

  public boolean updateTransactionIsolation(Connection conn, String s) {
    Assert.notNull(conn);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    int ti = JdbcUtils.transactionIsolationFromString(s);
    if (!JdbcUtils.validHoldability(ti)) {
      LogUtils.warning(logger, "unrecognized transaction isolation", s);
      return false;
    }

    return updateTransactionIsolation(conn, ti);
  }

  public boolean updateReadOnly(Connection conn, String s) {
    Assert.notNull(conn);
    Assert.notEmpty(s);
    Assert.state(validState());

    if (BeeConst.isDefault(s))
      return true;
    if (!BeeUtils.isBoolean(s)) {
      LogUtils.warning(logger, "unrecognized read only value", s);
      return false;
    }

    return updateReadOnly(conn, BeeUtils.toBoolean(s));
  }

  public int getAutoCommitQuietly(Connection conn) {
    if (conn == null) {
      noConnection();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = BeeUtils.toInt(conn.getAutoCommit());
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getHoldabilityQuietly(Connection conn) {
    if (conn == null) {
      noConnection();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = conn.getHoldability();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getTransactionIsolationQuietly(Connection conn) {
    if (conn == null) {
      noConnection();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = conn.getTransactionIsolation();
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public int getReadOnlyQuietly(Connection conn) {
    if (conn == null) {
      noConnection();
      return BeeConst.INT_ERROR;
    }

    int z;

    try {
      z = BeeUtils.toInt(conn.isReadOnly());
    } catch (SQLException ex) {
      handleError(ex);
      z = BeeConst.INT_ERROR;
    }

    return z;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  private boolean updateAutoCommit(Connection conn, boolean ac) {
    if (ac == isAutoCommit())
      return true;
    boolean ok;

    try {
      conn.setAutoCommit(ac);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean updateHoldability(Connection conn, int hold) {
    if (hold == getHoldability())
      return true;
    boolean ok;

    try {
      conn.setHoldability(hold);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean updateTransactionIsolation(Connection conn, int ti) {
    if (ti == getTransactionIsolation())
      return true;
    boolean ok;

    try {
      conn.setTransactionIsolation(ti);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean updateReadOnly(Connection conn, boolean ro) {
    if (ro == isReadOnly())
      return true;
    boolean ok;

    try {
      conn.setReadOnly(ro);
      addState(BeeConst.STATE_CHANGED);
      ok = true;
    } catch (SQLException ex) {
      handleError(ex);
      ok = false;
    }

    return ok;
  }

  private boolean checkAutoCommit(Connection conn) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (conn.getAutoCommit() == isAutoCommit());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkHoldability(Connection conn) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (conn.getHoldability() == getHoldability());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkTransactionIsolation(Connection conn) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (conn.getTransactionIsolation() == getTransactionIsolation());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private boolean checkReadOnly(Connection conn) {
    if (!validState())
      return true;
    boolean ok;

    try {
      ok = (conn.isReadOnly() == isReadOnly());
    } catch (SQLException ex) {
      handleError(ex);
      ok = true;
    }

    return ok;
  }

  private void handleError(SQLException ex) {
    errors.add(ex);
    LogUtils.error(logger, ex);
    addState(BeeConst.STATE_ERROR);
  }

  private void noConnection() {
    handleError(new SQLException("connection not available"));
  }

  private void addState(int st) {
    this.state &= st;
  }

  private boolean hasState(int st) {
    return (state & st) != 0;
  }

  private boolean validState() {
    return (state & BeeConst.STATE_INITIALIZED) != 0
        && (state & BeeConst.STATE_ERROR) == 0;
  }

}
