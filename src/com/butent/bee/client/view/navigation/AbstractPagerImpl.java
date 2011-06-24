package com.butent.bee.client.view.navigation;

import com.google.common.base.Objects;
import com.google.gwt.user.cellview.client.AbstractPager;
import com.google.gwt.view.client.HasRows;

import com.butent.bee.client.dom.DomUtils;
import com.butent.bee.client.presenter.Presenter;

/**
 * Is an abstract class with default pager implementation behavior.
 */

public abstract class AbstractPagerImpl extends AbstractPager implements PagerView {

  private Presenter viewPresenter = null;
  private boolean enabled = true;

  public Presenter getViewPresenter() {
    return viewPresenter;
  }

  public String getWidgetId() {
    return DomUtils.getId(getWidget());
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setViewPresenter(Presenter viewPresenter) {
    this.viewPresenter = viewPresenter;
  }

  public void start(HasRows display) {
    if (!Objects.equal(display, getDisplay())) {
      setDisplay(display);
    }
  }
}
