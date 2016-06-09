package com.butent.bee.client.dialog;

import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.administration.AdministrationConstants.*;
import static com.butent.bee.shared.modules.classifiers.ClassifierConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ParameterList;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.composite.UnboundSelector;
import com.butent.bee.client.data.Data;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowCallback;
import com.butent.bee.client.i18n.DateTimeFormat;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.widget.Button;
import com.butent.bee.client.widget.FaLabel;
import com.butent.bee.client.widget.InputDateTime;
import com.butent.bee.client.widget.TextLabel;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.font.FontAwesome;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.rights.Module;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.ui.Relation;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReminderDialog extends DialogBox {
  private static final String STYLE_DIALOG = BeeConst.CSS_CLASS_PREFIX + "reminder-dialog";
  private static final String STYLE_DIALOG_INFO_PANEL = STYLE_DIALOG + "-infoPanel";
  private static final String STYLE_DIALOG_INFO_TEXT = STYLE_DIALOG + "-infoText";
  private static final String STYLE_DIALOG_DATE_TEXT = STYLE_DIALOG + "-dateText";
  private static final String STYLE_DIALOG_PANEL = STYLE_DIALOG + "-panel";
  private static final String STYLE_DIALOG_BUTTON = STYLE_DIALOG + "-button";
  private static final String STYLE_DIALOG_SUSPEND_BUTTON = STYLE_DIALOG + "-suspendButton";
  private static final String STYLE_DIALOG_REMIND_BUTTON = STYLE_DIALOG + "-remindButton";
  private static final String STYLE_DIALOG_DATE_INPUT = STYLE_DIALOG + "-dateInput";
  private static final String STYLE_DIALOG_SELECTOR = STYLE_DIALOG + "-selectorInput";
  private static final String STYLE_ELEMENT_NOT_VISIBLE = STYLE_DIALOG + "-not-visible";

  private static final String REMINDER_ACTIVE = "bee-reminder-dialog-active";
  private static final String REMINDER_SUSPENDED = "bee-reminder-dialog-suspended";


  private BeeRow dataRow;
  private Map<Integer, DateTime> datesByField;
  private TextLabel dateTextLabel;
  private InputDateTime dateTimeInput;
  private Filter flt;
  private Module module;
  private long objectId;
  private FaLabel reminderLabel;
  private long userId;
  private UnboundSelector selector;


  public ReminderDialog(Module module, long remindForId, long userId) {
    super(Localized.dictionary().userReminder(), null);

    this.module = module;
    this.objectId = remindForId;
    this.userId = userId;

    flt = Filter.and(Filter.equals(COL_USER_REMINDER_OBJECT, remindForId),
        Filter.equals(COL_USER_REMINDER_USER, userId),
        Filter.equals(COL_USER_REMINDER_OBJECT_MODULE, module.ordinal()));

    addDefaultCloseBox();

    reminderLabel = createOrUpdateReminderLabel();

    super.addCloseHandler(event -> createOrUpdateReminderLabel());
  }

  public void createDialog(Map<Integer, DateTime> datesByFieldType) {
    this.datesByField = datesByFieldType;

    Queries.getRowSet(VIEW_USER_REMINDERS, null, flt, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          dataRow = null;
        } else {
          dataRow = result.getRow(0);
        }
        setWidget(generateReminderWidget());
        display();
      }
    });

  }

  private Flow generateReminderWidget() {
    Flow mainPanel = new Flow();

    Flow selectionPanel = new Flow(STYLE_DIALOG_PANEL);
    selectionPanel.add(createTextLabelWidget(Localized.dictionary().userReminderSendRemind(),
        null));

    Long dataTypeId = dataRow == null ? null : dataRow.getLong(
        Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TYPE));

    createSelectorWidget(dataTypeId);
    selectionPanel.add(selector);

    createDateTimeWidget(dataTypeId);
    selectionPanel.add(dateTimeInput);

    mainPanel.add(selectionPanel);

    Flow infoPanel = new Flow(STYLE_DIALOG_INFO_PANEL);
    infoPanel.add(createTextLabelWidget(Localized.dictionary().userReminderDataLabel(),
        STYLE_DIALOG_INFO_TEXT));

    dateTextLabel = new TextLabel(true);
    dateTextLabel.addStyleName(STYLE_DIALOG_DATE_TEXT);
    infoPanel.add(dateTextLabel);

    mainPanel.add(infoPanel);

    if (dataRow != null) {
      calculateReminderTime(selector.getRelatedRow(),
          dataRow.getDateTime(Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TIME)));
    }

    Flow buttonsPanel = new Flow(STYLE_DIALOG_PANEL);

    if (dataRow == null) {
      buttonsPanel.add(createReminderButton());

    } else {
      buttonsPanel.add(createUpdateButton());
      Boolean active = dataRow.getBoolean(
          Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE));
      if (active != null && active) {
        buttonsPanel.add(createSuspendReminderButton());
      }
    }

    buttonsPanel.add(createOtherTimeButton());
    buttonsPanel.add(createCancelButton());

    mainPanel.add(buttonsPanel);
    return mainPanel;
  }

  public FaLabel getReminderLabel() {
    return reminderLabel;
  }

  private Button createCancelButton() {
    final Button cancelButton =
        new Button(Localized.dictionary().userReminderCancel(), event -> {
          close();
        });
    cancelButton.addStyleName(STYLE_DIALOG_BUTTON);
    return cancelButton;
  }

  private void calculateReminderTime(BeeRow selectorRow, DateTime dateTime) {
    if (selectorRow != null) {
      Integer dataField = selectorRow.getInteger(
          Data.getColumnIndex(VIEW_REMINDER_TYPES, COL_REMINDER_DATA_FIELD));

      if (datesByField.containsKey(dataField)) {
        ParameterList params = BeeKeeper.getRpc().createParameters(Module.CLASSIFIERS,
            SVC_GET_USER_REMINDER_TIME);
        params.addQueryItem(VAR_OBJECT_DATE, datesByField.get(dataField).getTime());
        params.addQueryItem(VAR_REMINDER_TYPE_ID, selector.getRelatedId());

        BeeKeeper.getRpc().makeRequest(params, new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (!response.isEmpty()) {
              DateTime calculatedDate = new DateTime();
              calculatedDate.setTime(BeeUtils.toLong(response.getResponseAsString()));
              if (calculatedDate != null) {
                dateTimeInput.setDateTime(calculatedDate);
                dateTextLabel.setText(formatDate(calculatedDate));
              }
            }
          }
        });
      }

    } else if (dateTime != null) {
      dateTextLabel.setText(formatDate(dateTime));
    }

  }

  private void createDateTimeWidget(Long dataTypeId) {
    DateTime dataValue = dataRow == null ? null : dataRow.getDateTime(
        Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_TIME));

    dateTimeInput = new InputDateTime();
    dateTimeInput.addStyleName(STYLE_DIALOG_DATE_INPUT);
    dateTimeInput.setDateTimeFormat(DateTimeFormat.getFormat(
        DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT));

    if (dataValue != null) {
      dateTimeInput.setDateTime(dataValue);
    }

    if (dataRow == null || dataTypeId != null) {
      dateTimeInput.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
    }

    dateTimeInput.addEditStopHandler(valueChangeEvent ->
        calculateReminderTime(null, dateTimeInput.getDateTime()));
    dateTimeInput.addInputHandler(valueChangeEvent ->
        calculateReminderTime(null, dateTimeInput.getDateTime()));
  }

  public FaLabel createOrUpdateReminderLabel() {
    if (reminderLabel == null) {
      reminderLabel = new FaLabel(FontAwesome.ALARM_CLOCK);
      reminderLabel.setTitle(Localized.dictionary().userReminder());
    }

    Queries.getRowSet(VIEW_USER_REMINDERS, null, flt, new Queries.RowSetCallback() {
      @Override
      public void onSuccess(BeeRowSet result) {
        if (result.isEmpty()) {
          return;
        }

        BeeRow data = result.getRow(0);
        Boolean active = data.getBoolean(
            Data.getColumnIndex(VIEW_USER_REMINDERS, COL_USER_REMINDER_ACTIVE));
        if (active != null && active) {
          reminderLabel.removeStyleName(REMINDER_SUSPENDED);
          reminderLabel.addStyleName(REMINDER_ACTIVE);

        } else {
          reminderLabel.removeStyleName(REMINDER_ACTIVE);
          reminderLabel.addStyleName(REMINDER_SUSPENDED);
        }
      }
    });

    return reminderLabel;
  }

  private Button createOtherTimeButton() {
    final Button otherTimeButton =
        new Button(Localized.dictionary().userReminderOtherTime(), event -> {
          if (selector.getStyleName().contains(STYLE_ELEMENT_NOT_VISIBLE)) {
            selector.setValue(null, true);
            selector.removeStyleName(STYLE_ELEMENT_NOT_VISIBLE);
            dateTimeInput.clearValue();
            dateTimeInput.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
          } else {
            selector.setValue(null, true);
            selector.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
            dateTimeInput.clearValue();
            dateTimeInput.removeStyleName(STYLE_ELEMENT_NOT_VISIBLE);
          }
          dateTextLabel.setText(null);
        });
    otherTimeButton.addStyleName(STYLE_DIALOG_BUTTON);
    return otherTimeButton;
  }

  private Button createReminderButton() {
    final Button createReminderButton =
        new Button(Localized.dictionary().userRemind(), event -> {
          Long time =
              dateTimeInput.getDateTime() == null ? null : dateTimeInput.getDateTime().getTime();

          if (time != null && System.currentTimeMillis() < time) {
            final List<BeeColumn> columns = Data.getColumns(VIEW_USER_REMINDERS,
                Lists.newArrayList(COL_USER_REMINDER_OBJECT, COL_USER_REMINDER_OBJECT_MODULE,
                    COL_USER_REMINDER_TYPE, COL_USER_REMINDER_USER,
                    COL_USER_REMINDER_TIME, COL_USER_REMINDER_ACTIVE));

            Long selectorId = selector.getRelatedId();
            List<String> values = Lists.newArrayList(BeeUtils.toString(objectId),
                BeeUtils.toString(module.ordinal()),
                selectorId != null ? BeeUtils.toString(selectorId) : null,
                BeeUtils.toString(userId),
                selectorId == null ? BeeUtils.toString(time) : null, BeeConst.STRING_TRUE);

            Queries.insert(VIEW_USER_REMINDERS, columns, values, null, new RowCallback() {
              @Override
              public void onSuccess(BeeRow row) {
                close();
              }
            });
          } else {
            showDateError();
          }
        });
    createReminderButton.addStyleName(STYLE_DIALOG_REMIND_BUTTON);
    return createReminderButton;
  }

  private void createSelectorWidget(Long dataTypeId) {
    Relation relation = Relation.create(VIEW_REMINDER_TYPES, Lists.newArrayList(COL_REMINDER_NAME));
    selector = UnboundSelector.create(relation);
    if (dataTypeId != null) {
      selector.setValue(dataTypeId, true);
    }
    selector.addStyleName(STYLE_DIALOG_SELECTOR);
    selector.getOracle().setAdditionalFilter(
        Filter.equals(COL_REMINDER_MODULE, module.ordinal()), true);
    selector.addSelectorHandler(event -> {
      if (event.isChanged() && DataUtils.hasId(event.getRelatedRow())) {
        if (event != null) {
          calculateReminderTime(event.getRelatedRow(), null);
        }
      }
    });
    if (dataRow != null && dataTypeId == null) {
      selector.addStyleName(STYLE_ELEMENT_NOT_VISIBLE);
    }
  }

  private Button createSuspendReminderButton() {
    final Button suspendReminderButton =
        new Button(Localized.dictionary().userReminderSuspend(), event -> {

          Queries.update(VIEW_USER_REMINDERS, flt, COL_USER_REMINDER_ACTIVE,
              BeeConst.STRING_FALSE, new Queries.IntCallback() {
                @Override
                public void onSuccess(Integer result) {
                  close();
                }
              });
        });
    suspendReminderButton.addStyleName(STYLE_DIALOG_SUSPEND_BUTTON);
    return suspendReminderButton;
  }

  private Button createUpdateButton() {
    final Button updateReminderButton =
        new Button(Localized.dictionary().userReminderUpdate(), event -> {
          Long time = dateTimeInput.getDateTime() == null ? null
              : dateTimeInput.getDateTime().getTime();

          if (time != null && System.currentTimeMillis() < time) {
            List<String> columns = Lists.newArrayList(COL_USER_REMINDER_TYPE,
                COL_USER_REMINDER_TIME,
                COL_USER_REMINDER_ACTIVE);

            Long selectorId = selector.getRelatedId();
            List<String> values = Lists.newArrayList(
                selectorId != null ? BeeUtils.toString(selectorId) : null,
                selectorId == null ? BeeUtils.toString(time) : null, BeeConst.STRING_TRUE);

            Queries
                .update(VIEW_USER_REMINDERS, flt, columns, values, new Queries.IntCallback() {
                  @Override
                  public void onSuccess(Integer result) {
                    close();
                  }
                });
          } else {
           showDateError();
          }
        });
    updateReminderButton.addStyleName(STYLE_DIALOG_REMIND_BUTTON);
    return updateReminderButton;
  }

  private TextLabel createTextLabelWidget(String text, String style) {
    TextLabel textLabel = new TextLabel(true);
    textLabel.setText(text);
    textLabel.setStyleName(style);
    return textLabel;
  }

  private void display() {
    focusOnOpen(getContent());
    center();
  }

  private static String formatDate(DateTime dateTime) {
    return BeeUtils.join(" ",
        DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL).format(dateTime),
        DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.TIME_SHORT).format(dateTime));
  }

  private void showDateError() {
    Global.showError(Localized.dictionary().error(), Collections.singletonList(
        Localized.dictionary().userReminderSendRemindDateError()));
  }

}
