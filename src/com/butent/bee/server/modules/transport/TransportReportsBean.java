package com.butent.bee.server.modules.transport;

import com.google.common.collect.ImmutableMap;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;
import static com.butent.bee.shared.modules.trade.TradeConstants.COL_TRADE_VAT;
import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.server.data.QueryServiceBean;
import com.butent.bee.server.data.SystemBean;
import com.butent.bee.server.http.RequestInfo;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.administration.ExchangeUtils;
import com.butent.bee.server.modules.trade.TradeModuleBean;
import com.butent.bee.server.sql.HasConditions;
import com.butent.bee.server.sql.IsCondition;
import com.butent.bee.server.sql.IsExpression;
import com.butent.bee.server.sql.SqlSelect;
import com.butent.bee.server.sql.SqlUpdate;
import com.butent.bee.server.sql.SqlUtils;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SqlConstants;
import com.butent.bee.shared.report.ReportInfo;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

@Stateless
@LocalBean
public class TransportReportsBean {

  @EJB
  SystemBean sys;
  @EJB
  QueryServiceBean qs;
  @EJB
  ParamHolderBean prm;

  /**
   * Return SqlSelect query, calculating cargo incomes from CargoServices table.
   *
   * @param cargos query filter with <b>unique</b> "Cargo" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return query with columns: "Cargo", "CargoIncome", "CargoVat", "ServicesIncome", "ServicesVat"
   */
  public SqlSelect getCargoIncomeQuery(SqlSelect cargos, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();

    SqlSelect ss = new SqlSelect()
        .addField(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO), COL_CARGO)
        .addFrom(TBL_ORDER_CARGO)
        .addFromInner(cargos, alias, sys.joinTables(TBL_ORDER_CARGO, alias, COL_CARGO))
        .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
        .addFromLeft(TBL_CARGO_INCOMES,
            sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_INCOMES, COL_CARGO))
        .addFromLeft(TBL_SERVICES, sys.joinTables(TBL_SERVICES, TBL_CARGO_INCOMES, COL_SERVICE))
        .addGroup(TBL_ORDER_CARGO, sys.getIdName(TBL_ORDER_CARGO));

    IsExpression amountExpr = SqlUtils.field(TBL_CARGO_INCOMES, COL_AMOUNT);
    IsExpression vatExpr = TradeModuleBean.getVatExpression(TBL_CARGO_INCOMES, amountExpr);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_CARGO_INCOMES, amountExpr);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_CARGO_INCOMES, amountExpr);
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_CARGO_INCOMES, COL_CURRENCY);
    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_CARGO_INCOMES, COL_DATE),
        SqlUtils.field(TBL_ORDERS, COL_DATE));
    IsCondition isService = SqlUtils.isNull(TBL_SERVICES, COL_TRANSPORTATION);

    IsExpression cargoIncome = SqlUtils.sqlIf(isService, null, amountExpr);
    IsExpression cargoVat = SqlUtils.sqlIf(isService, null, vatExpr);
    IsExpression servicesIncome = SqlUtils.sqlIf(isService, amountExpr, null);
    IsExpression servicesVat = SqlUtils.sqlIf(isService, vatExpr, null);

    if (DataUtils.isId(currency)) {
      cargoIncome = ExchangeUtils.exchangeFieldTo(ss, cargoIncome, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
      cargoVat = ExchangeUtils.exchangeFieldTo(ss, cargoVat, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
      servicesIncome = ExchangeUtils.exchangeFieldTo(ss, servicesIncome, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
      servicesVat = ExchangeUtils.exchangeFieldTo(ss, servicesVat, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      cargoIncome = ExchangeUtils.exchangeField(ss, cargoIncome, currencyExpr, dateExpr);
      cargoVat = ExchangeUtils.exchangeField(ss, cargoVat, currencyExpr, dateExpr);
      servicesIncome = ExchangeUtils.exchangeField(ss, servicesIncome, currencyExpr, dateExpr);
      servicesVat = ExchangeUtils.exchangeField(ss, servicesVat, currencyExpr, dateExpr);
    }
    ss.addSum(cargoIncome, "CargoIncome")
        .addSum(cargoVat, "CargoVat")
        .addSum(servicesIncome, "ServicesIncome")
        .addSum(servicesVat, "ServicesVat");

    return ss;
  }

  /**
   * Returns Temporary table name with calculated trip income or cargo cost percents.
   *
   * @param key "Cargo" or "Trip"
   * @param filter query filter with <b>unique</b> key values.
   * @return Temporary table name with following structure: <br>
   * "Cargo" - cargo ID <br>
   * "Trip" - trip ID <br>
   * key == "Cargo" ? "TripPercent" : "CargoPercent"
   */
  public String getCargoTripPercents(String key, SqlSelect filter) {
    String percent = null;

    switch (key) {
      case COL_CARGO:
        percent = COL_TRIP_PERCENT;
        break;
      case COL_TRIP:
        percent = COL_CARGO_PERCENT;
        break;
      default:
        Assert.unsupported(key + ": only " + COL_CARGO + " and " + COL_TRIP + " values supported");
    }
    String alias = SqlUtils.uniqueName();

    String tmpCargoTrip = qs.sqlCreateTemp(new SqlSelect()
        .addFields(TBL_CARGO_TRIPS, COL_CARGO, COL_TRIP, percent)
        .addSum(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS)
        .addFrom(TBL_CARGO_TRIPS)
        .addFromInner(filter, alias, SqlUtils.joinUsing(TBL_CARGO_TRIPS, alias, key))
        .addFromLeft(TBL_TRIP_ROUTES,
            sys.joinTables(TBL_CARGO_TRIPS, TBL_TRIP_ROUTES, COL_ROUTE_CARGO))
        .addGroup(TBL_CARGO_TRIPS, COL_CARGO, COL_TRIP, percent));

    // Too big percent correction
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.multiply(
            SqlUtils.divide(SqlUtils.field(tmpCargoTrip, percent),
                SqlUtils.field(alias, percent)), 100))
        .setFrom(new SqlSelect()
                .addFields(tmpCargoTrip, key)
                .addSum(tmpCargoTrip, percent)
                .addFrom(tmpCargoTrip)
                .addGroup(tmpCargoTrip, key), alias,
            SqlUtils.and(SqlUtils.joinUsing(tmpCargoTrip, alias, key),
                SqlUtils.notNull(alias, percent), SqlUtils.more(alias, percent, 100))));

    // Percent correction by runned kilometers
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.multiply(
            SqlUtils.divide(SqlUtils.field(tmpCargoTrip, COL_ROUTE_KILOMETERS),
                SqlUtils.field(alias, COL_ROUTE_KILOMETERS)),
            SqlUtils.minus(100, SqlUtils.nvl(SqlUtils.field(alias, percent), 0))))
        .setFrom(new SqlSelect()
                .addFields(tmpCargoTrip, key)
                .addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmpCargoTrip, percent),
                    SqlUtils.field(tmpCargoTrip, COL_ROUTE_KILOMETERS), null), COL_ROUTE_KILOMETERS)
                .addSum(tmpCargoTrip, percent)
                .addFrom(tmpCargoTrip)
                .addGroup(tmpCargoTrip, key), alias,
            SqlUtils.and(SqlUtils.joinUsing(alias, tmpCargoTrip, key),
                SqlUtils.notNull(alias, COL_ROUTE_KILOMETERS),
                SqlUtils.positive(alias, COL_ROUTE_KILOMETERS)))
        .setWhere(SqlUtils.isNull(tmpCargoTrip, percent)));

    String cntEmpty = SqlUtils.uniqueName();

    // Percent correction proportionaly for empty percents
    qs.updateData(new SqlUpdate(tmpCargoTrip)
        .addExpression(percent, SqlUtils.divide(
            SqlUtils.minus(100.0, SqlUtils.nvl(SqlUtils.field(alias, percent), 0)),
            SqlUtils.field(alias, cntEmpty)))
        .setFrom(new SqlSelect()
            .addFields(tmpCargoTrip, key)
            .addSum(tmpCargoTrip, percent)
            .addSum(SqlUtils.sqlIf(SqlUtils.isNull(tmpCargoTrip, percent), 1, 0), cntEmpty)
            .addFrom(tmpCargoTrip)
            .addGroup(tmpCargoTrip, key), alias, SqlUtils.joinUsing(tmpCargoTrip, alias, key))
        .setWhere(SqlUtils.isNull(tmpCargoTrip, percent)));

    return tmpCargoTrip;
  }

  /**
   * Return SqlSelect query, calculating trip fuel consumptions from TripRoutes table.
   *
   * @param routes query filter with <b>unique</b> route values.
   * @param routeMode if true, returns results, grouped by RouteID, else grouped by Trip
   * @return query with columns: (RouteID or "Trip"), "Consumption"
   */
  public SqlSelect getFuelConsumptionsQuery(SqlSelect routes, boolean routeMode) {
    String trips = TBL_TRIPS;
    String fuel = TBL_FUEL_CONSUMPTIONS;
    String routeId = sys.getIdName(TBL_TRIP_ROUTES);

    IsExpression xprNorm = SqlUtils.sqlCase(SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_SEASON),
        FuelSeason.SUMMER, SqlUtils.field(fuel, BeeUtils.proper(FuelSeason.SUMMER.name())),
        SqlUtils.field(fuel, BeeUtils.proper(FuelSeason.WINTER.name())));

    IsExpression xpr = SqlUtils.sqlIf(SqlUtils.isNull(TBL_TRIP_ROUTES, COL_ROUTE_CONSUMPTION),
        SqlUtils.multiply(
            SqlUtils.plus(
                SqlUtils.divide(SqlUtils.nvl(SqlUtils.multiply(SqlUtils.field(TBL_TRIP_ROUTES,
                    COL_ROUTE_KILOMETERS), xprNorm), 0), 100),
                SqlUtils.nvl(SqlUtils.multiply(
                    SqlUtils.minus(SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS),
                        SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, COL_EMPTY_KILOMETERS), 0)),
                    SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_WEIGHT),
                    SqlUtils.field(fuel, "TonneKilometer")), 0),
                SqlUtils.nvl(SqlUtils.multiply(SqlUtils.field(TBL_TRIP_ROUTES, "MotoHours"),
                    SqlUtils.field(fuel, "MotoHour")), 0)),
            SqlUtils.plus(1, SqlUtils.divide(SqlUtils.nvl(SqlUtils.field(VIEW_FUEL_TEMPERATURES,
                "Rate"), 0), 100))),
        SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_CONSUMPTION));

    String alias = SqlUtils.uniqueName();

    return new SqlSelect()
        .addFields(TBL_TRIP_ROUTES, routeMode ? routeId : COL_TRIP)
        .addSum(SqlUtils.round(xpr, 2), COL_ROUTE_CONSUMPTION)
        .addFrom(TBL_TRIP_ROUTES)
        .addFromInner(routes, alias, SqlUtils.joinUsing(TBL_TRIP_ROUTES, alias, routeId))
        .addFromInner(trips, sys.joinTables(trips, TBL_TRIP_ROUTES, COL_TRIP))
        .addFromInner(fuel, SqlUtils.joinUsing(trips, fuel, COL_VEHICLE))
        .addFromLeft(VIEW_FUEL_TEMPERATURES,
            SqlUtils.and(sys.joinTables(fuel, VIEW_FUEL_TEMPERATURES, COL_ROUTE_CONSUMPTION),
                SqlUtils.joinUsing(VIEW_FUEL_TEMPERATURES, TBL_TRIP_ROUTES, COL_ROUTE_SEASON),
                SqlUtils.or(SqlUtils.isNull(VIEW_FUEL_TEMPERATURES, "TempFrom"),
                    SqlUtils.lessEqual(SqlUtils.field(VIEW_FUEL_TEMPERATURES, "TempFrom"),
                        SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, "Temperature"), 0))),
                SqlUtils.or(SqlUtils.isNull(VIEW_FUEL_TEMPERATURES, "TempTo"),
                    SqlUtils.more(SqlUtils.field(VIEW_FUEL_TEMPERATURES, "TempTo"),
                        SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, "Temperature"), 0)))))
        .setWhere(SqlUtils.and(
            SqlUtils.or(SqlUtils.isNull(fuel, COL_TRIP_DATE_FROM),
                SqlUtils.joinLessEqual(fuel, COL_TRIP_DATE_FROM, TBL_TRIP_ROUTES,
                    COL_ROUTE_DEPARTURE_DATE)),
            SqlUtils.or(SqlUtils.isNull(fuel, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(fuel, COL_TRIP_DATE_TO, TBL_TRIP_ROUTES,
                    COL_ROUTE_DEPARTURE_DATE))))
        .addGroup(TBL_TRIP_ROUTES, routeMode ? routeId : COL_TRIP);
  }

  public ResponseObject getFuelUsageReport(RequestInfo reqInfo) {
    Long currency = reqInfo.getParameterLong(COL_CURRENCY);
    boolean woVat = BeeUtils.toBoolean(reqInfo.getParameter(COL_TRADE_VAT));

    HasConditions clause = SqlUtils.and(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION),
        SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_FROM),
        SqlUtils.notNull(TBL_TRIPS, COL_TRIP_DATE_TO));

    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));

    String trucks = SqlUtils.uniqueName();
    String trailers = SqlUtils.uniqueName();

    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_TRIPS,
        sys.getIdName(TBL_TRIPS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_TRIP));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_NO));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_STATUS));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_FROM));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_TO));
    clause.add(report.getCondition(SqlUtils.field(trucks, COL_VEHICLE_NUMBER), COL_VEHICLE));
    clause.add(report.getCondition(SqlUtils.field(trailers, COL_VEHICLE_NUMBER), COL_TRAILER));

    String trips = qs.sqlCreateTemp(new SqlSelect()
        .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
        .addFields(TBL_TRIPS, COL_TRIP_STATUS, COL_TRIP_DATE, COL_TRIP_NO, COL_TRIP_DATE_FROM,
            COL_TRIP_DATE_TO)
        .addField(trucks, COL_VEHICLE_NUMBER, COL_VEHICLE)
        .addField(trailers, COL_VEHICLE_NUMBER, COL_TRAILER)
        .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
            SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")), COL_DRIVER)
        .addExpr(SqlUtils.minus(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_FUEL_BEFORE), 0),
            SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_FUEL_AFTER), 0)), COL_ROUTE_CONSUMPTION)
        .addEmptyDouble(COL_COSTS_PRICE)
        .addFrom(TBL_TRIPS)
        .addFromLeft(TBL_VEHICLES, trucks,
            sys.joinTables(TBL_VEHICLES, trucks, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailers,
            sys.joinTables(TBL_VEHICLES, trailers, TBL_TRIPS, COL_TRAILER))
        .addFromLeft(TBL_TRIP_DRIVERS, sys.joinTables(TBL_TRIP_DRIVERS, TBL_TRIPS, COL_MAIN_DRIVER))
        .addFromLeft(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromLeft(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_COMPANY_PERSON))
        .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .setWhere(clause));

    String als = SqlUtils.uniqueName();

    // Consumption
    qs.updateData(new SqlUpdate(trips)
        .addExpression(COL_ROUTE_CONSUMPTION,
            SqlUtils.plus(SqlUtils.field(trips, COL_ROUTE_CONSUMPTION),
                SqlUtils.nvl(SqlUtils.field(als, COL_COSTS_QUANTITY), 0)))
        .setFrom(new SqlSelect()
                .addFields(TBL_TRIP_FUEL_COSTS, COL_TRIP)
                .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
                .addFrom(TBL_TRIP_FUEL_COSTS)
                .addFromInner(trips, SqlUtils.joinUsing(trips, TBL_TRIP_FUEL_COSTS, COL_TRIP))
                .addGroup(TBL_TRIP_FUEL_COSTS, COL_TRIP), als,
            SqlUtils.joinUsing(trips, als, COL_TRIP)));

    // Fuel price
    SqlSelect query = new SqlSelect()
        .addFields(TBL_TRIP_FUEL_COSTS, COL_TRIP)
        .addFrom(TBL_TRIP_FUEL_COSTS)
        .addFromInner(new SqlSelect()
                .addFields(TBL_TRIP_FUEL_COSTS, COL_TRIP)
                .addMax(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE)
                .addFrom(TBL_TRIP_FUEL_COSTS)
                .addFromInner(trips,
                    SqlUtils.joinUsing(TBL_TRIP_FUEL_COSTS, trips, COL_TRIP))
                .addGroup(TBL_TRIP_FUEL_COSTS, COL_TRIP), als + 2,
            SqlUtils.joinUsing(TBL_TRIP_FUEL_COSTS, als + 2, COL_TRIP, COL_COSTS_DATE))
        .addGroup(TBL_TRIP_FUEL_COSTS, COL_TRIP);

    IsExpression amountExpr;

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_FUEL_COSTS,
          SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_PRICE));
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS,
          SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_PRICE));
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_CURRENCY);
    IsExpression dateExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE);

    IsExpression xpr;

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(query, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(query, amountExpr, currencyExpr, dateExpr);
    }
    qs.updateData(new SqlUpdate(trips)
        .addExpression(COL_COSTS_PRICE, SqlUtils.field(als, COL_COSTS_PRICE))
        .setFrom(query.addMax(xpr, COL_COSTS_PRICE), als,
            SqlUtils.joinUsing(trips, als, COL_TRIP)));

    // Norm consumption
    String normConsumption = "NormConsumption";
    String routeId = sys.getIdName(TBL_TRIP_ROUTES);

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addAllFields(trips)
        .addFields(TBL_CARGO_TRIPS, COL_CARGO)
        .addFields(TBL_TRIP_ROUTES, COL_ROUTE_KILOMETERS, COL_EMPTY_KILOMETERS, COL_ROUTE_WEIGHT)
        .addField(als, routeId, COL_TRIP_ROUTE)
        .addField(als, COL_ROUTE_CONSUMPTION, normConsumption)
        .addFrom(trips)
        .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(trips, TBL_TRIP_ROUTES, COL_TRIP))
        .addFromLeft(getFuelConsumptionsQuery(new SqlSelect()
                .addFields(TBL_TRIP_ROUTES, routeId)
                .addFrom(TBL_TRIP_ROUTES)
                .addFromInner(trips, SqlUtils.joinUsing(TBL_TRIP_ROUTES, trips, COL_TRIP)), true),
            als, SqlUtils.joinUsing(TBL_TRIP_ROUTES, als, routeId))
        .addFromLeft(TBL_CARGO_TRIPS,
            sys.joinTables(TBL_CARGO_TRIPS, TBL_TRIP_ROUTES, COL_ROUTE_CARGO)));

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_ROUTE_CONSUMPTION,
            SqlUtils.multiply(SqlUtils.field(tmp, COL_ROUTE_CONSUMPTION),
                SqlUtils.divide(SqlUtils.field(tmp, normConsumption),
                    SqlUtils.field(als, normConsumption))))
        .setFrom(new SqlSelect()
            .addFields(tmp, COL_TRIP)
            .addSum(tmp, normConsumption)
            .addFrom(tmp)
            .addGroup(tmp, COL_TRIP), als, SqlUtils.joinUsing(tmp, als, COL_TRIP))
        .setWhere(SqlUtils.notEqual(als, normConsumption, 0)));

    // Economy bonus
    query = getConstantsQuery(trips, TBL_TRIP_CONSTANTS, SqlUtils.constant(1.0), COL_COSTS_PRICE,
        currency);

    query.addFields(trips, COL_TRIP)
        .setWhere(SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
            TripConstant.ECONOMY_BONUS))
        .addGroup(trips, COL_TRIP);

    qs.updateData(new SqlUpdate(tmp)
        .addExpression(COL_COSTS_PRICE, SqlUtils.field(als, COL_COSTS_PRICE))
        .setFrom(query, als, SqlUtils.joinUsing(tmp, als, COL_TRIP))
        .setWhere(SqlUtils.more(tmp, normConsumption, SqlUtils.field(tmp, COL_ROUTE_CONSUMPTION))));

    qs.sqlDropTemp(trips);

    query = new SqlSelect()
        .addFrom(tmp)
        .setWhere(report.getCondition(tmp, COL_DRIVER));

    for (String column : qs.getData(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

      if (report.requiresField(column)) {
        query.addFields(tmp, column);
      }
    }
    SimpleRowSet data = qs.getData(query);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(data);
  }

  /**
   * Return Temporary table name with calculated trip costs.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return Temporary table name with following structure: <br>
   * "Trip" - trip ID <br>
   * "DailyCosts" - total trip daily costs <br>
   * "RoadCosts" - total trip road costs <br>
   * "OtherCosts" - total trip other costs <br>
   * "FuelCosts" - total trip fuel costs considering remainder corrections
   */
  public String getTripCosts(SqlSelect trips, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();

    // Trip costs
    SqlSelect ss = new SqlSelect()
        .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
        .addField(TBL_TRIPS, COL_TRIP_DATE, "TripDate")
        .addFields(TBL_TRIPS, COL_VEHICLE, COL_FUEL_BEFORE, COL_FUEL_AFTER)
        .addEmptyDouble("FuelCosts")
        .addFrom(TBL_TRIPS)
        .addFromInner(trips, alias, sys.joinTables(TBL_TRIPS, alias, COL_TRIP))
        .addFromLeft(TBL_TRIP_COSTS, sys.joinTables(TBL_TRIPS, TBL_TRIP_COSTS, COL_TRIP))
        .addGroup(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP_DATE, COL_VEHICLE, COL_FUEL_BEFORE,
            COL_FUEL_AFTER);

    IsExpression amountExpr;

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_COSTS);
    }
    IsExpression currencyExpr = SqlUtils.field(TBL_TRIP_COSTS, COL_CURRENCY);
    IsExpression dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_DATE),
        SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE));

    IsExpression xpr;

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    IsCondition dailyCond = SqlUtils.inList(TBL_TRIP_COSTS, COL_ITEM,
        qs.getLongList(new SqlSelect().setDistinctMode(true)
            .addFields(TBL_COUNTRY_NORMS, COL_DAILY_COSTS_ITEM)
            .addFrom(TBL_COUNTRY_NORMS)));

    if (dailyCond != null) {
      ss.addSum(SqlUtils.sqlIf(dailyCond, xpr, null), "DailyCosts");
    } else {
      ss.addEmptyDouble("DailyCosts");
    }
    IsCondition roadCond = SqlUtils.inList(TBL_TRIP_COSTS, COL_ITEM,
        qs.getLongList(new SqlSelect().setDistinctMode(true)
            .addFields(TBL_COUNTRY_NORMS, COL_ROAD_COSTS_ITEM)
            .addFrom(TBL_COUNTRY_NORMS)));

    if (roadCond != null) {
      ss.addSum(SqlUtils.sqlIf(roadCond, xpr, null), "RoadCosts");
    } else {
      ss.addEmptyDouble("RoadCosts");
    }
    if (BeeUtils.anyNotNull(dailyCond, roadCond)) {
      ss.addSum(SqlUtils.sqlIf(SqlUtils.or(dailyCond, roadCond), null, xpr), "OtherCosts");
    } else {
      ss.addSum(xpr, "OtherCosts");
    }
    String tmpCosts = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpCosts, COL_TRIP);

    // Fuel costs
    ss = new SqlSelect()
        .addFields(tmpCosts, COL_TRIP)
        .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
        .addFrom(tmpCosts)
        .addFromLeft(TBL_TRIP_FUEL_COSTS,
            SqlUtils.joinUsing(tmpCosts, TBL_TRIP_FUEL_COSTS, COL_TRIP))
        .addGroup(tmpCosts, COL_TRIP);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_FUEL_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS);
    }
    currencyExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_CURRENCY);
    dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE),
        SqlUtils.field(tmpCosts, "TripDate"));

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    ss.addSum(xpr, "FuelCosts");

    String tmp = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmp, COL_TRIP);

    qs.updateData(new SqlUpdate(tmpCosts)
        .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
        .addExpression("FuelCosts", SqlUtils.field(tmp, "FuelCosts")));

    // Fuel consumptions
    if (qs.sqlExists(tmpCosts, SqlUtils.isNull(tmpCosts, COL_FUEL_AFTER))) {
      String tmpRoutes = qs.sqlCreateTemp(getFuelConsumptionsQuery(new SqlSelect()
          .addFields(TBL_TRIP_ROUTES, sys.getIdName(TBL_TRIP_ROUTES))
          .addFrom(TBL_TRIP_ROUTES)
          .addFromInner(tmpCosts, SqlUtils.joinUsing(TBL_TRIP_ROUTES, tmpCosts, COL_TRIP)), false));
      qs.sqlIndex(tmpRoutes, COL_TRIP);

      String tmpConsumptions = qs.sqlCreateTemp(new SqlSelect()
          .addFields(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP)
          .addSum(TBL_TRIP_FUEL_CONSUMPTIONS, "Quantity")
          .addFrom(TBL_TRIP_FUEL_CONSUMPTIONS)
          .addFromInner(tmpCosts,
              SqlUtils.joinUsing(TBL_TRIP_FUEL_CONSUMPTIONS, tmpCosts, COL_TRIP))
          .addGroup(TBL_TRIP_FUEL_CONSUMPTIONS, COL_TRIP));

      qs.sqlIndex(tmpConsumptions, COL_TRIP);

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(new SqlSelect()
                  .addFields(tmp, COL_TRIP, "Quantity")
                  .addField(tmpRoutes, COL_ROUTE_CONSUMPTION, "routeQuantity")
                  .addField(tmpConsumptions, "Quantity", "consumeQuantity")
                  .addFrom(tmp)
                  .addFromLeft(tmpRoutes, SqlUtils.joinUsing(tmp, tmpRoutes, COL_TRIP))
                  .addFromLeft(tmpConsumptions, SqlUtils.joinUsing(tmp, tmpConsumptions, COL_TRIP)),
              "sub", SqlUtils.joinUsing(tmpCosts, "sub", COL_TRIP))
          .addExpression(COL_FUEL_AFTER, SqlUtils.minus(
              SqlUtils.plus(
                  SqlUtils.nvl(SqlUtils.field(tmpCosts, COL_FUEL_BEFORE), 0),
                  SqlUtils.nvl(SqlUtils.field("sub", "Quantity"), 0)),
              SqlUtils.nvl(SqlUtils.field("sub", "routeQuantity"), 0),
              SqlUtils.nvl(SqlUtils.field("sub", "consumeQuantity"), 0)))
          .setWhere(SqlUtils.isNull(tmpCosts, COL_FUEL_AFTER)));

      qs.sqlDropTemp(tmpRoutes);
      qs.sqlDropTemp(tmpConsumptions);
    }
    qs.sqlDropTemp(tmp);

    // Fuel cost correction
    ss =
        new SqlSelect()
            .addFields(TBL_TRIPS, COL_VEHICLE)
            .addField(TBL_TRIPS, "Date", "TripDate")
            .addFields(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE)
            .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
            .addFrom(TBL_TRIPS)
            .addFromInner(TBL_TRIP_FUEL_COSTS,
                sys.joinTables(TBL_TRIPS, TBL_TRIP_FUEL_COSTS, COL_TRIP))
            .addFromInner(new SqlSelect()
                    .addFields(TBL_TRIPS, COL_VEHICLE)
                    .addMax(TBL_TRIPS, "Date", "MaxDate")
                    .addFrom(TBL_TRIPS)
                    .addFromInner(tmpCosts, sys.joinTables(TBL_TRIPS, tmpCosts, COL_TRIP))
                    .addGroup(TBL_TRIPS, COL_VEHICLE), "sub",
                SqlUtils.and(SqlUtils.joinUsing(TBL_TRIPS, "sub", COL_VEHICLE),
                    SqlUtils.joinLessEqual(TBL_TRIPS, "Date", "sub", "MaxDate"),
                    SqlUtils.and(SqlUtils.positive(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY),
                        SqlUtils.positive(TBL_TRIP_FUEL_COSTS, COL_COSTS_PRICE))))
            .addGroup(TBL_TRIPS, COL_VEHICLE, "Date")
            .addGroup(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE);

    if (woVat) {
      amountExpr = TradeModuleBean.getWithoutVatExpression(TBL_TRIP_FUEL_COSTS);
    } else {
      amountExpr = TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS);
    }
    currencyExpr = SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_CURRENCY);
    dateExpr = SqlUtils.nvl(SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE),
        SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE));

    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, amountExpr, currencyExpr, dateExpr,
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, amountExpr, currencyExpr, dateExpr);
    }
    ss.addSum(xpr, "Sum");

    String tmpFuels = qs.sqlCreateTemp(ss);
    qs.sqlIndex(tmpFuels, COL_VEHICLE);

    for (int i = 0; i < 2; i++) {
      boolean plusMode = i == 0;
      String fld = plusMode ? COL_FUEL_BEFORE : COL_FUEL_AFTER;

      tmp = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmpCosts, COL_TRIP, "TripDate", COL_VEHICLE)
          .addField(tmpCosts, fld, "Remainder")
          .addEmptyDate("Date")
          .addEmptyDouble("Cost")
          .addFrom(tmpCosts)
          .setWhere(SqlUtils.positive(tmpCosts, fld)));

      qs.sqlIndex(tmp, COL_TRIP, COL_VEHICLE);
      int c;

      IsCondition cond = plusMode
          ? SqlUtils.joinLess(tmpFuels, "TripDate", tmp, "TripDate")
          : SqlUtils.joinLessEqual(tmpFuels, "TripDate", tmp, "TripDate");

      do {
        String tmp2 = qs.sqlCreateTemp(new SqlSelect()
            .addFields(tmp, COL_VEHICLE, "TripDate")
            .addMax(tmpFuels, "Date")
            .addFrom(tmp)
            .addFromInner(tmpFuels, SqlUtils.joinUsing(tmp, tmpFuels, COL_VEHICLE))
            .setWhere(SqlUtils.and(cond,
                SqlUtils.or(SqlUtils.isNull(tmp, "Date"),
                    SqlUtils.joinLess(tmpFuels, "Date", tmp, "Date")),
                SqlUtils.positive(tmp, "Remainder")))
            .addGroup(tmp, COL_VEHICLE, "TripDate"));

        qs.sqlIndex(tmp2, COL_VEHICLE);

        c = qs.updateData(new SqlUpdate(tmp)
            .setFrom(new SqlSelect()
                    .addFields(tmp2, COL_VEHICLE, "TripDate", "Date")
                    .addFields(tmpFuels, "Quantity", "Sum")
                    .addFrom(tmp2)
                    .addFromInner(tmpFuels,
                        SqlUtils.joinUsing(tmp2, tmpFuels, COL_VEHICLE, "Date")),
                "sub", SqlUtils.joinUsing(tmp, "sub", COL_VEHICLE, "TripDate"))
            .addExpression("Date", SqlUtils.field("sub", "Date"))
            .addExpression("Cost", SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0),
                SqlUtils.sqlIf(SqlUtils.joinLess(tmp, "Remainder", "sub", "Quantity"),
                    SqlUtils.multiply(SqlUtils.field(tmp, "Remainder"),
                        SqlUtils.divide((Object[]) SqlUtils.fields("sub", "Sum", "Quantity"))),
                    SqlUtils.field("sub", "Sum"))))
            .addExpression("Remainder", SqlUtils.minus(SqlUtils.field(tmp, "Remainder"),
                SqlUtils.field("sub", "Quantity")))
            .setWhere(SqlUtils.positive(tmp, "Remainder")));

        qs.sqlDropTemp(tmp2);

      } while (BeeUtils.isPositive(c));

      IsExpression expr = plusMode
          ? SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCosts"), 0),
          SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0))
          : SqlUtils.minus(SqlUtils.nvl(SqlUtils.field(tmpCosts, "FuelCosts"), 0),
          SqlUtils.nvl(SqlUtils.field(tmp, "Cost"), 0));

      qs.updateData(new SqlUpdate(tmpCosts)
          .setFrom(tmp, SqlUtils.joinUsing(tmpCosts, tmp, COL_TRIP))
          .addExpression("FuelCosts", expr));

      qs.sqlDropTemp(tmp);
    }
    qs.sqlDropTemp(tmpFuels);

    return tmpCosts;
  }

  /**
   * Returns Temporary table name with calculated trip percents and incomes by each cargo.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @param currency currency to convert to.
   * @param woVat exclude vat.
   * @return Temporary table name with following structure: <br>
   * "Trip" - trip ID <br>
   * "Cargo" - cargo ID <br>
   * "TripPercent" - calculated trip percent <br>
   * "TripIncome" - total trip income <br>
   */
  public String getTripIncomes(SqlSelect trips, Long currency, boolean woVat) {
    String alias = SqlUtils.uniqueName();
    String subq = SqlUtils.uniqueName();

    String tmpCargoTrip = getCargoTripPercents(COL_CARGO,
        new SqlSelect().setDistinctMode(true)
            .addFields(TBL_CARGO_TRIPS, COL_CARGO)
            .addFrom(TBL_CARGO_TRIPS)
            .addFromInner(trips, alias, SqlUtils.joinUsing(TBL_CARGO_TRIPS, alias, COL_TRIP)));

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(tmpCargoTrip, COL_CARGO, COL_TRIP, COL_TRIP_PERCENT)
        .addExpr(SqlUtils.multiply(SqlUtils.divide(SqlUtils.field(subq, "CargoIncome"), 100),
            SqlUtils.field(tmpCargoTrip, COL_TRIP_PERCENT)), "TripIncome")
        .addFrom(tmpCargoTrip)
        .addFromInner(getCargoIncomeQuery(new SqlSelect().setDistinctMode(true)
                .addFields(tmpCargoTrip, COL_CARGO)
                .addFrom(tmpCargoTrip), currency, woVat),
            subq, SqlUtils.joinUsing(tmpCargoTrip, subq, COL_CARGO))
        .addFromInner(trips, alias, SqlUtils.joinUsing(tmpCargoTrip, alias, COL_TRIP)));

    qs.sqlDropTemp(tmpCargoTrip);
    return tmp;
  }

  public ResponseObject getTripInfo(RequestInfo reqInfo) {
    Long trip = Assert.notNull(reqInfo.getParameterLong(COL_TRIP));

    Pair<Long, String> currencyInfo = prm.getRelationInfo(PRM_CURRENCY);

    String crs = getTripCosts(new SqlSelect().addConstant(trip, COL_TRIP),
        currencyInfo != null ? currencyInfo.getA() : null,
        BeeUtils.unbox(prm.getBoolean(PRM_EXCLUDE_VAT)));
    String fuelCosts = qs.getValue(new SqlSelect().addFields(crs, "FuelCosts").addFrom(crs));
    qs.sqlDropTemp(crs);

    Map<String, Object> pack = new HashMap<>();

    pack.put(COL_CURRENCY, currencyInfo != null ? currencyInfo.getB() : null);
    pack.put(TBL_TRIP_FUEL_COSTS, fuelCosts);

    pack.put(COL_FUEL, qs.getValue(new SqlSelect()
        .addSum(TBL_TRIP_FUEL_COSTS, COL_COSTS_QUANTITY)
        .addFrom(TBL_TRIP_FUEL_COSTS)
        .setWhere(SqlUtils.equals(TBL_TRIP_FUEL_COSTS, COL_TRIP, trip))));

    pack.put(TBL_TRIP_DRIVERS, qs.getData(new SqlSelect()
        .addField(TBL_TRIP_DRIVERS, sys.getIdName(TBL_TRIP_DRIVERS), COL_DRIVER)
        .addFields(TBL_TRIPS, COL_MAIN_DRIVER)
        .addFields(TBL_PERSONS, COL_FIRST_NAME, COL_LAST_NAME)
        .addFrom(TBL_TRIP_DRIVERS)
        .addFromInner(TBL_DRIVERS, sys.joinTables(TBL_DRIVERS, TBL_TRIP_DRIVERS, COL_DRIVER))
        .addFromInner(TBL_COMPANY_PERSONS,
            sys.joinTables(TBL_COMPANY_PERSONS, TBL_DRIVERS, COL_COMPANY_PERSON))
        .addFromInner(TBL_PERSONS,
            sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
        .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIP_DRIVERS, TBL_TRIPS, COL_MAIN_DRIVER))
        .setWhere(SqlUtils.equals(TBL_TRIP_DRIVERS, COL_TRIP, trip))));

    SqlSelect query = new SqlSelect()
        .addFields(TBL_DRIVER_ADVANCES, COL_DRIVER, COL_DATE)
        .addFrom(TBL_DRIVER_ADVANCES)
        .addFromInner(TBL_TRIP_DRIVERS,
            sys.joinTables(TBL_TRIP_DRIVERS, TBL_DRIVER_ADVANCES, COL_DRIVER))
        .setWhere(SqlUtils.equals(TBL_TRIP_DRIVERS, COL_TRIP, trip))
        .addOrder(TBL_DRIVER_ADVANCES, COL_DATE);

    IsExpression total = ExchangeUtils.exchangeFieldTo(query,
        TBL_DRIVER_ADVANCES, COL_AMOUNT, COL_CURRENCY, COL_DATE, currencyInfo.getA());

    pack.put(TBL_DRIVER_ADVANCES, qs.getData(query.addExpr(total, COL_AMOUNT)));

    query = new SqlSelect()
        .addFields(TBL_PAYMENT_TYPES, COL_PAYMENT_CASH)
        .addFields(TBL_TRIP_COSTS, COL_COSTS_ITEM, COL_COSTS_DATE, COL_COSTS_QUANTITY,
            COL_NUMBER, COL_DRIVER)
        .addFields(TBL_ITEMS, COL_ITEM_NAME)
        .addField(TBL_UNITS, COL_UNIT_NAME, COL_UNIT)
        .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COSTS_COUNTRY)
        .addFrom(TBL_TRIP_COSTS)
        .addFromInner(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_TRIP_COSTS, COL_COSTS_ITEM))
        .addFromInner(TBL_UNITS, sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT))
        .addFromLeft(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_TRIP_COSTS, COL_COSTS_COUNTRY))
        .addFromLeft(TBL_PAYMENT_TYPES,
            sys.joinTables(TBL_PAYMENT_TYPES, TBL_TRIP_COSTS, COL_PAYMENT_TYPE))
        .setWhere(SqlUtils.equals(TBL_TRIP_COSTS, COL_TRIP, trip))
        .addOrder(null, COL_COSTS_DATE);

    total = ExchangeUtils.exchangeFieldTo(query,
        TradeModuleBean.getTotalExpression(TBL_TRIP_COSTS),
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_CURRENCY),
        SqlUtils.field(TBL_TRIP_COSTS, COL_COSTS_DATE),
        DataUtils.isId(currencyInfo.getA()) ? SqlUtils.constant(currencyInfo.getA()) : null);

    query.addExpr(total, COL_AMOUNT);

    String alsItems = SqlUtils.uniqueName();
    String alsUnits = SqlUtils.uniqueName();

    SqlSelect fuelQuery = new SqlSelect()
        .addFields(TBL_PAYMENT_TYPES, COL_PAYMENT_CASH)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_ITEM),
            SqlUtils.field(TBL_FUEL_TYPES, COL_COSTS_ITEM)), COL_COSTS_ITEM)
        .addFields(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE, COL_COSTS_QUANTITY, COL_NUMBER)
        .addEmptyLong(COL_DRIVER)
        .addExpr(SqlUtils.nvl(SqlUtils.field(alsItems, COL_ITEM_NAME),
            SqlUtils.field(TBL_ITEMS, COL_ITEM_NAME)), COL_ITEM_NAME)
        .addExpr(SqlUtils.nvl(SqlUtils.field(alsUnits, COL_UNIT_NAME),
            SqlUtils.field(TBL_UNITS, COL_UNIT_NAME)), COL_UNIT)
        .addField(TBL_COUNTRIES, COL_COUNTRY_NAME, COL_COSTS_COUNTRY)
        .addFrom(TBL_TRIP_FUEL_COSTS)
        .addFromLeft(TBL_ITEMS, alsItems,
            sys.joinTables(TBL_ITEMS, alsItems, TBL_TRIP_FUEL_COSTS, COL_COSTS_ITEM))
        .addFromLeft(TBL_UNITS, alsUnits,
            sys.joinTables(TBL_UNITS, alsUnits, alsItems, COL_UNIT))
        .addFromLeft(TBL_TRIPS, sys.joinTables(TBL_TRIPS, TBL_TRIP_FUEL_COSTS, COL_TRIP))
        .addFromLeft(TBL_VEHICLES, sys.joinTables(TBL_VEHICLES, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_FUEL_TYPES, sys.joinTables(TBL_FUEL_TYPES, TBL_VEHICLES, COL_FUEL))
        .addFromLeft(TBL_ITEMS, sys.joinTables(TBL_ITEMS, TBL_FUEL_TYPES, COL_COSTS_ITEM))
        .addFromLeft(TBL_UNITS, sys.joinTables(TBL_UNITS, TBL_ITEMS, COL_UNIT))
        .addFromLeft(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_TRIP_FUEL_COSTS, COL_COSTS_COUNTRY))
        .addFromLeft(TBL_PAYMENT_TYPES,
            sys.joinTables(TBL_PAYMENT_TYPES, TBL_TRIP_FUEL_COSTS, COL_PAYMENT_TYPE))
        .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRIP_FUEL_COSTS, COL_TRIP, trip),
            SqlUtils.or(SqlUtils.notNull(TBL_TRIP_FUEL_COSTS, COL_COSTS_ITEM),
                SqlUtils.notNull(TBL_FUEL_TYPES, COL_COSTS_ITEM))));

    total = ExchangeUtils.exchangeFieldTo(fuelQuery,
        TradeModuleBean.getTotalExpression(TBL_TRIP_FUEL_COSTS),
        SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_CURRENCY),
        SqlUtils.field(TBL_TRIP_FUEL_COSTS, COL_COSTS_DATE),
        DataUtils.isId(currencyInfo.getA()) ? SqlUtils.constant(currencyInfo.getA()) : null);

    fuelQuery.addExpr(total, COL_AMOUNT);

    pack.put(TBL_TRIP_COSTS, qs.getData(query.addUnion(fuelQuery)));

    pack.put(COL_DAILY_COSTS_ITEM, qs.getColumn(new SqlSelect().setDistinctMode(true)
        .addFields(TBL_COUNTRY_NORMS, COL_DAILY_COSTS_ITEM)
        .addFrom(TBL_COUNTRY_NORMS)));

    return ResponseObject.response(pack);
  }

  public ResponseObject getTripProfitReport(RequestInfo reqInfo) {
    Long currency = reqInfo.getParameterLong(COL_CURRENCY);
    boolean woVat = BeeUtils.toBoolean(reqInfo.getParameter(COL_TRADE_VAT));

    ReportInfo report = ReportInfo.restore(reqInfo.getParameter(Service.VAR_DATA));

    String trucks = SqlUtils.uniqueName();
    String trailers = SqlUtils.uniqueName();

    String tripIncome = "TripIncome";
    String kilometers = COL_ROUTE_KILOMETERS;
    String fuelCosts = "FuelCosts";
    String dailyCosts = "DailyCosts";
    String roadCosts = "RoadCosts";
    String constantCosts = "ConstantCosts";
    String otherCosts = "OtherCosts";
    String plannedKilometers = "Planned" + kilometers;
    String plannedFuelCosts = "Planned" + fuelCosts;
    String plannedDailyCosts = "Planned" + dailyCosts;
    String plannedRoadCosts = "Planned" + roadCosts;

    HasConditions clause = SqlUtils.and();
    clause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_TRIPS,
        sys.getIdName(TBL_TRIPS)), SqlConstants.SqlDataType.STRING, 20, 0), COL_TRIP));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_NO));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_STATUS));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_FROM));
    clause.add(report.getCondition(TBL_TRIPS, COL_TRIP_DATE_TO));
    clause.add(report.getCondition(SqlUtils.field(trucks, COL_VEHICLE_NUMBER), COL_VEHICLE));
    clause.add(report.getCondition(SqlUtils.field(trailers, COL_VEHICLE_NUMBER), COL_TRAILER));

    HasConditions cargoClause = SqlUtils.and();
    cargoClause.add(report.getCondition(TBL_ORDERS, COL_ORDER_NO));
    cargoClause.add(report.getCondition(SqlUtils.field(TBL_ORDERS, COL_ORDER_DATE),
        COL_ORDER + COL_ORDER_DATE));
    cargoClause.add(report.getCondition(SqlUtils.field(TBL_COMPANIES, COL_COMPANY_NAME),
        COL_CUSTOMER));
    cargoClause.add(report.getCondition(SqlUtils.cast(SqlUtils.field(TBL_ORDER_CARGO,
        sys.getIdName(TBL_ORDER_CARGO)), SqlConstants.SqlDataType.STRING, 20, 0), COL_CARGO));
    cargoClause.add(report.getCondition(TBL_ORDER_CARGO, COL_CARGO_PARTIAL));

    if (!cargoClause.isEmpty()) {
      clause.add(SqlUtils.in(TBL_TRIPS, sys.getIdName(TBL_TRIPS),
          new SqlSelect().setDistinctMode(true)
              .addFields(TBL_CARGO_TRIPS, COL_TRIP)
              .addFrom(TBL_CARGO_TRIPS)
              .addFromLeft(TBL_ORDER_CARGO,
                  sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO))
              .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
              .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
              .setWhere(cargoClause)));
    }
    Function<IsExpression, IsExpression> dateTimeToDate = xpr ->
        SqlUtils.multiply(SqlUtils.divide(SqlUtils.plus(xpr,
            TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()),
            TimeUtils.MILLIS_PER_DAY), TimeUtils.MILLIS_PER_DAY);

    SqlSelect query = new SqlSelect()
        .addField(TBL_TRIPS, sys.getIdName(TBL_TRIPS), COL_TRIP)
        .addFields(TBL_TRIPS, COL_TRIP_STATUS, COL_TRIP_DATE, COL_TRIP_NO, COL_TRIP_ROUTE)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_FROM),
            dateTimeToDate.apply(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE))), COL_TRIP_DATE_FROM)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIPS, COL_TRIP_DATE_TO),
            SqlUtils.field(TBL_TRIPS, COL_TRIP_PLANNED_END_DATE)), COL_TRIP_DATE_TO)
        .addField(trucks, COL_VEHICLE_NUMBER, COL_VEHICLE)
        .addField(trailers, COL_VEHICLE_NUMBER, COL_TRAILER)
        .addEmptyDouble(plannedKilometers)
        .addEmptyDouble(plannedFuelCosts)
        .addEmptyDouble(plannedDailyCosts)
        .addEmptyDouble(plannedRoadCosts)
        .addEmptyDouble(kilometers)
        .addEmptyDouble(fuelCosts)
        .addEmptyDouble(dailyCosts)
        .addEmptyDouble(roadCosts)
        .addEmptyDouble(constantCosts)
        .addEmptyDouble(otherCosts)
        .addEmptyDouble(tripIncome)
        .addFrom(TBL_TRIPS)
        .addFromLeft(TBL_VEHICLES, trucks,
            sys.joinTables(TBL_VEHICLES, trucks, TBL_TRIPS, COL_VEHICLE))
        .addFromLeft(TBL_VEHICLES, trailers,
            sys.joinTables(TBL_VEHICLES, trailers, TBL_TRIPS, COL_TRAILER))
        .setWhere(clause.add(SqlUtils.isNull(TBL_TRIPS, COL_EXPEDITION)));

    String tmp = qs.sqlCreateTemp(query);

    // Routes
    if (report.requiresField(COL_TRIP_ROUTE)) {
      String rTmp = getTripRoutes(new SqlSelect()
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp)
          .setWhere(SqlUtils.isNull(tmp, COL_TRIP_ROUTE)));

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(COL_TRIP_ROUTE, SqlUtils.field(rTmp, COL_TRIP_ROUTE))
          .setFrom(rTmp, SqlUtils.joinUsing(tmp, rTmp, COL_TRIP)));

      qs.sqlDropTemp(rTmp);
    }
    // Kilometers
    if (report.requiresField(kilometers)) {
      String als = SqlUtils.uniqueName();

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(kilometers, SqlUtils.field(als, kilometers))
          .setFrom(new SqlSelect()
                  .addFields(TBL_TRIP_ROUTES, COL_TRIP)
                  .addSum(TBL_TRIP_ROUTES, kilometers)
                  .addFrom(TBL_TRIP_ROUTES)
                  .addFromInner(tmp, SqlUtils.joinUsing(TBL_TRIP_ROUTES, tmp, COL_TRIP))
                  .addGroup(TBL_TRIP_ROUTES, COL_TRIP), als,
              SqlUtils.joinUsing(tmp, als, COL_TRIP)));
    }
    String tmpTripCargo = null;

    // Planned kilometers
    if (report.requiresField(plannedKilometers) || report.requiresField(plannedFuelCosts)
        || report.requiresField(plannedDailyCosts) || report.requiresField(plannedRoadCosts)) {

      tmpTripCargo = qs.sqlCreateTemp(new SqlSelect()
          .addFields(TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO)
          .addField(TBL_CARGO_TRIPS, sys.getIdName(TBL_CARGO_TRIPS), COL_CARGO_TRIP)
          .addFields(tmp, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_DATE)
          .addFields(TBL_ORDER_CARGO, COL_CARGO_TYPE)
          .addEmptyDouble(COL_LOADING_PLACE)
          .addEmptyDouble(COL_UNLOADING_PLACE)
          .addEmptyDouble(plannedKilometers)
          .addEmptyDouble(plannedFuelCosts)
          .addEmptyDouble(plannedDailyCosts)
          .addEmptyDouble(plannedRoadCosts)
          .addFrom(TBL_CARGO_TRIPS)
          .addFromInner(tmp, SqlUtils.joinUsing(TBL_CARGO_TRIPS, tmp, COL_TRIP))
          .addFromInner(TBL_ORDER_CARGO,
              sys.joinTables(TBL_ORDER_CARGO, TBL_CARGO_TRIPS, COL_CARGO)));

      String als = SqlUtils.uniqueName();

      for (Map.Entry<String, String> entry : ImmutableMap.of(TBL_CARGO_LOADING, COL_LOADING_PLACE,
          TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE).entrySet()) {
        String tbl = entry.getKey();
        String col = entry.getValue();

        qs.updateData(new SqlUpdate(tmpTripCargo)
            .addExpression(col,
                SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(als, COL_LOADED_KILOMETERS), 0),
                    SqlUtils.nvl(SqlUtils.field(als, COL_EMPTY_KILOMETERS), 0)))
            .setFrom(new SqlSelect()
                    .addFields(tmpTripCargo, COL_CARGO_TRIP)
                    .addSum(TBL_CARGO_PLACES, COL_LOADED_KILOMETERS)
                    .addSum(TBL_CARGO_PLACES, COL_EMPTY_KILOMETERS)
                    .addFrom(tmpTripCargo)
                    .addFromInner(tbl, SqlUtils.joinUsing(tmpTripCargo, tbl, COL_CARGO_TRIP))
                    .addFromInner(TBL_CARGO_PLACES, sys.joinTables(TBL_CARGO_PLACES, tbl, col))
                    .addGroup(tmpTripCargo, COL_CARGO_TRIP),
                als, SqlUtils.joinUsing(tmpTripCargo, als, COL_CARGO_TRIP)));

        qs.updateData(new SqlUpdate(tmpTripCargo)
            .addExpression(col,
                SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(als, COL_LOADED_KILOMETERS), 0),
                    SqlUtils.nvl(SqlUtils.field(als, COL_EMPTY_KILOMETERS), 0)))
            .setFrom(new SqlSelect()
                    .addFields(tmpTripCargo, COL_CARGO_TRIP)
                    .addSum(TBL_CARGO_PLACES, COL_LOADED_KILOMETERS)
                    .addSum(TBL_CARGO_PLACES, COL_EMPTY_KILOMETERS)
                    .addFrom(tmpTripCargo)
                    .addFromInner(tbl, SqlUtils.joinUsing(tmpTripCargo, tbl, COL_CARGO))
                    .addFromInner(TBL_CARGO_PLACES, sys.joinTables(TBL_CARGO_PLACES, tbl, col))
                    .addGroup(tmpTripCargo, COL_CARGO_TRIP),
                als, SqlUtils.joinUsing(tmpTripCargo, als, COL_CARGO_TRIP))
            .setWhere(SqlUtils.isNull(tmpTripCargo, col)));
      }
      qs.updateData(new SqlUpdate(tmpTripCargo)
          .addExpression(plannedKilometers,
              SqlUtils.plus(SqlUtils.nvl(SqlUtils.field(tmpTripCargo, COL_LOADING_PLACE), 0),
                  SqlUtils.nvl(SqlUtils.field(tmpTripCargo, COL_UNLOADING_PLACE), 0))));
    }
    // Planned fuel costs
    if (report.requiresField(plannedFuelCosts)) {
      String als = SqlUtils.uniqueName();

      String tt = qs.sqlCreateTemp(new SqlSelect()
          .addFields(tmpTripCargo, COL_TRIP, COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO, COL_TRIP_DATE,
              COL_CARGO, plannedKilometers, COL_CARGO_TYPE)
          .addFields(TBL_TRIPS, COL_VEHICLE)
          .addEmptyNumeric(fuelCosts, 6, 2)
          .addFrom(tmpTripCargo)
          .addFromInner(TBL_TRIPS, sys.joinTables(TBL_TRIPS, tmpTripCargo, COL_TRIP)));

      qs.updateData(new SqlUpdate(tt)
          .addExpression(fuelCosts, SqlUtils.field(TBL_FUEL_CONSUMPTIONS, "Average"))
          .setFrom(TBL_FUEL_CONSUMPTIONS, SqlUtils.and(
              SqlUtils.joinUsing(tt, TBL_FUEL_CONSUMPTIONS, COL_VEHICLE),
              SqlUtils.or(SqlUtils.isNull(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_FROM),
                  SqlUtils.joinLessEqual(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_FROM, tt,
                      COL_TRIP_DATE_TO)),
              SqlUtils.or(SqlUtils.isNull(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_TO),
                  SqlUtils.joinMore(TBL_FUEL_CONSUMPTIONS, COL_TRIP_DATE_TO, tt,
                      COL_TRIP_DATE_FROM)))));

      query = getConstantsQuery(tt, TBL_TRIP_CONSTANTS,
          SqlUtils.multiply(SqlUtils.field(tt, plannedKilometers),
              SqlUtils.divide(SqlUtils.field(tt, fuelCosts), 100)), plannedFuelCosts, currency);

      query.addFields(tt, COL_TRIP, COL_CARGO)
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
              TripConstant.AVERAGE_FUEL_COST),
              SqlUtils.or(SqlUtils.isNull(TBL_TRIP_CONSTANTS, COL_CARGO_TYPE),
                  SqlUtils.joinUsing(tt, TBL_TRIP_CONSTANTS, COL_CARGO_TYPE))))
          .addGroup(tt, COL_TRIP, COL_CARGO);

      qs.updateData(new SqlUpdate(tmpTripCargo)
          .addExpression(plannedFuelCosts, SqlUtils.field(als, plannedFuelCosts))
          .setFrom(query, als, SqlUtils.joinUsing(tmpTripCargo, als, COL_TRIP, COL_CARGO)));

      qs.sqlDropTemp(tt);
    }
    // Planned daily costs
    if (report.requiresField(plannedDailyCosts)) {
      String als = SqlUtils.uniqueName();

      query = getConstantsQuery(tmpTripCargo, TBL_DRIVER_DAILY_COSTS,
          SqlUtils.field(tmpTripCargo, plannedKilometers), plannedDailyCosts, currency);

      query.addFields(tmpTripCargo, COL_TRIP, COL_CARGO)
          .addFromInner(TBL_TRIP_DRIVERS,
              SqlUtils.joinUsing(tmpTripCargo, TBL_TRIP_DRIVERS, COL_TRIP))
          .setWhere(SqlUtils.joinUsing(TBL_TRIP_DRIVERS, TBL_DRIVER_DAILY_COSTS, COL_DRIVER))
          .addGroup(tmpTripCargo, COL_TRIP, COL_CARGO);

      qs.updateData(new SqlUpdate(tmpTripCargo)
          .addExpression(plannedDailyCosts, SqlUtils.field(als, plannedDailyCosts))
          .setFrom(query, als, SqlUtils.joinUsing(tmpTripCargo, als, COL_TRIP, COL_CARGO)));
    }
    // Planned road costs
    if (report.requiresField(plannedRoadCosts)) {
      String als = SqlUtils.uniqueName();

      query = getConstantsQuery(tmpTripCargo, TBL_TRIP_CONSTANTS,
          SqlUtils.field(tmpTripCargo, plannedKilometers), plannedRoadCosts, currency);

      query.addFields(tmpTripCargo, COL_TRIP, COL_CARGO)
          .setWhere(SqlUtils.and(SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
              TripConstant.AVERAGE_KM_COST),
              SqlUtils.or(SqlUtils.isNull(TBL_TRIP_CONSTANTS, COL_CARGO_TYPE),
                  SqlUtils.joinUsing(tmpTripCargo, TBL_TRIP_CONSTANTS, COL_CARGO_TYPE))))
          .addGroup(tmpTripCargo, COL_TRIP, COL_CARGO);

      qs.updateData(new SqlUpdate(tmpTripCargo)
          .addExpression(plannedRoadCosts, SqlUtils.field(als, plannedRoadCosts))
          .setFrom(query, als, SqlUtils.joinUsing(tmpTripCargo, als, COL_TRIP, COL_CARGO)));
    }
    // Costs
    if (report.requiresField(dailyCosts) || report.requiresField(roadCosts)
        || report.requiresField(otherCosts) || report.requiresField(fuelCosts)) {
      String costs = getTripCosts(new SqlSelect()
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp), currency, woVat);

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(dailyCosts, SqlUtils.field(costs, dailyCosts))
          .addExpression(roadCosts, SqlUtils.field(costs, roadCosts))
          .addExpression(otherCosts, SqlUtils.field(costs, otherCosts))
          .addExpression(fuelCosts, SqlUtils.field(costs, fuelCosts))
          .setFrom(costs, SqlUtils.joinUsing(tmp, costs, COL_TRIP)));

      qs.sqlDropTemp(costs);
    }
    // Constant costs
    if (report.requiresField(constantCosts)) {
      String als = SqlUtils.uniqueName();

      query = getConstantsQuery(tmp, TBL_TRIP_CONSTANTS,
          SqlUtils.plus(SqlUtils.divide(SqlUtils.minus(SqlUtils.field(tmp, COL_TRIP_DATE_TO),
              SqlUtils.field(tmp, COL_TRIP_DATE_FROM)), TimeUtils.MILLIS_PER_DAY), 1),
          constantCosts, currency);

      query.addFields(tmp, COL_TRIP)
          .setWhere(SqlUtils.equals(TBL_TRIP_CONSTANTS, COL_TRIP_CONSTANT,
              TripConstant.CONSTANT_COSTS))
          .addGroup(tmp, COL_TRIP);

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(constantCosts, SqlUtils.field(als, constantCosts))
          .setFrom(query, als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));
    }
    // Cargo info
    boolean cargoRequired = report.requiresField(COL_ORDER_NO)
        || report.requiresField(COL_ORDER + COL_ORDER_DATE) || report.requiresField(COL_CUSTOMER)
        || report.requiresField(COL_ORDER_MANAGER) || report.requiresField(COL_CARGO)
        || report.requiresField(COL_CARGO_PARTIAL);

    if (cargoRequired) {
      String tmpPercents = getCargoTripPercents(COL_TRIP,
          new SqlSelect()
              .addFields(tmp, COL_TRIP)
              .addFrom(tmp));

      query = new SqlSelect()
          .addFields(TBL_ORDERS, COL_ORDER_NO)
          .addField(TBL_ORDERS, COL_ORDER_DATE, COL_ORDER + COL_ORDER_DATE)
          .addField(TBL_COMPANIES, COL_COMPANY_NAME, COL_CUSTOMER)
          .addExpr(SqlUtils.concat(SqlUtils.field(TBL_PERSONS, COL_FIRST_NAME), "' '",
              SqlUtils.nvl(SqlUtils.field(TBL_PERSONS, COL_LAST_NAME), "''")), COL_ORDER_MANAGER)
          .addFields(tmpPercents, COL_CARGO)
          .addFields(TBL_ORDER_CARGO, COL_CARGO_PARTIAL)
          .addFrom(tmp)
          .addFromLeft(tmpPercents, SqlUtils.joinUsing(tmp, tmpPercents, COL_TRIP))
          .addFromLeft(TBL_ORDER_CARGO, sys.joinTables(TBL_ORDER_CARGO, tmpPercents, COL_CARGO))
          .addFromLeft(TBL_ORDERS, sys.joinTables(TBL_ORDERS, TBL_ORDER_CARGO, COL_ORDER))
          .addFromLeft(TBL_COMPANIES, sys.joinTables(TBL_COMPANIES, TBL_ORDERS, COL_CUSTOMER))
          .addFromLeft(TBL_USERS, sys.joinTables(TBL_USERS, TBL_ORDERS, COL_ORDER_MANAGER))
          .addFromLeft(TBL_COMPANY_PERSONS,
              sys.joinTables(TBL_COMPANY_PERSONS, TBL_USERS, COL_COMPANY_PERSON))
          .addFromLeft(TBL_PERSONS, sys.joinTables(TBL_PERSONS, TBL_COMPANY_PERSONS, COL_PERSON))
          .setWhere(cargoClause);

      for (String column : qs.getData(new SqlSelect()
          .addAllFields(tmp)
          .addFrom(tmp)
          .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

        if (BeeUtils.inList(column, kilometers, fuelCosts, dailyCosts, roadCosts, otherCosts)) {
          query.addExpr(SqlUtils.multiply(SqlUtils.divide(SqlUtils.field(tmp, column), 100.0),
              SqlUtils.nvl(SqlUtils.field(tmpPercents, COL_CARGO_PERCENT), 100)), column);
        } else {
          query.addFields(tmp, column);
        }
      }
      String tt = qs.sqlCreateTemp(query);

      // Constant costs
      if (report.requiresField(constantCosts)) {
        String alsLoading = SqlUtils.uniqueName();
        String alsUnloading = SqlUtils.uniqueName();
        String alsDays = SqlUtils.uniqueName();

        query = new SqlSelect()
            .addFields(TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO)
            .addMin(alsLoading, COL_DATE, COL_TRIP_DATE_FROM)
            .addMax(alsUnloading, COL_DATE, COL_TRIP_DATE_TO)
            .addFrom(tmp)
            .addFromInner(TBL_CARGO_TRIPS, SqlUtils.joinUsing(tmp, TBL_CARGO_TRIPS, COL_TRIP))
            .addGroup(TBL_CARGO_TRIPS, COL_TRIP, COL_CARGO);

        String tmpDates = qs.sqlCreateTemp(query.copyOf()
            .addEmptyDouble(alsDays)
            .addFromLeft(TBL_CARGO_LOADING,
                sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_LOADING, COL_CARGO_TRIP))
            .addFromLeft(TBL_CARGO_PLACES, alsLoading, sys.joinTables(TBL_CARGO_PLACES, alsLoading,
                TBL_CARGO_LOADING, COL_LOADING_PLACE))
            .addFromLeft(TBL_CARGO_UNLOADING,
                sys.joinTables(TBL_CARGO_TRIPS, TBL_CARGO_UNLOADING, COL_CARGO_TRIP))
            .addFromLeft(TBL_CARGO_PLACES, alsUnloading, sys.joinTables(TBL_CARGO_PLACES,
                alsUnloading, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE)));

        String tmpCargoDates = qs.sqlCreateTemp(query.copyOf()
            .addFromLeft(TBL_CARGO_LOADING,
                SqlUtils.joinUsing(TBL_CARGO_TRIPS, TBL_CARGO_LOADING, COL_CARGO))
            .addFromLeft(TBL_CARGO_PLACES, alsLoading, sys.joinTables(TBL_CARGO_PLACES,
                alsLoading, TBL_CARGO_LOADING, COL_LOADING_PLACE))
            .addFromLeft(TBL_CARGO_UNLOADING,
                SqlUtils.joinUsing(TBL_CARGO_TRIPS, TBL_CARGO_UNLOADING, COL_CARGO))
            .addFromLeft(TBL_CARGO_PLACES, alsUnloading, sys.joinTables(TBL_CARGO_PLACES,
                alsUnloading, TBL_CARGO_UNLOADING, COL_UNLOADING_PLACE)));

        for (String col : new String[] {COL_TRIP_DATE_FROM, COL_TRIP_DATE_TO}) {
          qs.updateData(new SqlUpdate(tmpDates)
              .addConstant(col, SqlUtils.field(tmpCargoDates, col))
              .setFrom(tmpCargoDates,
                  SqlUtils.joinUsing(tmpDates, tmpCargoDates, COL_TRIP, COL_CARGO))
              .setWhere(SqlUtils.isNull(tmpDates, col)));

          qs.updateData(new SqlUpdate(tmpDates)
              .addConstant(col, SqlUtils.field(tmp, col))
              .setFrom(tmp, SqlUtils.joinUsing(tmpDates, tmp, COL_TRIP))
              .setWhere(SqlUtils.isNull(tmpDates, col)));
        }
        qs.sqlDropTemp(tmpCargoDates);

        qs.updateData(new SqlUpdate(tmpDates)
            .addConstant(alsDays, SqlUtils.plus(
                SqlUtils.divide(
                    SqlUtils.minus(dateTimeToDate.apply(SqlUtils.field(tmpDates, COL_TRIP_DATE_TO)),
                        dateTimeToDate.apply(SqlUtils.field(tmpDates, COL_TRIP_DATE_FROM))),
                    TimeUtils.MILLIS_PER_DAY), 1)));

        String als = SqlUtils.uniqueName();

        qs.updateData(new SqlUpdate(tmpDates)
            .addConstant(alsDays,
                SqlUtils.divide(SqlUtils.field(tmpDates, alsDays), SqlUtils.field(als, alsDays)))
            .setFrom(new SqlSelect()
                .addFields(tmpDates, COL_TRIP)
                .addSum(tmpDates, alsDays)
                .addFrom(tmpDates)
                .addGroup(tmpDates, COL_TRIP), als, SqlUtils.joinUsing(tmpDates, als, COL_TRIP)));

        qs.updateData(new SqlUpdate(tt)
            .addConstant(constantCosts, SqlUtils.multiply(SqlUtils.field(tt, constantCosts),
                SqlUtils.field(tmpDates, alsDays)))
            .setFrom(tmpDates, SqlUtils.joinUsing(tt, tmpDates, COL_TRIP, COL_CARGO)));

        qs.sqlDropTemp(tmpDates);
      }
      qs.sqlDropTemp(tmp);
      qs.sqlDropTemp(tmpPercents);
      tmp = tt;
    }
    if (!BeeUtils.isEmpty(tmpTripCargo)) {
      String[] joinFields = cargoRequired
          ? new String[] {COL_TRIP, COL_CARGO} : new String[] {COL_TRIP};
      String als = SqlUtils.uniqueName();

      qs.updateData(new SqlUpdate(tmp)
          .addExpression(plannedKilometers, SqlUtils.field(als, plannedKilometers))
          .addExpression(plannedFuelCosts, SqlUtils.field(als, plannedFuelCosts))
          .addExpression(plannedDailyCosts, SqlUtils.field(als, plannedDailyCosts))
          .addExpression(plannedRoadCosts, SqlUtils.field(als, plannedRoadCosts))
          .setFrom(new SqlSelect()
              .addFields(tmpTripCargo, joinFields)
              .addSum(tmpTripCargo, plannedKilometers)
              .addSum(tmpTripCargo, plannedFuelCosts)
              .addSum(tmpTripCargo, plannedDailyCosts)
              .addSum(tmpTripCargo, plannedRoadCosts)
              .addFrom(tmpTripCargo)
              .addGroup(tmpTripCargo, joinFields), als, SqlUtils.joinUsing(tmp, als, joinFields)));

      qs.sqlDropTemp(tmpTripCargo);
    }
    // Incomes
    if (report.requiresField(tripIncome)) {
      String als = SqlUtils.uniqueName();

      String tripIncomes = getTripIncomes(new SqlSelect().setDistinctMode(true)
          .addFields(tmp, COL_TRIP)
          .addFrom(tmp), currency, woVat);

      if (cargoRequired) {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(tripIncome, SqlUtils.field(tripIncomes, tripIncome))
            .setFrom(tripIncomes, SqlUtils.joinUsing(tmp, tripIncomes, COL_CARGO, COL_TRIP)));
      } else {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(tripIncome, SqlUtils.field(als, tripIncome))
            .setFrom(new SqlSelect()
                .addFields(tripIncomes, COL_TRIP)
                .addSum(tripIncomes, tripIncome)
                .addFrom(tripIncomes)
                .addGroup(tripIncomes, COL_TRIP), als, SqlUtils.joinUsing(tmp, als, COL_TRIP)));
      }
      qs.sqlDropTemp(tripIncomes);
    }
    query = new SqlSelect()
        .addFrom(tmp)
        .setWhere(SqlUtils.and(report.getCondition(tmp, COL_TRIP_ROUTE),
            report.getCondition(tmp, COL_ORDER_MANAGER)));

    for (String column : qs.getData(new SqlSelect()
        .addAllFields(tmp)
        .addFrom(tmp)
        .setWhere(SqlUtils.sqlFalse())).getColumnNames()) {

      if (report.requiresField(column)) {
        query.addFields(tmp, column);
      }
    }
    SimpleRowSet data = qs.getData(query);
    qs.sqlDropTemp(tmp);
    return ResponseObject.response(data);
  }

  /**
   * Return Temporary table name with calculated trip routes.
   *
   * @param trips query filter with <b>unique</b> "Trip" values.
   * @return Temporary table name with following structure: <br>
   * "Trip" - trip ID <br>
   * "Routes" - trip route <br>
   */
  public String getTripRoutes(SqlSelect trips) {
    String als = SqlUtils.uniqueName();
    String lastRoute = SqlUtils.uniqueName();
    String cnt = SqlUtils.uniqueName();

    String tmp = qs.sqlCreateTemp(new SqlSelect()
        .addFields(als, COL_TRIP)
        .addEmptyString(COL_TRIP_ROUTE, sys.getFieldPrecision(TBL_TRIPS, COL_TRIP_ROUTE))
        .addEmptyString(lastRoute, sys.getFieldPrecision(TBL_COUNTRIES, COL_COUNTRY_NAME))
        .addFrom(trips, als));

    String rTmp = qs.sqlCreateTemp(new SqlSelect().setUnionAllMode(false)
        .addFields(TBL_TRIP_ROUTES, COL_TRIP)
        .addField(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE, COL_DATE)
        .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_CODE),
            SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_NAME)), COL_COUNTRY_CODE)
        .addFrom(tmp)
        .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
        .addFromInner(TBL_COUNTRIES,
            sys.joinTables(TBL_COUNTRIES, TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_COUNTRY))
        .addUnion(new SqlSelect()
            .addFields(TBL_TRIP_ROUTES, COL_TRIP)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_DATE),
                SqlUtils.field(TBL_TRIP_ROUTES, COL_ROUTE_DEPARTURE_DATE)), COL_DATE)
            .addExpr(SqlUtils.nvl(SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_CODE),
                SqlUtils.field(TBL_COUNTRIES, COL_COUNTRY_NAME)), COL_COUNTRY_CODE)
            .addFrom(tmp)
            .addFromInner(TBL_TRIP_ROUTES, SqlUtils.joinUsing(tmp, TBL_TRIP_ROUTES, COL_TRIP))
            .addFromInner(TBL_COUNTRIES,
                sys.joinTables(TBL_COUNTRIES, TBL_TRIP_ROUTES, COL_ROUTE_ARRIVAL_COUNTRY))
        ));
    String routes = qs.sqlCreateTemp(new SqlSelect()
        .addFields(rTmp, COL_TRIP, COL_COUNTRY_CODE)
        .addCount(cnt)
        .addFrom(rTmp)
        .addFromInner(rTmp, als, SqlUtils.and(SqlUtils.joinUsing(rTmp, als, COL_TRIP),
            SqlUtils.or(SqlUtils.joinMore(rTmp, COL_DATE, als, COL_DATE),
                SqlUtils.and(SqlUtils.joinUsing(rTmp, als, COL_DATE),
                    SqlUtils.joinMoreEqual(rTmp, COL_COUNTRY_CODE, als, COL_COUNTRY_CODE)))))
        .addGroup(rTmp, COL_TRIP, COL_DATE, COL_COUNTRY_CODE));

    qs.sqlDropTemp(rTmp);

    int c = BeeUtils.unbox(qs.getInt(new SqlSelect().addMax(routes, cnt).addFrom(routes)));

    if (c > 0) {
      qs.updateData(new SqlUpdate(tmp)
          .addExpression(COL_TRIP_ROUTE, SqlUtils.field(routes, COL_COUNTRY_CODE))
          .addExpression(lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE))
          .setFrom(routes, SqlUtils.and(SqlUtils.joinUsing(tmp, routes, COL_TRIP),
              SqlUtils.equals(routes, cnt, 1))));

      for (int i = 2; i <= c; i++) {
        qs.updateData(new SqlUpdate(tmp)
            .addExpression(COL_TRIP_ROUTE,
                SqlUtils.concat(SqlUtils.field(tmp, COL_TRIP_ROUTE), "'-'",
                    SqlUtils.field(routes, COL_COUNTRY_CODE)))
            .addExpression(lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE))
            .setFrom(routes, SqlUtils.and(SqlUtils.joinUsing(tmp, routes, COL_TRIP),
                SqlUtils.equals(routes, cnt, i),
                SqlUtils.notEqual(tmp, lastRoute, SqlUtils.field(routes, COL_COUNTRY_CODE)))));
      }
    }
    qs.sqlDropTemp(routes);
    return tmp;
  }

  private static SqlSelect getConstantsQuery(String tmp, String src, IsExpression factor,
      String als, Long currency) {

    SqlSelect ss = new SqlSelect()
        .addFrom(tmp)
        .addFromInner(src, SqlUtils.and(SqlUtils.notNull(tmp, COL_TRIP_DATE_FROM),
            SqlUtils.notNull(tmp, COL_TRIP_DATE_TO),
            SqlUtils.joinMore(tmp, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_FROM),
            SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_FROM),
                SqlUtils.joinLess(src, COL_TRIP_DATE_FROM, tmp, COL_TRIP_DATE_TO)),
            SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_TO),
                SqlUtils.joinMore(src, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_FROM))));

    IsExpression xpr = SqlUtils.multiply(SqlUtils.divide(SqlUtils.minus(
        SqlUtils.sqlIf(SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_TO),
            SqlUtils.joinMore(src, COL_TRIP_DATE_TO, tmp, COL_TRIP_DATE_TO)),
            SqlUtils.field(tmp, COL_TRIP_DATE_TO),
            SqlUtils.field(src, COL_TRIP_DATE_TO)),
        SqlUtils.sqlIf(SqlUtils.or(SqlUtils.isNull(src, COL_TRIP_DATE_FROM),
            SqlUtils.joinLess(src, COL_TRIP_DATE_FROM, tmp, COL_TRIP_DATE_FROM)),
            SqlUtils.field(tmp, COL_TRIP_DATE_FROM),
            SqlUtils.field(src, COL_TRIP_DATE_FROM))),
        TimeUtils.MILLIS_PER_DAY), SqlUtils.field(src, COL_CARGO_VALUE));

    if (Objects.nonNull(factor)) {
      xpr = SqlUtils.divide(SqlUtils.multiply(xpr, factor),
          SqlUtils.divide(SqlUtils.minus(SqlUtils.field(tmp, COL_TRIP_DATE_TO),
              SqlUtils.field(tmp, COL_TRIP_DATE_FROM)), TimeUtils.MILLIS_PER_DAY));
    }
    if (DataUtils.isId(currency)) {
      xpr = ExchangeUtils.exchangeFieldTo(ss, xpr,
          SqlUtils.field(src, COL_CURRENCY), SqlUtils.field(tmp, COL_TRIP_DATE),
          SqlUtils.constant(currency));
    } else {
      xpr = ExchangeUtils.exchangeField(ss, xpr,
          SqlUtils.field(src, COL_CURRENCY), SqlUtils.field(tmp, COL_TRIP_DATE));
    }
    return ss.addSum(xpr, als);
  }
}
