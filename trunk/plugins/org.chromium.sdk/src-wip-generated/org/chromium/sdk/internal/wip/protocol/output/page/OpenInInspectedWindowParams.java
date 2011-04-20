// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84351

package org.chromium.sdk.internal.wip.protocol.output.page;

public class OpenInInspectedWindowParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public OpenInInspectedWindowParams(String url) {
    this.put("url", url);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".openInInspectedWindow";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
