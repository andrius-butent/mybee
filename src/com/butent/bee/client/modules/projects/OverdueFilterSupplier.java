package com.butent.bee.client.modules.projects;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import static com.butent.bee.shared.modules.projects.ProjectConstants.*;

import com.butent.bee.client.data.Data;
import com.butent.bee.client.view.search.RangeFilterSupplier;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.data.filter.Filter;
import com.butent.bee.shared.data.filter.FilterValue;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.utils.BeeUtils;

import java.util.List;

class OverdueFilterSupplier extends RangeFilterSupplier {

  OverdueFilterSupplier(String options) {
    super(VIEW_PROJECTS, Data.getColumn(VIEW_PROJECTS, COL_OVERDUE), Localized.dictionary()
        .prjOverduePercent(), options);
  }

  private static Pair<String, String> parseRange(String value) {
    if (BeeUtils.isEmpty(value)) {
      return null;
    }

    String lower = null;
    String upper = null;

    int i = 0;
    for (String s : Splitter.on(BeeConst.CHAR_COMMA).trimResults().split(value)) {
      if (i == 0) {
        lower = s;
      } else if (i == 1) {
        upper = s;
      }

      i++;
    }

    return Pair.of(lower, upper);
  }

  @Override
  public Filter parse(FilterValue input) {
    if (input == null) {
      return null;

    } else if (input.hasValue()) {
      Pair<String, String> r = parseRange(input.getValue());
      List<String> filterArgs = Lists.newArrayList(r.getA(), r.getB());
      return Filter.custom(FILTER_OVERDUE_CREATION, filterArgs);

    } else {
      return null;
    }
  }

}
