package com.butent.bee.shared.communication;

import com.google.common.collect.Lists;

import com.butent.bee.shared.Assert;
import com.butent.bee.shared.BeeConst;
import com.butent.bee.shared.Service;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.Codec;
import com.butent.bee.shared.utils.NameUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Contains utility functions for communication between server and client sides, for example
 * determining type of message type or encoding.
 */

public final class CommUtils {

  public static final char DEFAULT_INFORMATION_SEPARATOR = '\u001d';

  public static final String QUERY_STRING_SEPARATOR = "?";
  public static final String QUERY_STRING_PAIR_SEPARATOR = "&";
  public static final String QUERY_STRING_VALUE_SEPARATOR = "=";

  public static final String OPTION_DEBUG = "debug";

  public static final String CONTENT_TYPE_HEADER = "content-type";
  public static final String CONTENT_LENGTH_HEADER = "content-length";

  public static final String PATH_SEGMENT_SEPARATOR = "/";
  
  public static final ContentType DEFAULT_REQUEST_CONTENT_TYPE = ContentType.XML;
  public static final ContentType DEFAULT_RESPONSE_CONTENT_TYPE = ContentType.TEXT;

  public static final ContentType FORM_RESPONSE_CONTENT_TYPE = ContentType.HTML;

  public static String buildContentType(String type) {
    return buildContentType(type, getCharacterEncoding(getContentType(type)));
  }

  public static String buildContentType(String type, String encoding) {
    Assert.notEmpty(type);
    if (BeeUtils.isEmpty(encoding)) {
      return type;
    } else {
      return type.trim() + ";charset=" + encoding.trim();
    }
  }
  
  public static String buildPath(String first, String second, String... rest) {
    List<String> segments = Lists.newArrayList(first, second);
    if (rest != null) {
      segments.addAll(Arrays.asList(rest));
    }
    return BeeUtils.join(PATH_SEGMENT_SEPARATOR, segments);
  }

  public static boolean equals(ContentType z1, ContentType z2) {
    if (z1 == null || z2 == null) {
      return false;
    } else {
      return z1 == z2;
    }
  }

  public static String getCharacterEncoding(ContentType ctp) {
    if (ctp == null) {
      return null;
    } else {
      return BeeConst.CHARSET_UTF8;
    }
  }

  public static String getContent(ContentType type, String data) {
    if (isHtml(type) && BeeUtils.length(data) > 0) {
      return Codec.decodeBase64(data);
    } else {
      return data;
    }
  }

  public static ContentType getContentType(String s) {
    return NameUtils.getEnumByName(ContentType.class, s);
  }

  public static ResponseObject getFormResonse(String result) {
    if (BeeUtils.isEmpty(result)) {
      return null;
    }
    return ResponseObject.restore(getContent(FORM_RESPONSE_CONTENT_TYPE, result));
  }

  public static String getMediaType(ContentType ctp) {
    String mt;

    switch (ctp) {
      case TEXT:
        mt = "text/plain";
        break;
      case XML:
        mt = "text/xml";
        break;
      case HTML:
        mt = "text/html";
        break;
      case ZIP:
        mt = "application/zip";
        break;
      default:
        mt = "application/octet-stream";
    }
    return mt;
  }

  public static boolean isHtml(ContentType ctp) {
    return ContentType.HTML.equals(ctp);
  }

  public static boolean isReservedParameter(String name) {
    Assert.notEmpty(name);
    return BeeUtils.startsSame(name, Service.RPC_VAR_SYS_PREFIX);
  }

  public static boolean isResource(ContentType ctp) {
    return ContentType.RESOURCE.equals(ctp);
  }

  public static boolean isText(ContentType ctp) {
    return ContentType.TEXT.equals(ctp);
  }

  public static boolean isValidParameter(String name) {
    Assert.notEmpty(name);
    return NameUtils.isIdentifier(name) && !isReservedParameter(name);
  }

  public static ContentType normalizeRequest(ContentType ctp) {
    return (ctp == null) ? DEFAULT_REQUEST_CONTENT_TYPE : ctp;
  }

  public static ContentType normalizeResponse(ContentType ctp) {
    return (ctp == null) ? DEFAULT_RESPONSE_CONTENT_TYPE : ctp;
  }

  public static String prepareContent(ContentType type, String data) {
    if (isHtml(type) && BeeUtils.length(data) > 0) {
      return Codec.encodeBase64(data);
    } else {
      return data;
    }
  }

  public static String rpcMessageName(int i) {
    return Service.RPC_VAR_MSG + i;
  }

  public static String rpcParamName(int i) {
    return Service.RPC_VAR_PRM + i;
  }

  private CommUtils() {
  }
}
