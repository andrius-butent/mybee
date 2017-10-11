package com.butent.bee.shared.i18n;

public interface CustomDictionary {

    String g (String key);

    default String checkCancellations() {return g("checkCancellations");}

    default String expectantCustomer() {return g("expectantCustomer");}
}
