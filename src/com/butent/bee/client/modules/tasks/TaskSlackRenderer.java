package com.butent.bee.client.modules.tasks;

import static com.butent.bee.shared.modules.tasks.TaskConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.render.AbstractSlackRenderer;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.export.XCell;
import com.butent.bee.shared.export.XSheet;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants.TaskStatus;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.EnumUtils;

import java.util.List;

public class TaskSlackRenderer extends AbstractSlackRenderer {

  TaskSlackRenderer(List<? extends IsColumn> columns) {
    super(columns);
  }

  @Override
  public DateTime getStartDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDateTime(DataUtils.getColumnIndex(TaskConstants.COL_START_TIME, columns));
  }

  @Override
  public DateTime getFinishDateTime(List<? extends IsColumn> columns, IsRow row) {
    return row.getDateTime(DataUtils.getColumnIndex(TaskConstants.COL_FINISH_TIME, columns));
  }

  @Override
  public XCell export(IsRow row, int cellIndex, Integer styleRef, XSheet sheet) {
    TaskStatus status =
        EnumUtils.getEnumByIndex(TaskStatus.class, Data.getInteger(VIEW_TASKS, row, COL_STATUS));
    if (status == null || status == TaskStatus.COMPLETED || status == TaskStatus.CANCELED
        || status == TaskStatus.APPROVED) {
      return null;
    } else {
      return super.export(row, cellIndex, styleRef, sheet);
    }
  }

  @Override
  public String render(IsRow row) {
    TaskStatus status =
        EnumUtils.getEnumByIndex(TaskStatus.class, Data.getInteger(VIEW_TASKS, row, COL_STATUS));
    if (status == null || status == TaskStatus.COMPLETED || status == TaskStatus.CANCELED
        || status == TaskStatus.APPROVED) {
      return null;
    } else {
      return super.render(row);
    }
  }

}
