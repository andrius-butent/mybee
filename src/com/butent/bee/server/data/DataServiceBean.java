package com.butent.bee.server.data;

import com.butent.bee.server.DataSourceBean;
import com.butent.bee.server.communication.ResponseBuffer;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.jdbc.BeeConnection;
import com.butent.bee.server.jdbc.BeeResultSet;
import com.butent.bee.server.jdbc.BeeStatement;
import com.butent.bee.server.jdbc.JdbcConst;
import com.butent.bee.server.jdbc.JdbcUtils;
import com.butent.bee.server.utils.BeeDataSource;
import com.butent.bee.server.utils.SystemInfo;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.LogUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Manages JDBC connectivity and executes service requests with rpc_db_jdbc tag.
 */

@Stateless
public class DataServiceBean {
  private static Logger logger = Logger.getLogger(DataServiceBean.class.getName());

  @EJB
  DataSourceBean dsb;
  @EJB
  MetaDataBean mdb;
  @EJB
  ResultSetBean rsb;

  public void doService(String svc, String dsn, RequestInfo reqInfo, ResponseBuffer buff) {
    Assert.notEmpty(svc);

    BeeDataSource ds = checkDs(dsn, buff);
    if (ds == null) {
      return;
    }

    if (Service.isDbMetaService(svc)) {
      mdb.doService(svc, ds, reqInfo, buff);
    } else if (BeeUtils.same(svc, Service.DB_JDBC)) {
      testJdbc(ds.getConn(), reqInfo, buff);
    } else {
      String msg = BeeUtils.concat(1, svc, dsn, "data service not recognized");
      LogUtils.warning(logger, msg);
      buff.addWarning(msg);
    }

    ds.close();
  }

  private BeeDataSource checkDs(String dsn, ResponseBuffer buff) {
    BeeDataSource z;

    if (BeeUtils.isEmpty(dsn)) {
      z = dsb.getDefaultDs();
      if (z == null) {
        buff.addWarning("dsn not specified");
        return null;
      }
    } else {
      z = dsb.locateDs(dsn);
      if (z == null) {
        buff.addSevere(dsn, "not found");
        return null;
      }
    }

    if (!z.check()) {
      buff.addSevere("cannot open", dsn, z.getErrors());
      return null;
    }
    return z;
  }

  private void testJdbc(Connection conn, RequestInfo reqInfo, ResponseBuffer buff) {
    DateTime enter = new DateTime();

    Map<String, String> map = reqInfo.getVars();
    if (BeeUtils.isEmpty(map)) {
      buff.addSevere("Request data not found");
      return;
    }

    String sql = map.get(Service.VAR_JDBC_QUERY);
    if (BeeUtils.isEmpty(sql)) {
      buff.addSevere("Parameter", Service.VAR_JDBC_QUERY, "not found");
      return;
    }

    for (String key : map.keySet()) {
      if (BeeConst.isDefault(map.get(key))) {
        map.put(key, BeeConst.STRING_EMPTY);
      }
    }

    String cAc = map.get(Service.VAR_CONNECTION_AUTO_COMMIT);
    String cHo = map.get(Service.VAR_CONNECTION_HOLDABILITY);
    String cRo = map.get(Service.VAR_CONNECTION_READ_ONLY);
    String cTi = map.get(Service.VAR_CONNECTION_TRANSACTION_ISOLATION);

    String sCn = map.get(Service.VAR_STATEMENT_CURSOR_NAME);
    String sEp = map.get(Service.VAR_STATEMENT_ESCAPE_PROCESSING);
    String sFd = map.get(Service.VAR_STATEMENT_FETCH_DIRECTION);
    String sFs = map.get(Service.VAR_STATEMENT_FETCH_SIZE);
    String sMf = map.get(Service.VAR_STATEMENT_MAX_FIELD_SIZE);
    String sMr = map.get(Service.VAR_STATEMENT_MAX_ROWS);
    String sPo = map.get(Service.VAR_STATEMENT_POOLABLE);
    String sQt = map.get(Service.VAR_STATEMENT_QUERY_TIMEOUT);
    String sRc = map.get(Service.VAR_STATEMENT_RS_CONCURRENCY);
    String sRh = map.get(Service.VAR_STATEMENT_RS_HOLDABILITY);
    String sRt = map.get(Service.VAR_STATEMENT_RS_TYPE);

    String rFd = map.get(Service.VAR_RESULT_SET_FETCH_DIRECTION);
    String rFs = map.get(Service.VAR_RESULT_SET_FETCH_SIZE);

    String ret = map.get(Service.VAR_JDBC_RETURN);
    boolean debug = reqInfo.isDebug();

    String before = "before:";
    String after = "after:";

    boolean vb, ok;
    int v1, v2, vu;

    BeeConnection bc = new BeeConnection(conn);

    if (BeeUtils.isBoolean(cAc)) {
      vb = bc.isAutoCommit();
      bc.updateAutoCommit(conn, cAc);
      v2 = bc.getAutoCommitQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bc.getErrors());
        return;
      }

