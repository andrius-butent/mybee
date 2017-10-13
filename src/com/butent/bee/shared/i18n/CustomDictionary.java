package com.butent.bee.shared.i18n;

public interface CustomDictionary {

    String g (String key);

    default String qualityCheck() {return g("qualityCheck"); }

    default String checkCancellations() {return g("checkCancellations");}

    default String expectantCustomer() {return g("expectantCustomer");}

    default String symptom() {return g("symptom"); }

    default String symptomCode() {return g("symptomCode"); }

}
