package com.butent.bee.server.modules.commons;

import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.utils.BeeUtils;

public final class ExchangeUtils {
  public static final String TBL_CURRENCIES = "Currencies";
  public static final String TBL_CURRENCY_RATES = "CurrencyRates";
  public static final String COL_CURRENCY_NAME = "Name";
  public static final String COL_CURRENCY = "Currency";
  public static final String COL_RATES_DATE = "Date";
  public static final String COL_RATES_QUANTITY = "Quantity";
  public static final String COL_RATES_RATE = "Rate";

  public static IsExpression exchangeField(SqlSelect query, String tbl, String amountFld,
      String currencyFld, String dateFld) {

    IsExpression date = null;

    if (!BeeUtils.isEmpty(dateFld)) {
      date = SqlUtils.field(tbl, dateFld);
    }
    return exchangeField(query,
        SqlUtils.field(tbl, amountFld), SqlUtils.field(tbl, currencyFld), date);
  }

  public static IsExpression exchangeField(SqlSelect query, IsExpression amount,
      IsExpression currency, IsExpression date) {

    Assert.notNull(amount);
    String rates = SqlUtils.uniqueName();

    addExchangeFrom(query, rates, currency, date);

    return SqlUtils.multiply(amount,
        SqlUtils.divide(SqlUtils.field(rates, COL_RATES_RATE), SqlUtils.field(rates,
            COL_RATES_QUANTITY)));
  }

  public static IsExpression exchangeFieldTo(SqlSelect query, String tbl, String amountFld,
      String currencyFld, String dateFld, Long currencyTo) {

    DataUtils.assertId(currencyTo);
    IsExpression date = null;

    if (!BeeUtils.isEmpty(dateFld)) {
      date = SqlUtils.field(tbl, dateFld);
    }
    return exchangeFieldTo(query, SqlUtils.field(tbl, amountFld), SqlUtils.field(tbl, currencyFld),
        date, SqlUtils.constant(currencyTo));
  }

  public static IsExpression exchangeFieldTo(SqlSelect query, IsExpression amount,
      IsExpression currency, IsExpression date, IsExpression currencyTo) {

    Assert.notNull(currencyTo);
    String ratesTo = SqlUtils.uniqueName();

    IsExpression xpr = SqlUtils.multiply(exchangeField(query, amount, currency, date),
        SqlUtils.divide(SqlUtils.field(ratesTo, COL_RATES_QUANTITY), SqlUtils.field(ratesTo,
            COL_RATES_RATE)));

    addExchangeFrom(query, ratesTo, currencyTo, date);

    return xpr;
  }

  public static IsExpression exchangeFieldTo(SqlSelect query, IsExpression amount,
      IsExpression date, IsExpression currencyTo) {

    Assert.notNull(currencyTo);
    String ratesTo = SqlUtils.uniqueName();

    IsExpression xpr =
        SqlUtils.multiply(amount,
            SqlUtils.divide(SqlUtils.field(ratesTo, COL_RATES_QUANTITY), SqlUtils.field(ratesTo,
                COL_RATES_RATE)));

    addExchangeFrom(query, ratesTo, currencyTo, date);

    return xpr;
  }

  private static void addExchangeFrom(SqlSelect query, String ratesAlias, IsExpression currency,
      IsExpression date) {

    Assert.noNulls(query, currency);
    IsCondition dateClause = null;

    if (date != null) {
      dateClause = SqlUtils.less(TBL_CURRENCY_RATES, COL_RATES_DATE, date);
    }
    // TODO: ORACLE workaround needed
    query.addFromLeft(TBL_CURRENCY_RATES, ratesAlias,
        SqlUtils.equals(ratesAlias, COL_CURRENCY, currency, COL_RATES_DATE,
            new SqlSelect()
                .addMax(TBL_CURRENCY_RATES, COL_RATES_DATE)
                .addFrom(TBL_CURRENCY_RATES)
                .setWhere(SqlUtils.and(dateClause,
                    SqlUtils.equals(TBL_CURRENCY_RATES, COL_CURRENCY, currency)))));
  }

  private ExchangeUtils() {
  }
}
