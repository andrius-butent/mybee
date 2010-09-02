package com.butent.bee.egg.server.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.butent.bee.egg.server.jdbc.BeeConnection;
import com.butent.bee.egg.server.jdbc.BeeResultSet;
import com.butent.bee.egg.server.jdbc.JdbcException;
import com.butent.bee.egg.server.jdbc.JdbcUtils;
import com.butent.bee.egg.shared.BeeConst;
import com.butent.bee.egg.shared.Transformable;

import com.butent.bee.egg.shared.utils.BeeUtils;
import com.butent.bee.egg.shared.utils.PropUtils;
import com.butent.bee.egg.shared.utils.SubProp;

public class BeeDataSource implements Transformable {
  public static final int STATUS_ERROR = -1;
  public static final int STATUS_UNKNOWN = 0;
  public static final int STATUS_OPEN = 1;
  public static final int STATUS_CLOSED = 2;

  private String tp = null;
  private DataSource ds = null;
  private Connection conn = null;

  private int status = STATUS_UNKNOWN;
  private List<SQLException> errors = new ArrayList<SQLException>();

  public BeeDataSource(String tp, DataSource ds) {
    this.tp = tp;
    this.ds = ds;
  }

  public String getTp() {
    return tp;
  }

  public DataSource getDs() {
    return ds;
  }

  public Connection getConn() {
    return conn;
  }

  public List<SQLException> getErrors() {
    return errors;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int st) {
    this.status = st;
  }

  public boolean isOpen() {
    return this.getConn() != null;
  }

  public boolean open() {
    close();

    if (this.ds != null)
      try {
        conn = ds.getConnection();
        setStatus(STATUS_OPEN);
      } catch (SQLException ex) {
        addError(ex);
      }

    return isOpen();
  }

  public void close() {
    if (isOpen()) {
      try {
        conn.close();
        setStatus(STATUS_CLOSED);
      } catch (SQLException ex) {
        addError(ex);
      }

      conn = null;
    }
  }

  public boolean check() {
    if (isOpen())
      return true;
    else if (getStatus() == STATUS_ERROR)
      return false;
    else
      return open();
  }

  private void addError(SQLException ex) {
    if (ex != null) {
      errors.add(ex);
      setStatus(STATUS_ERROR);
    }
  }

  @Override
  public String toString() {
    return BeeUtils.concat(1, "Type", tp, "DataSource", ds, "Connection", conn);
  }

  public String transform() {
    return toString();
  }

  public DatabaseMetaData getDbMd() throws JdbcException {
    if (isOpen())
      try {
        return conn.getMetaData();
      } catch (SQLException ex) {
        throw new JdbcException(ex);
      }
    else
      return null;
  }

  public String ping() {
    String r = "sp_helpserver";
    // select @@version
    // select sysutcdatetime(), sysdatetime()
    // select system_user

    // select version(), user(), database()
    // select utc_timestamp(), sysdate(), now()

    // select * from v$version
    // select user, sysdate from "adm_par"
    // sys_context('userenv', 'current_schema'), sys_context('userenv', 'lang')

    return r;
  }

