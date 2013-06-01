package com.butent.bee.client.modules.commons;

import com.google.common.base.Objects;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;

import com.butent.bee.client.Global;
import com.butent.bee.client.dialog.InputCallback;
import com.butent.bee.client.dialog.Popup;
import com.butent.bee.client.event.EventUtils;
import com.butent.bee.client.grid.HtmlTable;
import com.butent.bee.client.ui.UiHelper;
import com.butent.bee.client.view.form.FormView;
import com.butent.bee.client.widget.InputPassword;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.data.IsRow;
import com.butent.bee.shared.i18n.Localized;
import com.butent.bee.shared.modules.commons.CommonsConstants;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;

class PasswordService {
  
  private static final String STYLE_DIALOG = "bee-ChangePassword";
  private static final int MAX_PASSWORD_LENGTH = 30;

  static void changePassword(final FormView userForm) {
    Assert.notNull(userForm);
    
    IsRow userRow = userForm.getActiveRow();
    Assert.notNull(userRow);
    
    int passwordIndex = userForm.getDataIndex(CommonsConstants.COL_PASSWORD);
    Assert.nonNegative(passwordIndex);

    final String oldPass = userRow.getString(passwordIndex);

    HtmlTable table = new HtmlTable();
    int row = 0;

    final InputPassword inpOld;
    if (BeeUtils.isEmpty(oldPass)) {
      inpOld = null;
    } else {
      inpOld = new InputPassword(MAX_PASSWORD_LENGTH);
      table.setText(row, 0, Localized.constants.oldPassword());
      table.setWidget(row, 1, inpOld);
      row++;
    }

    final InputPassword inpNew = new InputPassword(MAX_PASSWORD_LENGTH);
    table.setText(row, 0, Localized.constants.newPassword());
    table.setWidget(row, 1, inpNew);
    row++;

    final InputPassword inpNew2 = new InputPassword(MAX_PASSWORD_LENGTH);
    table.setText(row, 0, Localized.constants.repeatNewPassword());
    table.setWidget(row, 1, inpNew2);
    row++;
    
    if (inpOld != null) {
      inpOld.addKeyDownHandler(new KeyDownHandler() {
        @Override
        public void onKeyDown(KeyDownEvent event) {
          if (isEnter(event)) {
            inpNew.setFocus(true);
          }
        }
      });
    }

    inpNew.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (isEnter(event)) {
          inpNew2.setFocus(true);
        }
      }
    });
    
    inpNew2.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (isEnter(event)) {
          Popup popup = UiHelper.getParentPopup(inpNew2);
          if (popup != null && popup.getOnSave() != null) {
            popup.getOnSave().accept(null);
          }
        }
      }
    });
    
    Global.inputWidget(Localized.constants.changePassword(), table, new InputCallback() {
      @Override
      public String getErrorMessage() {
        if (!BeeUtils.isEmpty(oldPass) && inpOld != null) {
          String old = BeeUtils.trim(inpOld.getValue());

          if (BeeUtils.isEmpty(old)) {
            inpOld.setFocus(true);
            return Localized.constants.oldPasswordIsRequired();

          } else if (!Objects.equal(Codec.md5(old), oldPass)) {
            inpOld.setFocus(true);
            return Localized.constants.oldPasswordIsInvalid();
          }
        }

        String newPass = BeeUtils.trim(inpNew.getValue());

        if (BeeUtils.isEmpty(newPass)) {
          inpNew.setFocus(true);
          return Localized.constants.newPasswordIsRequired();

        } else if (!newPass.equals(BeeUtils.trim(inpNew2.getValue()))) {
          inpNew.setFocus(true);
          return Localized.constants.newPasswordsDoesNotMatch();
        }

        return super.getErrorMessage();
      }

      @Override
      public void onSuccess() {
        userForm.updateCell(CommonsConstants.COL_PASSWORD,
            Codec.md5(BeeUtils.trim(inpNew.getValue())));
      }
    }, STYLE_DIALOG);
  }
  
  private static boolean isEnter(KeyDownEvent event) {
    return event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
        && !EventUtils.hasModifierKey(event.getNativeEvent());
  }
}
