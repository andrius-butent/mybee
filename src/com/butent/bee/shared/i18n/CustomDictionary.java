package com.butent.bee.shared.i18n;

public interface CustomDictionary {

  String g(String key);

  default String customerId() {return g("customerId");}

  default String expensesEnteredPerson() {
    return g("expensesEnteredPerson");
  }

  default String trCustomerId() {
    return g("trCustomerId");
  }

  default String trERPTripCosts() { return g("trERPTripCosts");}

  default String trInsuranceCertificate() { return g("trInsuranceCertificate");}

  default String trMultipleSegments() {return g("trMultipleSegments");}

  default String trOrdersSumWithProforma() {
    return g("trOrdersSumWithProforma");
  }

  default String trSupplierId() { return g("trSupplierId"); }

  default String trTotalLiabilities() {
    return g("trTotalLiabilities");
  }

  default String trdPayerId() {
    return g("trdPayerId");
  }

  default String trdSupplierId() {
    return g("trdSupplierId");
  }
}