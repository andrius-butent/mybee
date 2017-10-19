package com.butent.bee.client.modules.cars;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.Widget;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;
import static com.butent.bee.shared.modules.mail.MailConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.composite.DataSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.FormFactory;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.add.ReadyForInsertEvent;
import com.butent.bee.client.view.edit.EditableWidget;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.view.form.interceptor.FormInterceptor;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.event.DataChangeEvent;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.discussions.DiscussionsConstants;
import com.butent.bee.shared.modules.projects.ProjectConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.modules.trade.TradeConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.HashMap;
import java.util.Map;

public class CarServiceCommentForm extends AbstractFormInterceptor {

  private static final String NAME_COMMENT_TEMPLATE = "CommentTemplate";
  private static final String NAME_SUBJECT_LABEL = "SubjectLabel";

  private IsRow serviceRow;

  public CarServiceCommentForm(IsRow serviceRow) {
    super();
    this.serviceRow = serviceRow;
  }

  @Override
  public void afterCreateEditableWidget(EditableWidget editableWidget, IdentifiableWidget widget) {
    if (BeeUtils.same(editableWidget.getColumnId(), COL_SEND_EMAIL)) {
      CheckBox cb = (CheckBox) widget;
      cb.addValueChangeHandler(valueChangeEvent -> setSubjectStyle(cb.isChecked()));
    }

    super.afterCreateEditableWidget(editableWidget, widget);
  }

  @Override
  public void afterCreateWidget(String name, IdentifiableWidget widget,
      FormFactory.WidgetDescriptionCallback callback) {

    if (BeeUtils.same(name, NAME_COMMENT_TEMPLATE)) {
      DataSelector template = (DataSelector) widget;

      template.addSelectorHandler(event -> {
        if (event.isClosed()) {
          template.clearDisplay();
        }

        IsRow templateRow = event.getRelatedRow();
        if (templateRow == null) {
          template.clearDisplay();
          return;
        }

        FormView form = getFormView();
        IsRow serviceComment = getActiveRow();

        Map<String, String> columnMap = new HashMap<>();
        columnMap.put(COL_SUBJECT, DiscussionsConstants.COL_NAME);
        columnMap.put(ProjectConstants.COL_COMMENT, COL_CONTENT);

        for (String key : columnMap.keySet()) {
          serviceComment.setValue(form.getDataIndex(key),
              templateRow.getString(Data.getColumnIndex(TBL_CAR_MESSAGE_TEMPLATES,
                  columnMap.get(key))));

          form.refreshBySource(key);
        }
      });
    }

    super.afterCreateWidget(name, widget, callback);
  }

  @Override
  public void afterInsertRow(IsRow row, boolean forced) {

    DateTime term = Data.getDateTime(TBL_CAR_SERVICE_COMMENTS, row, TradeConstants.COL_TRADE_TERM);
    boolean isEmail = BeeUtils.unbox(Data.getBoolean(TBL_CAR_SERVICE_COMMENTS, row,
        COL_SEND_EMAIL));
    boolean isSms = BeeUtils.unbox(Data.getBoolean(TBL_CAR_SERVICE_COMMENTS, row, COL_SEND_SMS));

    if (term == null && (isEmail || isSms)) {
      ParameterList params = CarsKeeper.createSvcArgs(SVC_INFORM_CUSTOMER);
      params.addDataItem(TaskConstants.COL_ID, row.getId());

      BeeKeeper.getRpc().makePostRequest(params, response -> {

        if (!response.hasErrors()) {
          DataChangeEvent.fireLocalRefresh(BeeKeeper.getBus(), TBL_CAR_SERVICE_COMMENTS);
        } else {
          BeeKeeper.getScreen().notifyWarning(response.getErrors());
        }
      });
    }

    super.afterInsertRow(row, forced);
  }

  @Override
  public void onReadyForInsert(HasHandlers listener, ReadyForInsertEvent event) {
    event.consume();

    boolean isEmail = BeeUtils.unbox(Data.getBoolean(TBL_CAR_SERVICE_COMMENTS, getActiveRow(),
        COL_SEND_EMAIL));
    String subject = Data.getString(TBL_CAR_SERVICE_COMMENTS, getActiveRow(), COL_SUBJECT);

    if (isEmail && BeeUtils.isEmpty(subject)) {
      event.getCallback().onFailure(Localized.dictionary().fieldRequired(
          Localized.dictionary().mailSubject()));
      return;
    }

    listener.fireEvent(event);
  }

  @Override
  public FormInterceptor getInstance() {
    return new CarServiceCommentForm(serviceRow);
  }

  @Override
  public void onStartNewRow(FormView form, IsRow row) {
    if (serviceRow != null) {
      row.setValue(getDataIndex(FORM_CAR_SERVICE_ORDER), serviceRow.getId());
      row.setValue(getDataIndex(TaskConstants.COL_EVENT_NOTE),
          serviceRow.getString(Data.getColumnIndex(TBL_SERVICE_ORDERS, ALS_STAGE_NAME)));
    }

    super.onStartNewRow(form, row);
  }

  private void setSubjectStyle(boolean required) {
    Widget subjectLabel = getFormView().getWidgetByName(NAME_SUBJECT_LABEL);

    if (subjectLabel != null) {
      subjectLabel.setStyleName(StyleUtils.NAME_REQUIRED, required);
    }
  }
}