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
import com.butent.bee.shared.data.RelationUtils;
import com.butent.bee.shared.data.view.DataInfo;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import static com.butent.bee.shared.modules.documents.DocumentConstants.*;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.VIEW_LOCATIONS;
import static com.butent.bee.shared.modules.payroll.PayrollConstants.FORM_LOCATION;

public class AllDocumentsGrid extends AbstractGridInterceptor {

    @Override
    public boolean beforeAddRow(final GridPresenter presenter, boolean copy) {
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
                RelationUtils.updateRow(info, COL_DOCUMENT_COMPANY, docRow,
                        Data.getDataInfo(parentViewName), parentRow, true);
                RelationUtils.updateRow(info, FORM_LOCATION, docRow,
                        Data.getDataInfo(parentViewName), parentRow, true);
            }
        }
        RowFactory.createRow(info, docRow, Opener.MODAL, result -> {
            if (isAttached()) {
                    presenter.handleAction(Action.REFRESH);
                    ViewHelper.getForm(presenter.getGridView().asWidget()).refresh();
                }
            });


        return false;
    }
}
