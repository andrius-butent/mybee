package com.butent.bee.client.modules.cars;

import com.google.common.collect.Sets;

import static com.butent.bee.shared.modules.cars.CarsConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.eventsboard.EventsBoard;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.view.form.interceptor.AbstractFormInterceptor;
import com.butent.bee.client.widget.CheckBox;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.data.view.Order;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.classifiers.ClassifierConstants;
import com.butent.bee.shared.modules.mail.MailConstants;
import com.butent.bee.shared.modules.tasks.TaskConstants;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Set;

public class CarServiceEventsHandler extends EventsBoard {

  private static final String STYLE_PREFIX = BeeConst.CSS_CLASS_PREFIX + "cars-Events-";
  private static final String STYLE_INFO_PANEL = STYLE_PREFIX + "info-panel";
  private static final String CELL_EVENT_NOTE_HEADER = "EventNoteHeader";

  private static final Set<Action> enabledActions = Sets.newHashSet(Action.ADD, Action.REFRESH);

  private IsRow serviceRow;
  private boolean canCreate;

  public CarServiceEventsHandler() {
    boolean readOnly = !Data.isViewEditable(getEventsDataViewName());
    canCreate = !readOnly && BeeKeeper.getUser().canCreateData(getEventsDataViewName());
  }

  @Override
  protected void afterCreateCellContent(BeeRowSet rs, BeeRow row, Flow widget) {
    Flow infoPanel = new Flow(STYLE_INFO_PANEL);
    infoPanel.addStyleName(StyleUtils.NAME_FLEX_BOX_HORIZONTAL);

    createCheckBox(infoPanel, rs, row, COL_SEND_EMAIL, false,
        Localized.dictionary().svcSendEmail());
    createCheckBox(infoPanel, rs, row, COL_SEND_SMS, false,
        Localized.dictionary().svcSendSms());

    widget.add(infoPanel);

    super.afterCreateCellContent(rs, row, widget);
  }

  @Override
  protected void beforeCreateEventNoteCell(BeeRowSet rs, BeeRow row, Flow widget) {

    int idxName = rs.getColumnIndex(MailConstants.COL_SUBJECT);
    if (BeeUtils.isNegative(idxName)) {
      return;
    }

    Flow cell = new Flow();
    if (!BeeUtils.isEmpty(row.getString(idxName))) {
      cell.add(createCellHtmlItem(CELL_EVENT_NOTE_HEADER, row.getString(idxName), rs, row));
    }

    widget.add(cell);

    super.beforeCreateEventNoteCell(rs, row, widget);
  }

  @Override
  protected IdentifiableWidget getAddEventActionWidget() {
    FaLabel label = new FaLabel(FontAwesome.COMMENT_O);
    label.setTitle(Localized.dictionary().crmActionComment());
    return label;
  }

  @Override
  protected String getAddEventFromName() {
    return TBL_CAR_SERVICE_COMMENTS;
  }

  @Override
  public Set<Action> getDisabledActions() {
    if (!canCreate) {
      return  Sets.newHashSet(Action.ADD);
    }
    return super.getDisabledActions();
  }

  @Override
  protected Set<Action> getEnabledActions() {
    if (canCreate) {
      return enabledActions;
    } else {
      return Sets.newHashSet(Action.REFRESH);
    }
  }

  @Override
  protected Order getEventsDataOrder() {
    return Order.ascending(getPublishTimeColumnName());
  }

  @Override
  protected String getEventsDataViewName() {
    return TBL_CAR_SERVICE_COMMENTS;
  }

  @Override
  protected String getEventNoteColumnName() {
    return TaskConstants.COL_COMMENT;
  }

  @Override
  protected String getEventTypeColumnName() {
    return TaskConstants.COL_EVENT_NOTE;
  }

  @Override
  protected AbstractFormInterceptor getNewEventFormInterceptor() {
    return new CarServiceCommentForm(serviceRow);
  }

  @Override
  protected String getPublisherPhotoColumnName() {
    return ClassifierConstants.COL_PHOTO;
  }

  @Override
  protected String getPublisherFirstNameColumnName() {
    return TaskConstants.ALS_PUBLISHER_FIRST_NAME;
  }

  @Override
  protected String getPublisherLastNameColumnName() {
    return TaskConstants.ALS_PUBLISHER_LAST_NAME;
  }

  @Override
  protected String getPublishTimeColumnName() {
    return TaskConstants.COL_PUBLISH_TIME;
  }

  @Override
  protected String getRelatedColumnName() {
    return FORM_CAR_SERVICE_ORDER;
  }

  @Override
  protected String getStylePrefix() {
    return STYLE_PREFIX;
  }

  @Override
  public String getViewKey() {
    return null;
  }

  @Override
  public String getCaption() {
    return Localized.dictionary().svcComments();
  }

  public void setServiceRow(IsRow row) {
    this.serviceRow = row;
  }

  private void createCheckBox(Flow parentFlow, BeeRowSet rs, BeeRow row, String column,
      boolean enabled, String text) {
    Flow rowCell = createEventRowCell(parentFlow, column, null, false);
    int idxCol = rs.getColumnIndex(column);

    if (!BeeUtils.isNegative(idxCol) && BeeKeeper.getUser()
        .isColumnVisible(Data.getDataInfo(getEventsDataViewName()), column)) {
      CheckBox checkBox = new CheckBox();
      checkBox.setChecked(BeeUtils.toBoolean(row.getString(idxCol)));
      checkBox.setEnabled(enabled && BeeKeeper.getUser().canEditColumn(getEventsDataViewName(),
          column));
      checkBox.setText(text);
      rowCell.add(createCellWidgetItem(column, checkBox));
    }
  }
}