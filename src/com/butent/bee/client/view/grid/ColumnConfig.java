package com.butent.bee.client.view.grid;

import com.google.common.collect.Lists;

import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.data.BeeColumn;
import com.butent.bee.shared.data.BeeRow;
import com.butent.bee.shared.data.DataUtils;
import com.butent.bee.shared.ui.ColumnDescription;
import com.butent.bee.shared.ui.Flexibility;
import com.butent.bee.shared.ui.StyleDeclaration;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class ColumnConfig {

  private static final List<BeeColumn> dataColumns = Lists.newArrayList();

  static int gridIndex;
  static int nameIndex;
  static int captionIndex;

  static int widthIndex;
  static int minWidthIndex;
  static int maxWidthIndex;

  static int autoFitIndex;

  static int flexGrowIndex;
  static int flexShrinkIndex;
  static int flexBasisIndex;
  static int flexUnitIndex;

  static int formatIndex;

  static int headerStyleIndex;
  static int headerFontIndex;

  static int bodyStyleIndex;
  static int bodyFontIndex;

  static int footerStyleIndex;
  static int footerFontIndex;

  static void ensureIndexes(List<BeeColumn> columns) {
    if (dataColumns.isEmpty()) {
      dataColumns.addAll(columns);

      List<String> names = DataUtils.getColumnNames(columns);

      gridIndex = GridUtils.getIndex(names, "GridSetting");
      nameIndex = GridUtils.getIndex(names, "Name");
      captionIndex = GridUtils.getIndex(names, "Caption");

      widthIndex = GridUtils.getIndex(names, "Width");
      minWidthIndex = GridUtils.getIndex(names, "MinWidth");
      maxWidthIndex = GridUtils.getIndex(names, "MaxWidth");

      autoFitIndex = GridUtils.getIndex(names, "AutoFit");

      flexGrowIndex = GridUtils.getIndex(names, "FlexGrow");
      flexShrinkIndex = GridUtils.getIndex(names, "FlexShrink");
      flexBasisIndex = GridUtils.getIndex(names, "FlexBasis");
      flexUnitIndex = GridUtils.getIndex(names, "FlexBasisUnit");

      formatIndex = GridUtils.getIndex(names, "Format");

      headerStyleIndex = GridUtils.getIndex(names, "HeaderStyle");
      headerFontIndex = GridUtils.getIndex(names, "HeaderFont");

      bodyStyleIndex = GridUtils.getIndex(names, "BodyStyle");
      bodyFontIndex = GridUtils.getIndex(names, "BodyFont");

      footerStyleIndex = GridUtils.getIndex(names, "FooterStyle");
      footerFontIndex = GridUtils.getIndex(names, "FooterFont");
    }
  }

  static List<BeeColumn> getDataColumns() {
    return dataColumns;
  }


  final BeeRow row;

  ColumnConfig(BeeRow row) {
    this.row = row;
  }

  void applyTo(ColumnDescription columnDescription) {
    String caption = getCaption();
    if (!BeeUtils.isEmpty(caption)) {
      columnDescription.setCaption(caption);
    }

    Integer width = getWidth();
    if (BeeUtils.isPositive(width)) {
      columnDescription.setWidth(width);
    }

    Integer minWidth = getMinWidth();
    if (BeeUtils.isPositive(minWidth)) {
      columnDescription.setMinWidth(minWidth);
    }

    Integer maxWidth = getMaxWidth();
    if (BeeUtils.isPositive(maxWidth)) {
      columnDescription.setMaxWidth(maxWidth);
    }

    Boolean autoFit = getAutoFit();
    if (BeeUtils.isTrue(autoFit)) {
      columnDescription.setAutoFit(BeeConst.STRING_TRUE);
    }

    Flexibility flexibility = Flexibility.maybeCreate(getFlexGrow(), getFlexShrink(),
        getFlexBasis(), getFlexBasisUnit());
    if (flexibility != null) {
      if (columnDescription.getFlexibility() == null) {
        columnDescription.setFlexibility(flexibility);
      } else {
        columnDescription.getFlexibility().merge(flexibility);
      }
    }

    String format = getFormat();
    if (!BeeUtils.isEmpty(format)) {
      columnDescription.setFormat(format);
    }

    columnDescription.setHeaderStyle(StyleDeclaration.fuse(columnDescription.getHeaderStyle(),
        null, getHeaderStyle(), getHeaderFont()));
    columnDescription.setBodyStyle(StyleDeclaration.fuse(columnDescription.getBodyStyle(),
        null, getBodyStyle(), getBodyFont()));
    columnDescription.setFooterStyle(StyleDeclaration.fuse(columnDescription.getFooterStyle(),
        null, getFooterStyle(), getFooterFont()));
  }

  String getName() {
    return row.getString(nameIndex);
  }

  boolean isEmpty() {
    return getCaption() == null
        && getWidth() == null && getMinWidth() == null && getMaxWidth() == null
        && getAutoFit() == null
        && getFlexGrow() == null && getFlexShrink() == null
        && getFlexBasis() == null && getFlexBasisUnit() == null
        && getFormat() == null
        && getHeaderStyle() == null && getHeaderFont() == null
        && getBodyStyle() == null && getBodyFont() == null
        && getFooterStyle() == null && getFooterFont() == null;
  }

  private Boolean getAutoFit() {
    return row.getBoolean(autoFitIndex);
  }

  private String getBodyFont() {
    return row.getString(bodyFontIndex);
  }

  private String getBodyStyle() {
    return row.getString(bodyStyleIndex);
  }

  private String getCaption() {
    return row.getString(captionIndex);
  }

  private Integer getFlexBasis() {
    return row.getInteger(flexBasisIndex);
  }

  private String getFlexBasisUnit() {
    return row.getString(flexUnitIndex);
  }

  private Integer getFlexGrow() {
    return row.getInteger(flexGrowIndex);
  }

  private Integer getFlexShrink() {
    return row.getInteger(flexShrinkIndex);
  }

  private String getFooterFont() {
    return row.getString(footerFontIndex);
  }

  private String getFooterStyle() {
    return row.getString(footerStyleIndex);
  }

  private String getFormat() {
    return row.getString(formatIndex);
  }

  private String getHeaderFont() {
    return row.getString(headerFontIndex);
  }

  private String getHeaderStyle() {
    return row.getString(headerStyleIndex);
  }

  private Integer getMaxWidth() {
    return row.getInteger(maxWidthIndex);
  }

  private Integer getMinWidth() {
    return row.getInteger(minWidthIndex);
  }

  private Integer getWidth() {
    return row.getInteger(widthIndex);
  }
}
