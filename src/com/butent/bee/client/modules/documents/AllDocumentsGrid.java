package com.butent.bee.client.modules.documents;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.RowFactory;
import com.butent.bee.client.presenter.GridPresenter;
import com.butent.bee.client.ui.Opener;
import com.butent.bee.client.view.ViewHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.grid.GridView;
import com.butent.bee.client.view.grid.interceptor.AbstractGridInterceptor;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.VIEW_LOCATIONS;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.FORM_LOCATION;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.COL_LOCATION_NAME;
import static com.butent.bee.shared.modules.service.ServiceConstants.ALS_COMPANY_TYPE_NAME;

public class AllDocumentsGrid extends AbstractGridInterceptor {

    @Override
    public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
        presenter.getGridView().ensureRelId(result -> {
            FormView parentForm = null;
            IsRow parentRow = null;
            String parentViewName = null;

            DataInfo info = Data.getDataInfo(VIEW_DOCUMENTS);
            BeeRow docRow = RowFactory.createEmptyRow(info, true);
            GridView gridView = presenter.getGridView();

            if (gridView != null) {
                parentForm = ViewHelper.getForm(gridView.asWidget());
            }
            if (parentForm != null) {
                parentRow = parentForm.getActiveRow();
            }
            if (parentForm != null) {
                parentViewName = parentForm.getViewName();
            }
            if (parentRow != null && !BeeUtils.isEmpty(parentViewName)) {
                if (BeeUtils.same(parentViewName, VIEW_LOCATIONS)) {
                    docRow.setValue(info.getColumnIndex(FORM_LOCATION), result);
                    for (String col : new String[] {COL_LOCATION_NAME, ALS_DOCUMENT_COMPANY_NAME,
                            ALS_COMPANY_TYPE_NAME, COL_DOCUMENT_COMPANY }) {
                        docRow.setValue(info.getColumnIndex(col), parentForm.getStringValue(col));
                    }
                }
            }
            RowFactory.createRow(info, docRow, Opener.MODAL, row -> {
                if (isAttached()) {
                    presenter.handleAction(Action.REFRESH);
                    ViewHelper.getForm(presenter.getGridView().asWidget()).refresh();
                }
            });
        });
        return false;
    }
}
