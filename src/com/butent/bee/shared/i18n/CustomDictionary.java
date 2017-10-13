package com.butent.bee.shared.i18n;

public interface CustomDictionary {

    String g (String key);

    default String symptom() {return g("symptom"); }

    default String symptomCode() {return g("symptomCode"); }

    default String qualityCheck() {return g("qualityCheck"); }
}
