// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84775

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Deletes browser cookie with given name for the given domain.
 */
public class DeleteCookieParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param cookieName Name of the cookie to remove.
   @param domain Domain of the cookie to remove.
   */
  public DeleteCookieParams(String cookieName, String domain) {
    this.put("cookieName", cookieName);
    this.put("domain", domain);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".deleteCookie";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
