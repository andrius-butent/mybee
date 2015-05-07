package com.butent.bee.client.imports;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.COL_IMPORT_MAPPING;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.render.AbstractCellRenderer;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.client.view.grid.interceptor.GridInterceptor;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.CellSource;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.IsColumn;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.administration.AdministrationConstants;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.ArrayList;
import java.util.List;

public class ImportMappingsGrid extends AbstractGridInterceptor {

  private final String viewName;
  private BeeRowSet cache;
  private boolean waiting;

  public ImportMappingsGrid(String viewName) {
    this.viewName = viewName;
  }

  @Override
  public ColumnDescription beforeCreateColumn(GridView gridView, ColumnDescription description) {
    if (BeeUtils.same(description.getId(), AdministrationConstants.COL_IMPORT_MAPPING)) {
      Relation relation = Data.getRelation(viewName);

      if (relation == null) {
        List<String> columns = new ArrayList<>();

        for (BeeColumn column : Data.getColumns(viewName)) {
          columns.add(column.getId());
        }
        relation = Relation.create(viewName, columns);
        LogUtils.getRootLogger().warning("Missing relation info:", viewName);
      }
      description.setRelation(relation);

      Queries.getRowSet(viewName, relation.getChoiceColumns(), new Queries.RowSetCallback() {
        @Override
        public void onSuccess(BeeRowSet result) {
          cache = result;

          if (getGridPresenter() != null && waiting) {
            getGridPresenter().refresh(true);
          }
        }
      });
    }
    return super.beforeCreateColumn(gridView, description);
  }

  @Override
  public GridInterceptor getInstance() {
    return null;
  }

  @Override public AbstractCellRenderer getRenderer(final String columnName,
      List<? extends IsColumn> dataColumns, ColumnDescription columnDescription,
      CellSource cellSource) {

    if (BeeUtils.same(columnName, COL_IMPORT_MAPPING)) {
      return new AbstractCellRenderer(null) {
        @Override
        public String render(IsRow row) {
          if (!BeeUtils.isEmpty(getViewName()) && cache != null) {
            Long mapping = Data.getLong(getViewName(), row, columnName);

            if (DataUtils.isId(mapping)) {
              BeeRow data = cache.getRowById(mapping);

              if (data != null) {
                return BeeUtils.joinWords(data.getValues());
              }
            }
          } else {
            waiting = true;
          }
          return null;
        }
      };
    }
    return super.getRenderer(columnName, dataColumns, columnDescription, cellSource);
  }
}
