package com.butent.bee.client.grid.cell;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.CellContext;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.HasOptions;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.data.HasViewName;
import com.butent.bee.shared.data.event.RowActionEvent;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

public class ActionCell extends AbstractCell<String> implements HasOptions, HasViewName {

  public enum Type {
    LINK, BUTTON;

    private SafeHtml render(String value) {
      switch (this) {
        case LINK:
          return TEMPLATE.link(value);
        case BUTTON:
          return TEMPLATE.button(value);
        default:
          Assert.untouchable();
          return null;
      }
    }
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<button class=\"bee-ActionCellButton\">{0}</button>")
    SafeHtml button(String option);

    @Template("<div class=\"bee-ActionCellLink\">{0}</div>")
    SafeHtml link(String option);
  }

  private static final Template TEMPLATE = GWT.create(Template.class);

  private static final Type DEFAULT_TYPE = Type.LINK;

  public static ActionCell create(String viewName, ColumnDescription columnDescription) {
    ActionCell cell = new ActionCell(NameUtils.getEnumByName(Type.class,
        columnDescription.getElementType()));

    cell.setViewName(viewName);
    cell.setOptions(columnDescription.getOptions());

    return cell;
  }

  private final Type type;

  private String viewName;

  private String options;

  public ActionCell() {
    this(DEFAULT_TYPE);
  }

  public ActionCell(Type type) {
    super(EventUtils.EVENT_TYPE_CLICK);
    this.type = (type == null) ? DEFAULT_TYPE : type;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public String getViewName() {
    return viewName;
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, String value, NativeEvent event,
      ValueUpdater<String> valueUpdater) {
    if (EventUtils.isClick(event) && context instanceof CellContext) {
      BeeKeeper.getBus().fireEvent(new RowActionEvent(getViewName(),
          ((CellContext) context).getRowValue(), Service.CELL_ACTION, getOptions()));
    }
    super.onBrowserEvent(context, parent, value, event, valueUpdater);
  }

  @Override
  public void render(Context context, String value, SafeHtmlBuilder sb) {
    if (!BeeUtils.isEmpty(value)) {
      sb.append(type.render(value));
    }
  }

  @Override
  public void setOptions(String options) {
    this.options = options;
  }

  public void setViewName(String viewName) {
    this.viewName = viewName;
  }
}
