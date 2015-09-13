package com.butent.bee.shared.modules.payroll;

import com.butent.bee.shared.i18n.LocalizableConstants;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.ui.HasLocalizedCaption;
import com.butent.bee.shared.utils.EnumUtils;

public final class PayrollConstants {

  public enum ObjectStatus implements HasLocalizedCaption {
    INACTIVE {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.objectStatusInactive();
      }
    },
    ACTIVE {
      @Override
      public String getCaption(LocalizableConstants constants) {
        return constants.objectStatusActive();
      }
    };

    @Override
    public String getCaption() {
      return getCaption(Localized.getConstants());
    }
  }

  public static final String TBL_EMPLOYEES = "Employees";
  public static final String TBL_LOCATIONS = "Locations";

  public static final String VIEW_EMPLOYEES = "Employees";
  public static final String VIEW_LOCATIONS = "Locations";

  public static final String COL_TAB_NUMBER = "TabNumber";

  public static final String COL_LOCATION_NAME = "LocationName";

  public static final String ALS_DEPARTMENT_NAME = "DepartmentName";

  public static final String ALS_LOCATION_MANAGER_FIRST_NAME = "ManagerFirstName";
  public static final String ALS_LOCATION_MANAGER_LAST_NAME = "ManagerLastName";

  public static void register() {
    EnumUtils.register(ObjectStatus.class);
  }

  private PayrollConstants() {
  }
}
