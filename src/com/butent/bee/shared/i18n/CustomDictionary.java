package com.butent.bee.shared.i18n;

public interface CustomDictionary {

    String g(String key);

    default String carriersReport() {return g("carriersReport");}

    default String customerId() {return g("customerId");}

    default String incomeId() {return g("incomeId");}

    default String prmForwarderExpeditionType() {return g("prmForwarderExpeditionType");}

    default String quantityUnit() {return g("quantityUnit");}

    default String supplierId() {return g("supplierId");}

    default String trCustomerId() {return g("trCustomerId");}

    default String trExpendituresAccount() {return g("trExpendituresAccount");}

    default String trExpendituresAccountNo() {return g("trExpendituresAccountNo");}

    default String trExpendituresAccountSeriesShort() {return g("trExpendituresAccountSeriesShort");}

    default String trExpendituresDate() {return g("trExpendituresDate");}

    default String trExpendituresOperation() {return g("trExpendituresOperation");}

    default String trExpendituresService() {return g("trExpendituresService");}

    default String trExpendituresSupplier() {return g("trExpendituresSupplier");}

    default String trExpendituresSupplierId() {return g("trExpendituresSupplierId");}

    default String trFinishedCountShort() {return g("trFinishedCountShort");}

    default String trIncomeInvoicesDate() {return g("trIncomeInvoicesDate");}

    default String trIncomeInvoicesNo() {return g("trIncomeInvoicesNo");}

    default String trIncomeInvoicesSeriesShort() {return g("trIncomeInvoicesSeriesShort");}

    default String trLogCountShort() {return g("trLogCountShort");}

    default String trOrdersId() {return g("trOrdersId");}

    default String trSupplierId() {return g("trSupplierId");}

    default String trdPayerId() {return g("trdPayerId");}

    default String trdSupplierId() {return g("trdSupplierId");}
}
