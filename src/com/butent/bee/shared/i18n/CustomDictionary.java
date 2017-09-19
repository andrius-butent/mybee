package com.butent.bee.shared.i18n;

public interface CustomDictionary {

    String g(String key);

    default String carriersReport() {return g("carriersReport");}

    default String prmForwarderExpeditionType() {return g("prmForwarderExpeditionType");}

    default String quantityUnit() {return g("quantityUnit");}

    default String trFinishedCountShort() {return g("trFinishedCountShort");}

    default String trLogCountShort() {return g("trLogCountShort");}
}