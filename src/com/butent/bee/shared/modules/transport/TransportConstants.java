package com.butent.bee.shared.modules.transport;

import com.butent.bee.shared.Service;
import com.butent.bee.shared.ui.HasCaption;

public class TransportConstants {

  public enum OrderStatus implements HasCaption {
    CREATED("Naujas"),
    ACTIVATED("Aktyvus"),
    CONFIRMED("Patvirtintas"),
    CANCELED("Atšauktas"),
    COMPLETED("Baigtas");

    private final String caption;

    private OrderStatus(String caption) {
      this.caption = caption;
    }

    @Override
    public String getCaption() {
      return caption;
    }
  }

  public static final String TRANSPORT_MODULE = "Transport";
  public static final String TRANSPORT_METHOD = TRANSPORT_MODULE + "Method";

  public static final String SVC_GET_BEFORE = "GetBeforeData";
  public static final String SVC_GET_PROFIT = "GetProfit";
  public static final String SVC_GET_FX_DATA = "GetFxData";

  public static final String VAR_TRIP_ID = Service.RPC_VAR_PREFIX + "trip_id";
  public static final String VAR_CARGO_ID = Service.RPC_VAR_PREFIX + "cargo_id";
  public static final String VAR_ORDER_ID = Service.RPC_VAR_PREFIX + "order_id";

  public static final String TBL_TRIPS = "Trips";

  public static final String TBL_TRANSPORT_SETTINGS = "TransportSettings";
  
  public static final String TBL_ORDERS = "TransportationOrders";
  public static final String TBL_ORDER_CARGO = "OrderCargo";
  public static final String TBL_CARGO_TRIPS = "CargoTrips";
  public static final String TBL_CARGO_PLACES = "CargoPlaces";

  public static final String VIEW_ORDERS = "TransportationOrders";

  public static final String VIEW_CARGO = "OrderCargo";
  public static final String VIEW_CARGO_SERVICES = "CargoServices";
  public static final String VIEW_CARGO_TRIPS = "CargoTrips";

  public static final String VIEW_TRIPS = TBL_TRIPS;
  public static final String VIEW_EXP_TRIPS = "ExpeditionTrips";
  public static final String VIEW_ALL_TRIPS = "AllTrips";

  public static final String VIEW_TRIP_CARGO = "TripCargo";
  public static final String VIEW_TRIP_ROUTES = "TripRoutes";
  public static final String VIEW_TRIP_COSTS = "TripCosts";
  public static final String VIEW_TRIP_FUEL_COSTS = "TripFuelCosts";
  public static final String VIEW_TRIP_FUEL_CONSUMPTIONS = "TripFuelConsumptions";

  public static final String VIEW_VEHICLES = "Vehicles";
  public static final String VIEW_FUEL_CONSUMPTIONS = "FuelConsumptions";
  public static final String VIEW_FUEL_TEMPERATURES = "FuelTemperatures";

  public static final String VIEW_SPARE_PARTS = "SpareParts";

  public static final String VIEW_TRANSPORT_SETTINGS = "TransportSettings";
  
  public static final String COL_TRIP = "Trip";
  public static final String COL_CARGO = "Cargo";
  public static final String COL_CARGO_ID = "CargoID";

  public static final String COL_STATUS = "Status";
  public static final String COL_OWNER = "Owner";
  public static final String COL_OWNER_NAME = "OwnerName";
  public static final String COL_PARENT_MODEL_NAME = "ParentModelName";
  public static final String COL_MODEL_NAME = "ModelName";
  public static final String COL_NUMBER = "Number";

  public static final String COL_ORDER = "Order";
  public static final String COL_ORDER_NO = "OrderNo";
  public static final String COL_ORDER_DATE = "Date";
  public static final String COL_CUSTOMER = "Customer";
  public static final String COL_CUSTOMER_NAME = "CustomerName";

  public static final String COL_DESCRIPTION = "Description";

  public static final String COL_UNLOADING_PLACE = "UnloadingPlace";
  public static final String COL_LOADING_PLACE = "LoadingPlace";

  public static final String COL_PLACE_DATE = "Date";
  public static final String COL_COUNTRY = "Country";
  public static final String COL_PLACE = "Place";
  public static final String COL_TERMINAL = "Terminal";

  public static final String COL_PLACE_NAME = "PlaceName";

  public static final String COL_USER = "User";

  public static final String COL_FX_PIXELS_PER_CUSTOMER = "FxPixelsPerCustomer";
  public static final String COL_FX_PIXELS_PER_ORDER = "FxPixelsPerOrder";
  public static final String COL_FX_PIXELS_PER_DAY = "FxPixelsPerDay";
  public static final String COL_FX_PIXELS_PER_ROW = "FxPixelsPerRow";
  public static final String COL_FX_HEADER_HEIGHT = "FxHeaderHeight";
  public static final String COL_FX_FOOTER_HEIGHT = "FxFooterHeight";
  public static final String COL_FX_SLIDER_WIDTH = "FxSliderWidth";
  public static final String COL_FX_THEME = "FxTheme";
  
  public static final String FORM_NEW_VEHICLE = "NewVehicle";
  public static final String FORM_ORDER = "TransportationOrder";
  public static final String FORM_TRIP = "Trip";
  public static final String FORM_CARGO = "OrderCargo";

  public static final String PROP_COLORS = "Colors";
  public static final String PROP_COUNTRIES = "Countries";
  public static final String PROP_DATA = "Data";
  
  public static final String loadingColumnAlias(String colName) {
    return "Loading" + colName;
  }

  public static final String unloadingColumnAlias(String colName) {
    return "Unloading" + colName;
  }
  
  private TransportConstants() {
  }
}
