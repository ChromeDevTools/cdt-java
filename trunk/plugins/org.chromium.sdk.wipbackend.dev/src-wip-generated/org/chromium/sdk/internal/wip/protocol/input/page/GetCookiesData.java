// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Returns all browser cookies. Depending on the backend support, will either return detailed cookie information in the <code>cookie</code> field or string cookie representation using <code>cookieString</code>.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GetCookiesData {
  /**
   Array of cookie objects.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.page.CookieValue> cookies();

  /**
   document.cookie string representation of the cookies.
   */
  String cookiesString();

}
