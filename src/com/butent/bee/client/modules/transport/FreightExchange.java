package com.butent.bee.client.modules.transport;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasNativeEvent;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.HandlerRegistration;

import static com.butent.bee.shared.modules.transport.TransportConstants.*;

import com.butent.bee.client.BeeKeeper;
import com.butent.bee.client.Callback;
import com.butent.bee.client.Global;
import com.butent.bee.client.communication.ResponseCallback;
import com.butent.bee.client.data.Queries;
import com.butent.bee.client.data.RowEditor;
import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.dom.Rectangle;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.event.logical.MoveEvent;
import com.butent.bee.client.event.logical.VisibilityChangeEvent;
import com.butent.bee.client.layout.Flow;
import com.butent.bee.client.layout.Simple;
import com.butent.bee.client.output.Printable;
import com.butent.bee.client.output.Printer;
import com.butent.bee.client.presenter.Presenter;
import com.butent.bee.client.style.StyleUtils;
import com.butent.bee.client.ui.HasWidgetSupplier;
import com.butent.bee.client.ui.IdentifiableWidget;
import com.butent.bee.client.ui.UiOption;
import com.butent.bee.client.view.HeaderImpl;
import com.butent.bee.client.view.HeaderView;
import com.butent.bee.client.view.View;
import com.butent.bee.client.widget.BeeLabel;
import com.butent.bee.client.widget.CustomDiv;
import com.butent.bee.client.widget.Mover;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.BeeRowSet;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.data.SimpleRowSet.SimpleRow;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.time.DateTime;
import com.butent.bee.shared.time.HasDateRange;
import com.butent.bee.shared.time.JustDate;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.ui.Action;
import com.butent.bee.shared.ui.Orientation;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class FreightExchange extends Flow implements Presenter, View, Printable,
    VisibilityChangeEvent.Handler, HasWidgetSupplier, HasVisibleRange, MoveEvent.Handler {

  private static class Freight implements HasDateRange {
    private final Long orderId;

    private final OrderStatus orderStatus;
    private final DateTime orderDate;
    private final String orderNo;

    private final Long customerId;
    private final String customerName;

    private final Long cargoId;
    private final String cargoDescription;

    private final Long loadingPlaceId;
    private final Long unloadingPlaceId;

    private final JustDate loadingDate;
    private final Long loadingCountry;
    private final String loadingPlace;
    private final String loadingTerminal;

    private final JustDate unloadingDate;
    private final Long unloadingCountry;
    private final String unloadingPlace;
    private final String unloadingTerminal;

    private final Range<JustDate> range;

    private Freight(SimpleRow row) {
      super();

      this.orderId = row.getLong(COL_ORDER);

      this.orderStatus = NameUtils.getEnumByIndex(OrderStatus.class, row.getInt(COL_STATUS));
      this.orderDate = row.getDateTime(COL_ORDER_DATE);
      this.orderNo = row.getValue(COL_ORDER_NO);

      this.customerId = row.getLong(COL_CUSTOMER);
      this.customerName = row.getValue(COL_CUSTOMER_NAME);

      this.cargoId = row.getLong(COL_CARGO_ID);
      this.cargoDescription = row.getValue(COL_DESCRIPTION);

      this.loadingPlaceId = row.getLong(COL_LOADING_PLACE);
      this.unloadingPlaceId = row.getLong(COL_UNLOADING_PLACE);

      this.loadingDate = row.getDate(loadingColumnAlias(COL_PLACE_DATE));
      this.loadingCountry = row.getLong(loadingColumnAlias(COL_COUNTRY));
      this.loadingPlace = row.getValue(loadingColumnAlias(COL_PLACE_NAME));
      this.loadingTerminal = row.getValue(loadingColumnAlias(COL_TERMINAL));

      this.unloadingDate = row.getDate(unloadingColumnAlias(COL_PLACE_DATE));
      this.unloadingCountry = row.getLong(unloadingColumnAlias(COL_COUNTRY));
      this.unloadingPlace = row.getValue(unloadingColumnAlias(COL_PLACE_NAME));
      this.unloadingTerminal = row.getValue(unloadingColumnAlias(COL_TERMINAL));

      JustDate start = BeeUtils.nvl(loadingDate, unloadingDate, orderDate.getDate());
      JustDate end = BeeUtils.nvl(unloadingDate, start);

      this.range = Range.closed(start, TimeUtils.max(start, end));
    }

    @Override
    public Range<JustDate> getRange() {
      return range;
    }

    @Override
    public String toString() {
      return "orderId=" + orderId + ", orderStatus=" + orderStatus + ", orderDate="
          + orderDate + ", orderNo=" + orderNo + ", customerId=" + customerId + ", customerName="
          + customerName + ", cargoId=" + cargoId + ", cargoDescription=" + cargoDescription
          + ", loadingPlaceId=" + loadingPlaceId + ", unloadingPlaceId=" + unloadingPlaceId
          + ", loadingDate=" + loadingDate + ", loadingCountry=" + loadingCountry
          + ", loadingPlace=" + loadingPlace + ", loadingTerminal=" + loadingTerminal
          + ", unloadingDate=" + unloadingDate + ", unloadingCountry=" + unloadingCountry
          + ", unloadingPlace=" + unloadingPlace + ", unloadingTerminal=" + unloadingTerminal;
    }
  }

  private enum RangeMover {
    START_SLIDER, END_SLIDER, BAR
  }

  static final String supplierKey = "freight_exchange";

  private static final BeeLogger logger = LogUtils.getLogger(FreightExchange.class);

  private static final String STYLE_PREFIX = "bee-tr-fx-";

  private static final String STYLE_CONTAINER = STYLE_PREFIX + "Container";
  private static final String STYLE_CANVAS = STYLE_PREFIX + "Canvas";
  private static final String STYLE_HEADER = STYLE_PREFIX + "Header";
  private static final String STYLE_SCROLL_AREA = STYLE_PREFIX + "ScrollArea";
  private static final String STYLE_CONTENT = STYLE_PREFIX + "Content";
  private static final String STYLE_FOOTER = STYLE_PREFIX + "Footer";

  private static final String STYLE_CUSTOMER_PREFIX = STYLE_PREFIX + "Customer-";
  private static final String STYLE_CUSTOMER_COLUMN_SEPARATOR = STYLE_CUSTOMER_PREFIX + "col-sep";
  private static final String STYLE_CUSTOMER_ROW_SEPARATOR = STYLE_CUSTOMER_PREFIX + "row-sep";
  private static final String STYLE_CUSTOMER_PANEL = STYLE_CUSTOMER_PREFIX + "panel";
  private static final String STYLE_CUSTOMER_LABEL = STYLE_CUSTOMER_PREFIX + "label";

  private static final String STYLE_ORDER_PREFIX = STYLE_PREFIX + "Order-";
  private static final String STYLE_ORDER_COLUMN_SEPARATOR = STYLE_ORDER_PREFIX + "col-sep";
  private static final String STYLE_ORDER_ROW_SEPARATOR = STYLE_ORDER_PREFIX + "row-sep";
  private static final String STYLE_ORDER_PANEL = STYLE_ORDER_PREFIX + "panel";
  private static final String STYLE_ORDER_LABEL = STYLE_ORDER_PREFIX + "label";

  private static final String STYLE_ITEM_PREFIX = STYLE_PREFIX + "Item-";
  private static final String STYLE_ITEM_PANEL = STYLE_ITEM_PREFIX + "panel";
  private static final String STYLE_ITEM_LOAD = STYLE_ITEM_PREFIX + "load";
  private static final String STYLE_ITEM_UNLOAD = STYLE_ITEM_PREFIX + "unload";

  private static final String STYLE_CONTENT_ROW_SEPARATOR = STYLE_PREFIX + "row-sep";
  private static final String STYLE_CONTENT_BOTTOM_SEPARATOR = STYLE_PREFIX + "bottom-sep";

  private static final String STYLE_SELECTOR_PREFIX = STYLE_PREFIX + "Selector-";
  private static final String STYLE_SELECTOR_PANEL = STYLE_SELECTOR_PREFIX + "panel";
  private static final String STYLE_SELECTOR_STRIP = STYLE_SELECTOR_PREFIX + "strip";
  private static final String STYLE_SELECTOR_START_SLIDER = STYLE_SELECTOR_PREFIX + "startSlider";
  private static final String STYLE_SELECTOR_END_SLIDER = STYLE_SELECTOR_PREFIX + "endSlider";
  private static final String STYLE_SELECTOR_BAR = STYLE_SELECTOR_PREFIX + "bar";

  static void open(final Callback<IdentifiableWidget> callback) {
    Assert.notNull(callback);

    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(SVC_GET_FX_DATA),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            FreightExchange fx = new FreightExchange();
            if (fx.setData(response)) {
              callback.onSuccess(fx);
            } else {
              callback.onFailure(fx.getCaption(), "negavo duomenų iš serverio",
                  Global.CONSTANTS.sorry());
            }
          }
        });
  }

  private final HeaderView headerView;
  private final Flow canvas;

  private final List<HandlerRegistration> registry = Lists.newArrayList();

  private boolean enabled = true;

  private final List<Freight> items = Lists.newArrayList();
  private final List<String> colors = Lists.newArrayList();

  private BeeRowSet settings = null;
  private BeeRowSet countries = null;

  private Range<JustDate> maxRange = null;
  private Range<JustDate> visibleRange = null;

  private boolean renderPending = false;

  private String startSliderId = null;
  private String endSliderId = null;
  private String selectorBarId = null;
  
  private int sliderWidth = 0;

  private FreightExchange() {
    super();
    addStyleName(STYLE_CONTAINER);

    this.headerView = GWT.create(HeaderImpl.class);
    headerView.create("Užsakymų birža", false, true, EnumSet.of(UiOption.ROOT),
        EnumSet.of(Action.REFRESH, Action.CONFIGURE), null);
    headerView.setViewPresenter(this);
    add(headerView);

    this.canvas = new Flow();
    canvas.addStyleName(STYLE_CANVAS);
    add(canvas);
  }

  @Override
  public String getCaption() {
    return headerView.getCaption();
  }

  @Override
  public String getEventSource() {
    return null;
  }

  @Override
  public HeaderView getHeader() {
    return headerView;
  }

  @Override
  public String getIdPrefix() {
    return "tr-fx";
  }

  @Override
  public View getMainView() {
    return this;
  }

  @Override
  public Range<JustDate> getMaxRange() {
    return maxRange;
  }

  @Override
  public Element getPrintElement() {
    return getElement();
  }

  @Override
  public String getSupplierKey() {
    return supplierKey;
  }

  @Override
  public Presenter getViewPresenter() {
    return this;
  }

  @Override
  public Range<JustDate> getVisibleRange() {
    return visibleRange;
  }

  @Override
  public IdentifiableWidget getWidget() {
    return this;
  }

  @Override
  public String getWidgetId() {
    return getId();
  }

  @Override
  public void handleAction(Action action) {
    switch (action) {
      case REFRESH:
        refresh();
        break;

      case CONFIGURE:
        break;

      case CLOSE:
        BeeKeeper.getScreen().closeWidget(this);
        break;

      case PRINT:
        Printer.print(this);
        break;

      default:
        logger.info(action, "not implemented");
    }
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onMove(MoveEvent event) {
    if (!(event.getSource() instanceof Mover)) {
      return;
    }

    int delta = event.getDelta();

    Element source = ((Mover) event.getSource()).getElement();
    Element panel = source.getParentElement();

    EnumMap<RangeMover, Element> movers = Maps.newEnumMap(RangeMover.class);

    RangeMover sourceType = getMoverType(source);
    if (sourceType == null) {
      return;
    }
    movers.put(sourceType, source);

    for (Element elem = source.getPreviousSiblingElement(); elem != null; elem =
        elem.getPreviousSiblingElement()) {
      RangeMover type = getMoverType(elem);
      if (type == null) {
        break;
      }
      movers.put(type, elem);
    }

    for (Element elem = source.getNextSiblingElement(); elem != null; elem =
        elem.getNextSiblingElement()) {
      RangeMover type = getMoverType(elem);
      if (type == null) {
        break;
      }
      movers.put(type, elem);
    }

    if (movers.size() != RangeMover.values().length) {
      logger.warning("found movers:", movers.keySet());
      return;
    }

    int panelWidth = StyleUtils.getWidth(panel) - getSliderWidth();

    int startPos = StyleUtils.getLeft(movers.get(RangeMover.START_SLIDER));
    int endPos = StyleUtils.getLeft(movers.get(RangeMover.END_SLIDER));

    boolean changed = false;

    if (delta != 0) {
      switch (sourceType) {
        case START_SLIDER:
          int newStart = BeeUtils.clamp(startPos + delta, 0, endPos - getSliderWidth());
          if (newStart != startPos) {
            startPos = newStart;

            StyleUtils.setLeft(movers.get(RangeMover.START_SLIDER), startPos);
            StyleUtils.setLeft(movers.get(RangeMover.BAR), startPos);
            StyleUtils.setWidth(movers.get(RangeMover.BAR), endPos - startPos + getSliderWidth());

            changed = true;
          }
          break;

        case END_SLIDER:
          int newEnd = BeeUtils.clamp(endPos + delta, startPos + getSliderWidth(), panelWidth);
          if (newEnd != endPos) {
            endPos = newEnd;

            StyleUtils.setLeft(movers.get(RangeMover.END_SLIDER), endPos);
            StyleUtils.setWidth(movers.get(RangeMover.BAR), endPos - startPos + getSliderWidth());

            changed = true;
          }
          break;

        case BAR:
          int dx = BeeUtils.clamp(delta, -startPos, panelWidth - endPos);
          if (dx != 0) {
            startPos += dx;
            endPos += dx;

            StyleUtils.setLeft(movers.get(RangeMover.START_SLIDER), startPos);
            StyleUtils.setLeft(movers.get(RangeMover.END_SLIDER), endPos);
            StyleUtils.setLeft(movers.get(RangeMover.BAR), startPos);

            changed = true;
          }
          break;
      }
    }

    if (changed || event.isFinished()) {
      JustDate min = getMaxRange().lowerEndpoint();
      JustDate max = getMaxRange().upperEndpoint();

      double dayWidth = (double) panelWidth / ChartHelper.getSize(getMaxRange());
      JustDate start = TimeUtils.clamp(ChartHelper.getDate(min, startPos, dayWidth), min, max);
      JustDate end = TimeUtils.clamp(ChartHelper.getDate(min, endPos, dayWidth), start, max);

      if (event.isFinished()) {
        setVisibleRange(start, end);

      } else {
        movers.get(RangeMover.START_SLIDER).setTitle(start.toString());
        movers.get(RangeMover.END_SLIDER).setTitle(end.toString());
        movers.get(RangeMover.BAR).setTitle(ChartHelper.getRangeLabel(start, end));
      }
    }
  }

  @Override
  public boolean onPrint(Element source, Element target) {
    boolean ok;
    String id = source.getId();

    if (getId().equals(id)) {
      int height = source.getClientHeight() + getPrintHeightAdjustment();
      StyleUtils.setSize(target, source.getClientWidth(), height);
      ok = true;

    } else if (headerView.asWidget().getElement().isOrHasChild(source)) {
      ok = headerView.onPrint(source, target);

    } else {
      ok = true;
    }

    return ok;
  }

  @Override
  public void onResize() {
    render(false);
  }

  @Override
  public void onViewUnload() {
  }

  @Override
  public void onVisibilityChange(VisibilityChangeEvent event) {
    if (event.isVisible() && DomUtils.isOrHasAncestor(getElement(), event.getId())) {
      if (isRenderPending()) {
        render(false);
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void setEventSource(String eventSource) {
  }

  @Override
  public void setViewPresenter(Presenter viewPresenter) {
  }

  @Override
  public void setVisibleEnd(JustDate end) {
    if (getVisibleRange() != null && end != null
        && !getVisibleRange().upperEndpoint().equals(end)) {

      JustDate start = getVisibleRange().lowerEndpoint();

      if (TimeUtils.isMeq(end, start)) {
        setVisibleRange(Range.closed(start, end));
        render(false);
      }
    }
  }

  @Override
  public void setVisibleRange(JustDate start, JustDate end) {
    if (start != null && end != null && TimeUtils.isLeq(start, end)) {

      Range<JustDate> range = Range.closed(start, end);

      if (!range.equals(getVisibleRange())) {
        setVisibleRange(range);
        render(false);
      }
    }
  }

  @Override
  public void setVisibleStart(JustDate start) {
    if (getVisibleRange() != null && start != null
        && !getVisibleRange().lowerEndpoint().equals(start)) {

      JustDate end = getVisibleRange().upperEndpoint();

      if (TimeUtils.isLeq(start, end)) {
        setVisibleRange(Range.closed(start, end));
        render(false);
      }
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    EventUtils.clearRegistry(registry);
    registry.add(VisibilityChangeEvent.register(this));

    render(true);
  }

  @Override
  protected void onUnload() {
    EventUtils.clearRegistry(registry);

    super.onUnload();
  }

  private void addItemWidgets(Freight item, HasWidgets panel, Rectangle rectangle) {
    CustomDiv itemWidget = new CustomDiv(STYLE_ITEM_PANEL);
    setItemWidgetColor(item, itemWidget);

    rectangle.applyTo(itemWidget);

    String loading = getPlaceLabel(item.loadingCountry, item.loadingPlace, item.loadingTerminal);
    String unloading = getPlaceLabel(item.unloadingCountry, item.unloadingPlace,
        item.unloadingTerminal);

    String title = BeeUtils.buildLines(item.cargoDescription,
        BeeUtils.joinWords("Pakrovimas:", item.loadingDate, loading),
        BeeUtils.joinWords("Iškrovimas:", item.unloadingDate, unloading));

    final Long cargoId = item.cargoId;
    ClickHandler opener = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowEditor.opendRow(VIEW_CARGO, cargoId, openModal(event), null);
      }
    };

    panel.add(itemWidget);

    if (BeeUtils.allEmpty(loading, unloading)) {
      itemWidget.setTitle(title);
      itemWidget.addClickHandler(opener);
    }
    
    if (!BeeUtils.isEmpty(loading)) {
      BeeLabel loadingLabel = new BeeLabel(loading);
      loadingLabel.addStyleName(STYLE_ITEM_LOAD);

      if (BeeUtils.isEmpty(unloading)) {
        rectangle.applyTo(loadingLabel);
      
        loadingLabel.setTitle(title);
        loadingLabel.addClickHandler(opener);

      } else {
        rectangle.applyLeft(loadingLabel.getElement().getStyle());
        rectangle.applyTop(loadingLabel.getElement().getStyle());
      }

      panel.add(loadingLabel);
    }

    if (!BeeUtils.isEmpty(unloading)) {
      BeeLabel unloadingLabel = new BeeLabel(unloading);
      unloadingLabel.addStyleName(STYLE_ITEM_UNLOAD);

      rectangle.applyTo(unloadingLabel);

      unloadingLabel.setTitle(title);
      unloadingLabel.addClickHandler(opener);

      panel.add(unloadingLabel);
    }
  }

  private Widget createCustomerWidget(Freight item) {
    BeeLabel widget = new BeeLabel(item.customerName);
    widget.addStyleName(STYLE_CUSTOMER_LABEL);

    final Long customerId = item.customerId;

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowEditor.opendRow(CommonsConstants.VIEW_COMPANIES, customerId, openModal(event), null);
      }
    });

    Simple panel = new Simple(widget);
    panel.addStyleName(STYLE_CUSTOMER_PANEL);

    return panel;
  }

  private Widget createOrderWidget(Freight item) {
    BeeLabel widget = new BeeLabel(item.orderNo);
    widget.addStyleName(STYLE_ORDER_LABEL);

    widget.setTitle(BeeUtils.buildLines(
        BeeUtils.joinWords("Data:", TimeUtils.renderCompact(item.orderDate)),
        BeeUtils.joinWords("Būsena:", item.orderStatus.getCaption()),
        BeeUtils.joinWords("ID:", item.orderId)));

    final Long orderId = item.orderId;

    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        RowEditor.opendRow(VIEW_ORDERS, orderId, openModal(event), null);
      }
    });

    Simple panel = new Simple(widget);
    panel.addStyleName(STYLE_ORDER_PANEL);

    return panel;
  }

  private List<List<Freight>> doLayout() {
    List<List<Freight>> rows = Lists.newArrayList();

    Long orderId = null;
    List<Freight> rowItems = Lists.newArrayList();

    for (Freight item : items) {
      if (BeeUtils.intersects(getVisibleRange(), item.getRange())) {

        if (!Objects.equal(item.orderId, orderId) ||
            ChartHelper.intersects(rowItems, item.getRange())) {

          if (!rowItems.isEmpty()) {
            rows.add(Lists.newArrayList(rowItems));
            rowItems.clear();
          }

          orderId = item.orderId;
        }

        rowItems.add(item);
      }
    }

    if (!rowItems.isEmpty()) {
      rows.add(Lists.newArrayList(rowItems));
    }
    return rows;
  }

  private String getColor(Long id) {
    int index = ChartHelper.getColorIndex(id, colors.size());
    if (BeeUtils.isIndex(colors, index)) {
      return colors.get(index);
    } else {
      return "red";
    }
  }

  private BeeRowSet getCountries() {
    return countries;
  }

  private String getCountryLabel(Long countryId) {
    if (countryId == null || DataUtils.isEmpty(getCountries())) {
      return null;
    }

    BeeRow row = getCountries().getRowById(countryId);
    if (row == null) {
      return null;
    }

    String label = row.getString(getCountries().getColumnIndex(CommonsConstants.COL_CODE));

    if (BeeUtils.isEmpty(label)) {
      return BeeUtils.trim(row.getString(getCountries().getColumnIndex(CommonsConstants.COL_NAME)));
    } else {
      return BeeUtils.trim(label).toUpperCase();
    }
  }

  private String getEndSliderId() {
    return endSliderId;
  }

  private RangeMover getMoverType(Element element) {
    if (element == null) {
      return null;
    }

    String id = element.getId();
    if (BeeUtils.isEmpty(id)) {
      return null;
    }

    if (id.equals(getStartSliderId())) {
      return RangeMover.START_SLIDER;
    } else if (id.equals(getEndSliderId())) {
      return RangeMover.END_SLIDER;
    } else if (id.equals(getSelectorBarId())) {
      return RangeMover.BAR;
    } else {
      return null;
    }
  }

  private String getPlaceLabel(Long countryId, String placeName, String terminal) {
    String countryLabel = getCountryLabel(countryId);

    if (BeeUtils.isEmpty(countryLabel) || BeeUtils.containsSame(placeName, countryLabel)
        || BeeUtils.containsSame(terminal, countryLabel)) {
      return BeeUtils.joinNoDuplicates(BeeConst.STRING_SPACE, placeName, terminal);
    } else {
      return BeeUtils.joinNoDuplicates(BeeConst.STRING_SPACE, countryLabel, placeName, terminal);
    }
  }

  private int getPrintHeightAdjustment() {
    return 0;
  }

  private String getSelectorBarId() {
    return selectorBarId;
  }

  private BeeRowSet getSettings() {
    return settings;
  }

  private int getSliderWidth() {
    return sliderWidth;
  }

  private String getStartSliderId() {
    return startSliderId;
  }

  private boolean isRenderPending() {
    return renderPending;
  }

  private boolean openModal(HasNativeEvent event) {
    return !EventUtils.hasModifierKey(event.getNativeEvent());
  }

  private void refresh() {
    BeeKeeper.getRpc().makePostRequest(TransportHandler.createArgs(SVC_GET_FX_DATA),
        new ResponseCallback() {
          @Override
          public void onResponse(ResponseObject response) {
            if (setData(response)) {
              render(false);
            }
          }
        });
  }

  private void render(boolean updateRange) {
    canvas.clear();
    if (items.isEmpty() || getMaxRange() == null) {
      return;
    }

    int width = canvas.getElement().getClientWidth();
    int height = canvas.getElement().getClientHeight();
    if (width < 30 || height < 30) {
      setRenderPending(true);
      return;
    }

    int customerWidth = ChartHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_CUSTOMER, 100,
        width / 5);
    int orderWidth = ChartHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_ORDER, 60, width / 5);

    int headerHeight = ChartHelper.getPixels(getSettings(), COL_FX_HEADER_HEIGHT, 20, height / 5);
    int footerHeight = ChartHelper.getPixels(getSettings(), COL_FX_FOOTER_HEIGHT, 30, height / 3);

    int chartLeft = customerWidth + orderWidth;
    int chartRight = DomUtils.getScrollBarWidth() + 1;

    int chartWidth = width - chartLeft - chartRight;

    int scrollAreaHeight = height - headerHeight - footerHeight;

    int dayWidth = ChartHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_DAY, 20);
    int rowHeight = ChartHelper.getPixels(getSettings(), COL_FX_PIXELS_PER_ROW, 20,
        scrollAreaHeight / 2);

    int slider = ChartHelper.getPixels(getSettings(), COL_FX_SLIDER_WIDTH, 5);
    setSliderWidth(BeeUtils.clamp(slider, 1, chartRight));
    
    if (updateRange || getVisibleRange() == null || !getMaxRange().encloses(getVisibleRange())) {
      setVisibleRange(ChartHelper.getDefaultRange(getMaxRange(), chartWidth, dayWidth));
    }

    int chartColumnCount = ChartHelper.getSize(getVisibleRange());
    if (chartColumnCount > 1) {
      dayWidth = chartWidth / chartColumnCount;
      chartWidth = dayWidth * chartColumnCount;
    }

    Flow header = new Flow();
    header.addStyleName(STYLE_HEADER);
    StyleUtils.setHeight(header, headerHeight);

    ChartHelper.renderVisibleRange(this, header, chartLeft);
    ChartHelper.renderDayLabels(header, getVisibleRange(), chartLeft, dayWidth);

    canvas.add(header);

    Flow content = new Flow();
    content.addStyleName(STYLE_CONTENT);

    renderContent(content, customerWidth, orderWidth, dayWidth, chartLeft, chartWidth, rowHeight);

    Simple scroll = new Simple();
    scroll.addStyleName(STYLE_SCROLL_AREA);
    StyleUtils.setTop(scroll, headerHeight);
    StyleUtils.setBottom(scroll, footerHeight);

    scroll.setWidget(content);

    canvas.add(scroll);

    Flow footer = new Flow();
    footer.addStyleName(STYLE_FOOTER);
    StyleUtils.setHeight(footer, footerHeight);

    ChartHelper.renderMaxRange(getMaxRange(), footer, chartLeft);

    Flow selector = new Flow();
    selector.addStyleName(STYLE_SELECTOR_PANEL);
    StyleUtils.setLeft(selector, chartLeft);
    StyleUtils.setWidth(selector, chartWidth + getSliderWidth());

    renderRangeSelector(selector, chartWidth, footerHeight);

    footer.add(selector);

    canvas.add(footer);

    setRenderPending(false);
  }

  private void renderContent(ComplexPanel panel, int customerWidth, int orderWidth, int dayWidth,
      int chartLeft, int chartWidth, int rowHeight) {

    List<List<Freight>> layoutRows = doLayout();
    if (layoutRows.isEmpty()) {
      return;
    }

    int height = layoutRows.size() * rowHeight;
    StyleUtils.setHeight(panel, height);

    ChartHelper.addColumnSeparator(panel, STYLE_CUSTOMER_COLUMN_SEPARATOR, customerWidth, height);
    ChartHelper.addColumnSeparator(panel, STYLE_ORDER_COLUMN_SEPARATOR, chartLeft, height);

    ChartHelper.renderDayColumns(panel, getVisibleRange(), chartLeft, dayWidth, height,
        false, true);

    JustDate firstDate = getVisibleRange().lowerEndpoint();
    JustDate lastDate = getVisibleRange().upperEndpoint();

    Long lastCustomer = null;
    Long lastOrder = null;

    Widget customerWidget = null;
    Widget orderWidget = null;

    int customerStartRow = 0;
    int orderStartRow = 0;

    for (int row = 0; row < layoutRows.size(); row++) {
      List<Freight> rowItems = layoutRows.get(row);
      int top = row * rowHeight;

      Freight rowItem = rowItems.get(0);

      if (row == 0) {
        customerWidget = createCustomerWidget(rowItem);
        customerStartRow = row;

        orderWidget = createOrderWidget(rowItem);
        orderStartRow = row;

        lastCustomer = rowItem.customerId;
        lastOrder = rowItem.orderId;

      } else {
        boolean customerChanged = !Objects.equal(lastCustomer, rowItem.customerId);
        boolean orderChanged = customerChanged || !Objects.equal(lastOrder, rowItem.orderId);

        if (customerChanged) {
          ChartHelper.addLegendWidget(panel, customerWidget, 0, customerWidth,
              customerStartRow, row - 1, rowHeight);

          customerWidget = createCustomerWidget(rowItem);
          customerStartRow = row;

          lastCustomer = rowItem.customerId;
        }

        if (orderChanged) {
          ChartHelper.addLegendWidget(panel, orderWidget, customerWidth, orderWidth,
              orderStartRow, row - 1, rowHeight);

          orderWidget = createOrderWidget(rowItem);
          orderStartRow = row;

          lastOrder = rowItem.orderId;
        }

        if (customerChanged) {
          ChartHelper.addRowSeparator(panel, STYLE_CUSTOMER_ROW_SEPARATOR, top, 0,
              customerWidth + orderWidth + chartWidth);
        } else if (orderChanged) {
          ChartHelper.addRowSeparator(panel, STYLE_ORDER_ROW_SEPARATOR, top, customerWidth,
              orderWidth + chartWidth);
        } else {
          ChartHelper.addRowSeparator(panel, STYLE_CONTENT_ROW_SEPARATOR, top, chartLeft,
              chartWidth);
        }
      }

      for (Freight item : rowItems) {
        JustDate start = TimeUtils.clamp(item.getRange().lowerEndpoint(), firstDate, lastDate);
        JustDate end = TimeUtils.clamp(item.getRange().upperEndpoint(), firstDate, lastDate);

        int left = chartLeft + TimeUtils.dayDiff(firstDate, start) * dayWidth;
        int width = (TimeUtils.dayDiff(start, end) + 1) * dayWidth;

        Rectangle rectangle = new Rectangle(left + 1, top + 1, width, rowHeight);
        addItemWidgets(item, panel, rectangle);
      }
    }

    int lastRow = layoutRows.size() - 1;

    if (customerWidget != null) {
      ChartHelper.addLegendWidget(panel, customerWidget, 0, customerWidth,
          customerStartRow, lastRow, rowHeight);
    }
    if (orderWidget != null) {
      ChartHelper.addLegendWidget(panel, orderWidget, customerWidth, orderWidth,
          orderStartRow, lastRow, rowHeight);
    }

    ChartHelper.addRowSeparator(panel, STYLE_CONTENT_BOTTOM_SEPARATOR, height,
        0, customerWidth + orderWidth + chartWidth + 1);
  }

  private void renderRangeSelector(HasWidgets panel, int width, int height) {
    JustDate firstDate = getMaxRange().lowerEndpoint();
    JustDate lastDate = getMaxRange().upperEndpoint();

    double dayWidth = (double) width / ChartHelper.getSize(getMaxRange());
    int itemHeight = BeeUtils.clamp(height / items.size(), Math.max(height / 10, 3), height);
    int rowCount = height / itemHeight;

    Set<Range<JustDate>> ranges = Sets.newHashSet();
    int row = 0;

    for (Freight item : items) {
      Widget itemStrip = new CustomDiv(STYLE_SELECTOR_STRIP);
      setItemWidgetColor(item, itemStrip);

      if (BeeUtils.intersects(ranges, item.getRange()) && row < rowCount - 1) {
        row++;
      } else {
        row = 0;
      }
      ranges.add(item.getRange());

      StyleUtils.setTop(itemStrip, row * itemHeight);
      StyleUtils.setHeight(itemStrip, itemHeight);

      JustDate start = TimeUtils.clamp(item.getRange().lowerEndpoint(), firstDate, lastDate);
      JustDate end = TimeUtils.clamp(item.getRange().upperEndpoint(), firstDate, lastDate);

      int left = BeeUtils.round(TimeUtils.dayDiff(firstDate, start) * dayWidth);
      StyleUtils.setLeft(itemStrip, BeeUtils.clamp(left, 0, width - 1));

      int w = BeeUtils.round((TimeUtils.dayDiff(start, end) + 1) * dayWidth);
      StyleUtils.setWidth(itemStrip, BeeUtils.clamp(w, 1, width - left));

      panel.add(itemStrip);
    }

    JustDate firstVisible = getVisibleRange().lowerEndpoint();
    JustDate lastVisible = getVisibleRange().upperEndpoint();

    int startPos = ChartHelper.getPosition(firstDate, firstVisible, dayWidth);
    startPos = BeeUtils.clamp(startPos, 0, width - getSliderWidth());

    int endPos = ChartHelper.getPosition(firstDate, TimeUtils.nextDay(lastVisible), dayWidth);
    endPos = BeeUtils.clamp(endPos, startPos + getSliderWidth(), width);

    Mover startSlider = new Mover(Orientation.HORIZONTAL, STYLE_SELECTOR_START_SLIDER);
    StyleUtils.setLeft(startSlider, startPos);
    StyleUtils.setWidth(startSlider, getSliderWidth());
    startSlider.setTitle(firstVisible.toString());

    startSlider.addMoveHandler(this);

    setStartSliderId(startSlider.getId());
    panel.add(startSlider);

    Mover endSlider = new Mover(Orientation.HORIZONTAL, STYLE_SELECTOR_END_SLIDER);
    StyleUtils.setLeft(endSlider, endPos);
    StyleUtils.setWidth(endSlider, getSliderWidth());
    endSlider.setTitle(lastVisible.toString());

    endSlider.addMoveHandler(this);

    setEndSliderId(endSlider.getId());
    panel.add(endSlider);

    Mover bar = new Mover(Orientation.HORIZONTAL, STYLE_SELECTOR_BAR);
    StyleUtils.setLeft(bar, startPos);
    StyleUtils.setWidth(bar, endPos - startPos + getSliderWidth());
    bar.setTitle(ChartHelper.getRangeLabel(firstDate, lastVisible));

    bar.addMoveHandler(this);

    setSelectorBarId(bar.getId());
    panel.add(bar);
  }

  private void setCountries(BeeRowSet countries) {
    this.countries = countries;
  }

  private boolean setData(ResponseObject response) {
    if (!Queries.checkResponse(SVC_GET_FX_DATA, null, response, BeeRowSet.class, null)) {
      return false;
    }

    BeeRowSet rowSet = BeeRowSet.restore((String) response.getResponse());
    setSettings(rowSet);

    String serialized = rowSet.getTableProperty(PROP_COUNTRIES);
    if (!BeeUtils.isEmpty(serialized)) {
      setCountries(BeeRowSet.restore(serialized));
    }

    serialized = rowSet.getTableProperty(PROP_COLORS);
    if (!BeeUtils.isEmpty(serialized)) {
      String[] arr = Codec.beeDeserializeCollection(serialized);
      if (arr != null) {
        colors.clear();
        colors.addAll(Arrays.asList(arr));
      }
    }

    serialized = rowSet.getTableProperty(PROP_DATA);
    if (!BeeUtils.isEmpty(serialized)) {
      SimpleRowSet data = SimpleRowSet.restore(serialized);
      items.clear();

      for (SimpleRow row : data) {
        items.add(new Freight(row));
      }

      logger.debug(NameUtils.getName(this), "loaded", items.size(), "items");
      if (!items.isEmpty()) {
        setMaxRange(ChartHelper.getSpan(items));
      }
    }

    return true;
  }

  private void setEndSliderId(String endSliderId) {
    this.endSliderId = endSliderId;
  }

  private void setItemWidgetColor(Freight item, Widget widget) {
    widget.getElement().getStyle().setBackgroundColor(getColor(item.cargoId));
  }

  private void setMaxRange(Range<JustDate> maxRange) {
    this.maxRange = maxRange;
  }

  private void setRenderPending(boolean renderPending) {
    this.renderPending = renderPending;
  }

  private void setSelectorBarId(String selectorBarId) {
    this.selectorBarId = selectorBarId;
  }

  private void setSettings(BeeRowSet settings) {
    this.settings = settings;
  }

  private void setSliderWidth(int sliderWidth) {
    this.sliderWidth = sliderWidth;
  }

  private void setStartSliderId(String startSliderId) {
    this.startSliderId = startSliderId;
  }

  private void setVisibleRange(Range<JustDate> visibleRange) {
    this.visibleRange = visibleRange;
  }
}