  public List<SubProp> getDbInfo() throws SQLException {
    ResultSet rs, z;
    String nm, k, v, s;
    int c;
    boolean ok;

    if (!isOpen())
      return null;

    DatabaseMetaData dbMd = conn.getMetaData();
    if (BeeUtils.isEmpty(dbMd))
      return null;

    List<SubProp> lst = new ArrayList<SubProp>();

    PropUtils.addPropSub(lst, false, "DB Name", dbMd.getDatabaseProductName(),
        "DB Version", dbMd.getDatabaseProductVersion(), "Driver Name",
        dbMd.getDriverName(), "Driver Version", dbMd.getDriverVersion(),
        "JDBC Major Version", dbMd.getJDBCMajorVersion(), "JDBC Minor Version",
        dbMd.getJDBCMinorVersion(), "URL", dbMd.getURL());

    PropUtils.appendString(lst, "Connection", BeeConnection.getInfo(conn));

    PropUtils.addPropSub(lst, false, "User", dbMd.getUserName(),
        "Catalog Term", dbMd.getCatalogTerm(), "Catalog Separator",
        dbMd.getCatalogSeparator(), "Schema Term", dbMd.getSchemaTerm(),
        "Procedure Term", dbMd.getProcedureTerm(), "Extra Name Characters",
        dbMd.getExtraNameCharacters(), "Identifier Quote",
        dbMd.getIdentifierQuoteString(), "Search String Escape",
        dbMd.getSearchStringEscape(), "Max Catalog Name",
        dbMd.getMaxCatalogNameLength(), "Max Schema Name",
        dbMd.getMaxSchemaNameLength(), "Max Table Name",
        dbMd.getMaxTableNameLength(), "Max Cursor Name",
        dbMd.getMaxCursorNameLength(), "Max Column Name",
        dbMd.getMaxColumnNameLength(), "Max Procedure Name",
        dbMd.getMaxProcedureNameLength(), "Max User Name",
        dbMd.getMaxUserNameLength(), "Max Columns In Table",
        dbMd.getMaxColumnsInTable(), "Max Row Size", dbMd.getMaxRowSize(),
        "Max Columns In Index", dbMd.getMaxColumnsInIndex(),
        "Max Index Length", dbMd.getMaxIndexLength(), "Max Statement Length",
        dbMd.getMaxStatementLength(), "Max Tables In Select",
        dbMd.getMaxTablesInSelect(), "Max Columns In Select",
        dbMd.getMaxColumnsInSelect(), "Max Columns In GroupBy",
        dbMd.getMaxColumnsInGroupBy(), "Max Columns In Order By",
        dbMd.getMaxColumnsInOrderBy(), "Max Binary Literal",
        dbMd.getMaxBinaryLiteralLength(), "Max Char Literal",
        dbMd.getMaxCharLiteralLength(), "Max Connections",
        dbMd.getMaxConnections(), "Max Statements", dbMd.getMaxStatements());

    PropUtils.addPropSub(lst, false, "Default Transaction Isolation", JdbcUtils
        .transactionIsolationAsString(dbMd.getDefaultTransactionIsolation()),
        "Result Set Holdability", JdbcUtils.holdabilityAsString(dbMd
            .getResultSetHoldability()));

    PropUtils.addSplit(lst, "SQL Keywords", null, dbMd.getSQLKeywords(),
        BeeConst.STRING_COMMA);
    PropUtils.addSplit(lst, "System Functions", null,
        dbMd.getSystemFunctions(), BeeConst.STRING_COMMA);
    PropUtils.addSplit(lst, "Numeric Functions", null,
        dbMd.getNumericFunctions(), BeeConst.STRING_COMMA);
    PropUtils.addSplit(lst, "String Functions", null,
        dbMd.getStringFunctions(), BeeConst.STRING_COMMA);
    PropUtils.addSplit(lst, "Time Date Functions", null,
        dbMd.getTimeDateFunctions(), BeeConst.STRING_COMMA);

    PropUtils.addPropSub(lst, false, "All Procedures Are Callable",
        dbMd.allProceduresAreCallable(), "All Tables Are Selectable",
        dbMd.allTablesAreSelectable(),
        "Auto Commit Failure Closes All Result Sets",
        dbMd.autoCommitFailureClosesAllResultSets(),
        "Data Definition Causes Transaction Commit",
        dbMd.dataDefinitionCausesTransactionCommit(),
        "Data Definition Ignored In Transactions",
        dbMd.dataDefinitionIgnoredInTransactions(),
        "Max Row Size Include Blobs", dbMd.doesMaxRowSizeIncludeBlobs(),
        "Catalog At Start", dbMd.isCatalogAtStart(), "Read Only",
        dbMd.isReadOnly(), "Locators Update Copy", dbMd.locatorsUpdateCopy(),
        "Null Plus Non Null Is Null", dbMd.nullPlusNonNullIsNull(),
        "Nulls Are Sorted At End", dbMd.nullsAreSortedAtEnd(),
        "Nulls Are Sorted At Start", dbMd.nullsAreSortedAtStart(),
        "Nulls Are Sorted High", dbMd.nullsAreSortedHigh(),
        "Nulls Are Sorted Low", dbMd.nullsAreSortedLow(),
        "Stores Lower Case Identifiers", dbMd.storesLowerCaseIdentifiers(),
        "Stores Lower Case Quoted Identifiers",
        dbMd.storesLowerCaseQuotedIdentifiers(),
        "Stores Mixed Case Identifiers", dbMd.storesMixedCaseIdentifiers(),
        "Stores Mixed Case Quoted Identifiers",
        dbMd.storesMixedCaseQuotedIdentifiers(),
        "Stores Upper Case Identifiers", dbMd.storesUpperCaseIdentifiers(),
        "Stores Upper Case Quoted Identifiers",
        dbMd.storesUpperCaseQuotedIdentifiers(),
        "Supports Alter Table With Add Column",
        dbMd.supportsAlterTableWithAddColumn(),
        "Supports Alter Table With Drop Column",
        dbMd.supportsAlterTableWithDropColumn(),
        "Supports ANSI92 Entry Level SQL", dbMd.supportsANSI92EntryLevelSQL(),
        "Supports ANSI92 Full SQL", dbMd.supportsANSI92FullSQL(),
        "Supports ANSI92 Intermediate SQL",
        dbMd.supportsANSI92IntermediateSQL(), "Supports Batch Updates",
        dbMd.supportsBatchUpdates(), "Supports Catalogs In Data Manipulation",
        dbMd.supportsCatalogsInDataManipulation(),
        "Supports Catalogs In Index Definitions",
        dbMd.supportsCatalogsInIndexDefinitions(),
        "Supports Catalogs In PrivilegeDefinitions",
        dbMd.supportsCatalogsInPrivilegeDefinitions(),
        "Supports Catalogs In ProcedureCalls",
        dbMd.supportsCatalogsInProcedureCalls(),
        "Supports Catalogs In TableDefinitions",
        dbMd.supportsCatalogsInTableDefinitions(), "Supports Column Aliasing",
        dbMd.supportsColumnAliasing(), "Supports JDBC Convert",
        dbMd.supportsConvert(), "Supports Core SQL Grammar",
        dbMd.supportsCoreSQLGrammar(), "Supports Correlated Subqueries",
        dbMd.supportsCorrelatedSubqueries(),
        "Supports Data Definition And Data Manipulation Transactions",
        dbMd.supportsDataDefinitionAndDataManipulationTransactions(),
        "Supports Data Manipulation Transactions Only",
        dbMd.supportsDataManipulationTransactionsOnly(),
        "Supports Different Table Correlation Names",
        dbMd.supportsDifferentTableCorrelationNames(),
        "Supports Expressions In Order By",
        dbMd.supportsExpressionsInOrderBy(), "Supports Extended SQL Grammar",
        dbMd.supportsExtendedSQLGrammar(), "Supports Full Outer Joins",
        dbMd.supportsFullOuterJoins(), "Supports Get Generated Keys",
        dbMd.supportsGetGeneratedKeys(), "Supports Group By",
        dbMd.supportsGroupBy(), "Supports Group By Beyond Select",
        dbMd.supportsGroupByBeyondSelect(), "Supports Group By Unrelated",
        dbMd.supportsGroupByUnrelated(),
        "Supports Integrity Enhancement Facility",
        dbMd.supportsIntegrityEnhancementFacility(),
        "Supports Like Escape Clause", dbMd.supportsLikeEscapeClause(),
        "Supports Limited Outer Joins", dbMd.supportsLimitedOuterJoins(),
        "Supports Minimum SQL Grammar", dbMd.supportsMinimumSQLGrammar(),
        "Supports Mixed Case Identifiers", dbMd.supportsMixedCaseIdentifiers(),
        "Supports Mixed Case Quoted Identifiers",
        dbMd.supportsMixedCaseQuotedIdentifiers(),
        "Supports Multiple Open Results", dbMd.supportsMultipleOpenResults(),
        "Supports Multiple Result Sets", dbMd.supportsMultipleResultSets(),
        "Supports Multiple Transactions", dbMd.supportsMultipleTransactions(),
        "Supports Named Parameters", dbMd.supportsNamedParameters(),
        "Supports Non Nullable Columns", dbMd.supportsNonNullableColumns(),
        "Supports Open Cursors Across Commit",
        dbMd.supportsOpenCursorsAcrossCommit(),
        "Supports Open Cursors Across Rollback",
        dbMd.supportsOpenCursorsAcrossRollback(),
        "Supports Open Statements Across Commit",
        dbMd.supportsOpenStatementsAcrossCommit(),
        "Supports Open Statements Across Rollback",
        dbMd.supportsOpenStatementsAcrossRollback(),
        "Supports Order By Unrelated", dbMd.supportsOrderByUnrelated(),
        "Supports Outer Joins", dbMd.supportsOuterJoins(),
        "Supports Positioned Delete", dbMd.supportsPositionedDelete(),
        "Supports Positioned Update", dbMd.supportsPositionedUpdate(),
        "Supports Savepoints", dbMd.supportsSavepoints(),
        "Supports Schemas In Data Manipulation",
        dbMd.supportsSchemasInDataManipulation(),
        "Supports Schemas In Index Definitions",
        dbMd.supportsSchemasInIndexDefinitions(),
        "Supports Schemas In Privilege Definitions",
        dbMd.supportsSchemasInPrivilegeDefinitions(),
        "Supports Schemas In Procedure Calls",
        dbMd.supportsSchemasInProcedureCalls(),
        "Supports Schemas In Table Definitions",
        dbMd.supportsSchemasInTableDefinitions(), "Supports Select For Update",
        dbMd.supportsSelectForUpdate(), "Supports Statement Pooling",
        dbMd.supportsStatementPooling(),
        "Supports Stored Functions Using Call Syntax",
        dbMd.supportsStoredFunctionsUsingCallSyntax(),
        "Supports Stored Procedures", dbMd.supportsStoredProcedures(),
        "Supports Subqueries In Comparisons",
        dbMd.supportsSubqueriesInComparisons(),
        "Supports Subqueries In Exists", dbMd.supportsSubqueriesInExists(),
        "Supports Subqueries In Ins", dbMd.supportsSubqueriesInIns(),
        "Supports Subqueries In Quantifieds",
        dbMd.supportsSubqueriesInQuantifieds(),
        "Supports Table Correlation Names",
        dbMd.supportsTableCorrelationNames(), "Supports Transactions",
        dbMd.supportsTransactions(), "Supports Union", dbMd.supportsUnion(),
        "Supports Union All", dbMd.supportsUnionAll(),
        "Uses Local File Per Table", dbMd.usesLocalFilePerTable(),
        "Uses Local Files", dbMd.usesLocalFiles());

    rs = dbMd.getClientInfoProperties();
    while (rs.next()) {
      k = rs.getString("NAME");
      if (BeeUtils.isEmpty(k))
        continue;

      v = BeeUtils.concat(null, BeeUtils.addName("Max", rs.getInt("MAX_LEN")),
          BeeUtils.addName("Default", rs.getString("DEFAULT_VALUE")),
          rs.getString("DESCRIPTION"));

      PropUtils.addSub(lst, "Client Property", k,
          BeeUtils.ifString(v, "(empty)"));
    }
    rs.close();

    rs = dbMd.getFunctions(null, null, null);
    c = JdbcUtils.getSize(rs);
    rs.close();

    PropUtils.addSub(lst, "Functions", null, BeeUtils.bracket(c));

    if (BeeUtils.between(c, 1, 100)) {
      rs = dbMd.getFunctions(null, null, null);
      while (rs.next()) {
        k = rs.getString("FUNCTION_NAME");
        if (BeeUtils.isEmpty(k))
          continue;

        v = BeeUtils.concat(null,
            BeeUtils.addName("Cat", rs.getString("FUNCTION_CAT")),
            BeeUtils.addName("Schem", rs.getString("FUNCTION_SCHEM")));

        PropUtils.addSub(lst, "Function", k, BeeUtils.ifString(v, k));
      }
      rs.close();
    }

    rs = dbMd.getProcedures(null, null, null);
    c = JdbcUtils.getSize(rs);
    rs.close();

    PropUtils.addSub(lst, "Procedures", null, BeeUtils.bracket(c));

    if (BeeUtils.between(c, 1, 100)) {
      rs = dbMd.getProcedures(null, null, null);
      while (rs.next()) {
        k = rs.getString("PROCEDURE_NAME");
        if (BeeUtils.isEmpty(k))
          continue;

        v = BeeUtils.concat(null,
            BeeUtils.addName("Cat", rs.getString("PROCEDURE_CAT")),
            BeeUtils.addName("Schem", rs.getString("PROCEDURE_SCHEM")));

        PropUtils.addSub(lst, "Procedure", k, BeeUtils.ifString(v, k));
      }
      rs.close();
    }

    rs = dbMd.getCatalogs();
    while (rs.next()) {
      k = rs.getString(1);
      if (BeeUtils.isEmpty(k))
        continue;

      z = dbMd.getTables(k, null, null, null);
      c = JdbcUtils.getSize(z);
      z.close();

      PropUtils.addSub(lst, "Catalog", k, c);
    }
    rs.close();

    ok = true;

    rs = dbMd.getSchemas();
    while (rs.next()) {
      k = rs.getString("TABLE_SCHEM");
      if (BeeUtils.isEmpty(k))
        continue;

      try {
        s = rs.getString("TABLE_CATALOG");
        z = dbMd.getTables(s, k, null, null);
        c = JdbcUtils.getSize(z);
        z.close();

        v = "Table count " + (c > 0 ? BeeUtils.transform(c) : "unknown");
      } catch (Exception ex) {
        s = null;
        v = ex.getMessage();
        ok = false;
      }

      PropUtils.addSub(lst, "Schema",
          k + (s == null ? "" : " Catalog " + s.trim()), v);
    }
    rs.close();

    if (!ok) {
      rs = dbMd.getSchemas();

      List<SubProp> rsInf = new BeeResultSet(rs).getRsInfo();
      if (!BeeUtils.isEmpty(rsInf))
        for (SubProp el : rsInf)
          PropUtils.addSub(lst, BeeUtils.concat(1, "getSchemas", el.getName()),
              el.getSub(), el.getValue());

      PropUtils.appendString(lst, "getSchemas Data", JdbcUtils.getRs(rs));

      rs.close();
    }

    rs = dbMd.getTableTypes();
    while (rs.next()) {
      k = rs.getString(1);
      if (BeeUtils.isEmpty(k))
        continue;

      z = dbMd.getTables(null, null, null, new String[] { k.trim() });
      c = JdbcUtils.getSize(z);
      z.close();

      PropUtils.addSub(lst, "Table Type", k, c);
    }
    rs.close();

    nm = "Type";

    rs = dbMd.getTypeInfo();
    while (rs.next()) {
      k = rs.getString("TYPE_NAME");
      if (BeeUtils.isEmpty(k))
        continue;

      c = rs.getInt("DATA_TYPE");

      v = BeeUtils.concat(1, BeeUtils.addName("sql.Type", c),
          JdbcUtils.getJdbcTypeName(c));
      PropUtils.addSub(lst, nm, k, v);

      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Precision", rs.getInt("PRECISION")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Prefix", rs.getString("LITERAL_PREFIX")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Suffix", rs.getString("LITERAL_SUFFIX")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Create", rs.getString("CREATE_PARAMS")));

