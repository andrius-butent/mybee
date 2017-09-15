package com.butent.bee.shared.i18n;

public interface CustomDictionary {

  String g(String key);

  default String expensesEnteredPerson() {
    return g("expensesEnteredPerson");
  }

  default String trOrdersSumWithProforma() {
    return g("trOrdersSumWithProforma");
  }

  default String trTotalLiabilities() {
    return g("trTotalLiabilities");
  }

  default String customerId() {return g("customerId");}

  default String trCustomerId() {
    return g("trCustomerId");
  }

  default String trSupplierId() { return g("trSupplierId"); }

  default String trdPayerId() {
    return g("trdPayerId");
  }

  default String trdSupplierId() {
    return g("trdSupplierId");
  }
}