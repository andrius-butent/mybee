package com.butent.bee.client.modules.projects;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.render.AbstractSlackRenderer;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.modules.projects.ProjectStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public class ProjectSlackRenderer extends AbstractSlackRenderer {

  protected ProjectSlackRenderer(List<? extends IsColumn> columns) {
    super(columns);
  }

  @Override
  public DateTime getStartDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDate(DataUtils.getColumnIndex(COL_DATES_START_DATE, columns)).getDateTime();
  }

  @Override
  public DateTime getFinishDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDate(DataUtils.getColumnIndex(COL_DATES_END_DATE, columns)).getDateTime();
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    ProjectStatus status =
        EnumUtils.getEnumByIndex(ProjectStatus.class, Data.getInteger(VIEW_PROJECTS, row,
            COL_PROJECT_STATUS));
    if (status == null || status != ProjectStatus.ACTIVE) {
      return null;
    } else {
      return super.export(row, cellIndex, styleRef, sheet);
    }
  }

  @Override
  public String render(IsRow row) {
    ProjectStatus status =
        EnumUtils.getEnumByIndex(ProjectStatus.class, Data.getInteger(VIEW_PROJECTS, row,
            COL_PROJECT_STATUS));
    if (status == null || status != ProjectStatus.ACTIVE) {
      return null;
    } else {
      return super.render(row);
    }
  }

}