      switch (rs.getInt("NULLABLE")) {
      case (DatabaseMetaData.typeNoNulls): {
        s = BeeConst.NO;
        break;
      }
      case (DatabaseMetaData.typeNullable): {
        s = BeeConst.YES;
        break;
      }
      default:
        s = null;
      }
      PropUtils.addSub(lst, nm, k, BeeUtils.addName("Nullable", s));

      PropUtils.addSub(lst, nm, k, BeeUtils.addName("Case sensitive",
          rs.getBoolean("CASE_SENSITIVE") ? BeeConst.YES : BeeConst.NO));

      switch (rs.getInt("SEARCHABLE")) {
      case (DatabaseMetaData.typePredNone): {
        s = BeeConst.NO;
        break;
      }
      case (DatabaseMetaData.typeSearchable): {
        s = BeeConst.YES;
        break;
      }
      case (DatabaseMetaData.typePredBasic): {
        s = "except LIKE";
        break;
      }
      case (DatabaseMetaData.typePredChar): {
        s = "only LIKE";
        break;
      }
      default:
        s = null;
      }
      PropUtils.addSub(lst, nm, k, BeeUtils.addName("Searchable", s));

      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Unsigned", rs.getBoolean("UNSIGNED_ATTRIBUTE")));
      PropUtils.addSub(
          lst,
          nm,
          k,
          BeeUtils.addName("Fixed prec scale",
              rs.getBoolean("FIXED_PREC_SCALE")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Auto increment", rs.getBoolean("AUTO_INCREMENT")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Local name", rs.getString("LOCAL_TYPE_NAME")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Min scale", rs.getShort("MINIMUM_SCALE")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Max scale", rs.getShort("MAXIMUM_SCALE")));
      PropUtils.addSub(lst, nm, k,
          BeeUtils.addName("Num prec radix", rs.getInt("NUM_PREC_RADIX")));
    }
    rs.close();

    return lst;
  }

}
