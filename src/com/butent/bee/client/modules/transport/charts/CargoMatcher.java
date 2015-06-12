package com.butent.bee.client.modules.transport.charts;

import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collection;

final class CargoMatcher {

  static CargoMatcher maybeCreate(Collection<ChartData> data) {
    if (BeeUtils.isEmpty(data)) {
      return null;
    }

    ChartData customerData = FilterHelper.getDataByType(data, ChartData.Type.CUSTOMER);
    ChartData managerData = FilterHelper.getDataByType(data, ChartData.Type.MANAGER);

    ChartData orderData = FilterHelper.getDataByType(data, ChartData.Type.ORDER);
    ChartData statusData = FilterHelper.getDataByType(data, ChartData.Type.ORDER_STATUS);

    ChartData cargoData = FilterHelper.getDataByType(data, ChartData.Type.CARGO);

    if (BeeUtils.anyNotNull(customerData, managerData, orderData, statusData, cargoData)) {
      return new CargoMatcher(customerData, managerData, orderData, statusData, cargoData);
    } else {
      return null;
    }
  }

  private final ChartData customerData;
  private final ChartData managerData;

  private final ChartData orderData;
  private final ChartData statusData;

  private final ChartData cargoData;

  private CargoMatcher(ChartData customerData, ChartData managerData, ChartData orderData,
      ChartData statusData, ChartData cargoData) {

    this.customerData = customerData;
    this.managerData = managerData;

    this.orderData = orderData;
    this.statusData = statusData;

    this.cargoData = cargoData;
  }

  boolean matches(OrderCargo cargo) {
    if (cargo == null) {
      return false;
    }

    if (customerData != null && !customerData.contains(cargo.getCustomerId())) {
      return false;
    }
    if (managerData != null && !managerData.contains(cargo.getManager())) {
      return false;
    }
    if (orderData != null && !orderData.contains(cargo.getOrderId())) {
      return false;
    }
    if (statusData != null && !statusData.contains(cargo.getOrderStatus())) {
      return false;
    }

    if (cargoData != null && !cargoData.contains(cargo.getCargoDescription())) {
      return false;
    }
    return true;
  }
}