      buff.addMessage("Connection Auto Commit:", cAc, before, vb, after, BeeUtils.toBoolean(v2));
    }

    if (!BeeUtils.isEmpty(cHo)) {
      v1 = bc.getHoldability();
      bc.updateHoldability(conn, cHo);
      v2 = bc.getHoldabilityQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bc.getErrors());
        return;
      }

      buff.addMessage("Connection Holdability:", cHo,
          before, v1, JdbcUtils.holdabilityAsString(v1),
          after, v2, JdbcUtils.holdabilityAsString(v2));
    }

    if (!BeeUtils.isEmpty(cTi)) {
      v1 = bc.getTransactionIsolation();
      bc.updateTransactionIsolation(conn, cTi);
      v2 = bc.getTransactionIsolationQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bc.getErrors());
        return;
      }

      buff.addMessage("Connection Transaction Isolation:", cTi,
          before, v1, JdbcUtils.transactionIsolationAsString(v1),
          after, v2, JdbcUtils.transactionIsolationAsString(v2));
    }

    if (BeeUtils.isBoolean(cRo)) {
      vb = bc.isReadOnly();
      bc.updateReadOnly(conn, cRo);
      v2 = bc.getReadOnlyQuietly(conn);

      if (bc.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bc.getErrors());
        return;
      }

      buff.addMessage("Connection Read Only:", cRo, before, vb, after, BeeUtils.toBoolean(v2));
    }

    Statement stmt = bc.createStatement(conn, sRt, sRc, sRh);
    if (bc.hasErrors() || stmt == null) {
      bc.revert(conn);
      buff.addErrors(bc.getErrors());
      if (stmt == null) {
        buff.addMessage(Level.SEVERE, "Statement not created");
      }
      JdbcUtils.closeStatement(stmt);
      return;
    }

    BeeStatement bs = new BeeStatement(stmt);
    if (bs.hasErrors()) {
      bc.revert(conn);
      buff.addErrors(bs.getErrors());
      JdbcUtils.closeStatement(stmt);
      return;
    }

    if (!BeeUtils.allEmpty(sRt, sRc, sRh)) {
      buff.addMessage("Statement parameters:", sRt, sRc, sRh);
      buff.addMessage("Statement created:", bs.getResultSetType(),
          JdbcUtils.rsTypeAsString(bs.getResultSetType()), bs.getConcurrency(),
          JdbcUtils.concurrencyAsString(bs.getConcurrency()), bs.getHoldability(),
          JdbcUtils.holdabilityAsString(bs.getHoldability()));
    }

    if (!BeeUtils.isEmpty(sCn)) {
      if (JdbcUtils.supportsCursorName(conn)) {
        buff.addMessage("Cursor name:", sCn);
        bs.setCursorName(stmt, sCn);

        if (bs.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return;
        }

      } else {
        buff.addMessage(Level.WARNING, "Cursor name:", sCn, JdbcConst.FEATURE_NOT_SUPPORTED);
      }
    }

    if (BeeUtils.isBoolean(sEp)) {
      buff.addMessage("Escape Processing:", sEp);
      bs.setEscapeProcessing(stmt, BeeUtils.toBoolean(sEp));

      if (bs.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return;
      }
    }

    if (!BeeUtils.isEmpty(sFd)) {
      v1 = bs.getFetchDirection();
      bs.updateFetchDirection(stmt, sFd);
      v2 = bs.getFetchDirectionQuietly(stmt);

      if (bs.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return;
      }

      buff.addMessage("Statement Fetch Direction:", sFd,
          before, v1, JdbcUtils.fetchDirectionAsString(v1),
          after, v2, JdbcUtils.fetchDirectionAsString(v2));
    }

    if (!BeeUtils.isEmpty(sFs)) {
      ok = true;
      if (BeeUtils.inListSame(sFs, "min", "-")) {
        vu = Integer.MIN_VALUE;
      } else if (BeeUtils.isInt(sFs)) {
        vu = BeeUtils.toInt(sFs);
      } else {
        buff.addMessage(Level.WARNING, "Statement Fetch Size:", sFs, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getFetchSize();
        bs.updateFetchSize(stmt, vu);
        v2 = bs.getFetchSizeQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return;
        }

        buff.addMessage("Statement Fetch Size:", sFs, BeeUtils.bracket(vu), before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sMf)) {
      if (BeeUtils.isInt(sMf)) {
        vu = BeeUtils.toInt(sMf);
        ok = true;
      } else {
        buff.addMessage(Level.WARNING, "Statement Max Field Size:", sMf, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getMaxFieldSize();
        bs.updateMaxFieldSize(stmt, vu);
        v2 = bs.getMaxFieldSizeQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return;
        }

        buff.addMessage("Statement Max Field Size:", sMf, BeeUtils.bracket(vu),
            before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sMr)) {
      if (BeeUtils.isInt(sMr)) {
        vu = BeeUtils.toInt(sMr);
        ok = true;
      } else {
        buff.addMessage(Level.WARNING, "Statement Max Rows:", sMr, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getMaxRows();
        bs.updateMaxRows(stmt, vu);
        v2 = bs.getMaxRowsQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return;
        }

        buff.addMessage("Statement Max Rows:", sMr, BeeUtils.bracket(vu), before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sQt)) {
      if (BeeUtils.isInt(sQt)) {
        vu = BeeUtils.toInt(sQt);
        ok = true;
      } else {
        buff.addMessage(Level.WARNING, "Statement Query Timeout:", sQt, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        v1 = bs.getQueryTimeout();
        bs.updateQueryTimeout(stmt, vu);
        v2 = bs.getQueryTimeoutQuietly(stmt);

        if (bs.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(bs.getErrors());
          JdbcUtils.closeStatement(stmt);
          return;
        }

        buff.addMessage("Statement Query Timeout:", sQt, BeeUtils.bracket(vu),
            before, v1, after, v2);
      }
    }

    if (!BeeUtils.isEmpty(sPo)) {
      vb = bs.isPoolable();
      bs.updatePoolable(stmt, sPo);
      v2 = bs.getPoolableQuietly(stmt);

      if (bs.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(bs.getErrors());
        JdbcUtils.closeStatement(stmt);
        return;
      }

      buff.addMessage("Statement Poolable:", sPo, before, vb, after, BeeUtils.toBoolean(v2));
    }

    long memQ1 = SystemInfo.freeMemory();
    ResultSet rs = bs.executeQuery(stmt, sql);
    long memQ2 = SystemInfo.freeMemory();

    if (bs.hasErrors() || rs == null) {
      bc.revert(conn);
      buff.addErrors(bs.getErrors());
      if (rs == null) {
        buff.addMessage(Level.SEVERE, BeeUtils.bracket(sql), "result set not created");
      }
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(stmt);
      return;
    }

    buff.addWarnings(JdbcUtils.getWarnings(stmt));

    BeeResultSet br = new BeeResultSet();

    if (!BeeUtils.isEmpty(rFd)) {
      br.setFetchDirection(br.getFetchDirectionQuietly(rs));
      br.setInitialized();

      v1 = br.getFetchDirection();
      br.updateFetchDirection(rs, rFd);
      v2 = br.getFetchDirectionQuietly(rs);

      if (br.hasErrors()) {
        bc.revert(conn);
        buff.addErrors(br.getErrors());
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        return;
      }

      buff.addMessage("Result Set Fetch Direction:", rFd,
          before, v1, JdbcUtils.fetchDirectionAsString(v1),
          after, v2, JdbcUtils.fetchDirectionAsString(v2));
    }

    if (!BeeUtils.isEmpty(rFs)) {
      ok = true;
      if (BeeUtils.inListSame(rFs, "min", "-")) {
        vu = Integer.MIN_VALUE;
      } else if (BeeUtils.isInt(rFs)) {
        vu = BeeUtils.toInt(rFs);
      } else {
        buff.addMessage(Level.WARNING, "Result Set Fetch Size:", rFs, "not an integer");
        vu = BeeConst.INT_ERROR;
        ok = false;
      }

      if (ok) {
        br.setFetchSize(br.getFetchSizeQuietly(rs));
        br.setInitialized();

        v1 = br.getFetchSize();
        br.updateFetchSize(rs, vu);
        v2 = br.getFetchSizeQuietly(rs);

        if (br.hasErrors()) {
          bc.revert(conn);
          buff.addErrors(br.getErrors());
          JdbcUtils.closeResultSet(rs);
          JdbcUtils.closeStatement(stmt);
          return;
        }

        buff.addMessage("Result Set Fetch Size:", rFs, BeeUtils.bracket(vu), before, v1, after, v2);
      }
    }

    if (BeeConst.JDBC_COLUMNS.equals(ret)) {
      rsb.rsMdToResponse(rs, buff, debug);
    } else if (BeeConst.JDBC_ROW_COUNT.equals(ret)) {
      DateTime start = new DateTime();
      long memC1 = SystemInfo.freeMemory();
      int rc = JdbcUtils.getSize(rs);
      long memC2 = SystemInfo.freeMemory();
      DateTime end = new DateTime();

      buff.addLine(enter.toTimeString(), start.toTimeString(), end.toTimeString());
      buff.addLine(ret, rc, BeeUtils.bracket(BeeUtils.toSeconds(end.getTime() - start.getTime())),
          "type", JdbcUtils.getTypeInfo(rs));
      buff.addLine("memory", BeeUtils.addName("executeQuery", memQ1 - memQ2),
          BeeUtils.addName(ret, memC1 - memC2));
    } else if (BeeConst.JDBC_META_DATA.equals(ret)) {
      buff.addExtendedPropertiesColumns();
      buff.appendProperties("Connection", BeeConnection.getInfo(conn));
      buff.appendProperties("Statement", BeeStatement.getInfo(stmt));
      buff.appendProperties("Result Set", BeeResultSet.getInfo(rs));
    } else {
      rsb.rsToResponse(rs, buff, debug);
    }

    JdbcUtils.closeResultSet(rs);
    JdbcUtils.closeStatement(stmt);
    bc.revert(conn);
  }
}
