package com.butent.bee.egg.server.ui;

import com.butent.bee.egg.server.Assert;
import com.butent.bee.egg.server.http.RequestInfo;
import com.butent.bee.egg.server.http.ResponseBuffer;
import com.butent.bee.egg.server.utils.XmlUtils;
import com.butent.bee.egg.shared.data.BeeColumn;
import com.butent.bee.egg.shared.sql.QueryBuilder;
import com.butent.bee.egg.shared.ui.UiComponent;
import com.butent.bee.egg.shared.utils.BeeUtils;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class UiServiceBean {
  private static Logger logger = Logger.getLogger(UiServiceBean.class.getName());

  @EJB
  UiHolderBean holder;
  @EJB
  QueryServiceBean qs;

  public void doService(String svc, RequestInfo reqInfo, ResponseBuffer buff) {
    Assert.notEmpty(svc);
    Assert.notNull(buff);

    if (svc.equals("rpc_ui_form")) {
      formInfo(reqInfo, buff);
    } else if (svc.equals("rpc_ui_form_list")) {
      formList(reqInfo, buff);
    } else if (svc.equals("rpc_ui_menu")) {
      menuInfo(reqInfo, buff);
    } else {
      String msg = BeeUtils.concat(1, svc, "loader service not recognized");
      logger.warning(msg);
      buff.add(msg);
    }
  }

  private void formInfo(RequestInfo reqInfo, ResponseBuffer buff) {
    String fName = getXmlField(reqInfo, buff, "form_name");

    UiComponent form = holder.getForm(fName);

    if (BeeUtils.isEmpty(form)) {
      String msg = "Form name not recognized: " + fName;
      logger.warning(msg);
      buff.add(msg);
    } else {
      buff.add(form.serialize());
    }
  }

  private void formList(RequestInfo reqInfo, ResponseBuffer buff) {
    QueryBuilder qb = new QueryBuilder();
    qb.addFields("f", "form").addFrom("x_forms", "f").addOrder("form");

    List<Object[]> res = qs.getQueryData(qb);
    if (res == null) {
      return;
    }

    for (String alias : qb.getFieldAliases()) {
      buff.addColumn(new BeeColumn(alias));
    }
    for (Object[] row : res) {
      for (Object cell : row) {
        buff.add(cell);
      }
    }
  }

  private String getXmlField(RequestInfo reqInfo, ResponseBuffer buff,
      String fieldName) {
    String xml = reqInfo.getContent();
    if (BeeUtils.isEmpty(xml)) {
      buff.add("Request data not found");
      return null;
    }

    Map<String, String> fields = XmlUtils.getElements(xml);
    if (BeeUtils.isEmpty(fields)) {
      buff.addLine("No elements with text found in", xml);
      return null;
    }
    return fields.get(fieldName);
  }

  private void menuInfo(RequestInfo reqInfo, ResponseBuffer buff) {
    String mName = getXmlField(reqInfo, buff, "menu_name");
    String lRoot = getXmlField(reqInfo, buff, "root_layout");
    String lItem = getXmlField(reqInfo, buff, "item_layout");

    UiComponent menu = holder.getMenu(mName, lRoot, lItem,
        "/com/butent/bee/egg/server/menu.xml");

    if (BeeUtils.isEmpty(menu)) {
      String msg = "Error initializing menu: " + mName;
      logger.warning(msg);
      buff.add(msg);
    } else {
      buff.add(menu.serialize());
    }
  }
}
