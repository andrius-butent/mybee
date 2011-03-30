package com.butent.bee.server.data;

import com.butent.bee.server.data.BeeTable.BeeField;
import com.butent.bee.shared.sql.HasFrom;
import com.butent.bee.shared.sql.SqlCreate;
import com.butent.bee.shared.sql.SqlInsert;
import com.butent.bee.shared.sql.SqlUpdate;

public interface HasTranslations {

  SqlCreate createTranslationTable(SqlCreate query, BeeField field);

  String getTranslationField(BeeField field, String locale);

  String getTranslationTable(BeeField field);

  SqlInsert insertTranslationField(SqlInsert query, long rootId, BeeField field, String locale,
      Object newValue);

  String joinTranslationField(HasFrom<?> query, String tblAlias, BeeField field, String locale);

  void setTranslationActive(BeeField field, String... flds);

  SqlUpdate updateTranslationField(SqlUpdate query, long rootId, BeeField field, String locale,
      Object newValue);
}
