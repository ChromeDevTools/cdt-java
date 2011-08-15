// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84775

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Opens given URL either in the inspected page or in a new tab / window.
 */
public class OpenParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param url URL to open.
   @param newWindowOpt If True, opens given URL in a new window or tab.
   */
  public OpenParams(String url, Boolean newWindowOpt) {
    this.put("url", url);
    if (newWindowOpt != null) {
      this.put("newWindow", newWindowOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".open";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
