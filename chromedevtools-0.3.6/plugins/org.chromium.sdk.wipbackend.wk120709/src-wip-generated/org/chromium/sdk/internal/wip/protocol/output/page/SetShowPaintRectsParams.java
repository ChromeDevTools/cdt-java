// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@108993

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Requests that backend shows paint rectangles
 */
public class SetShowPaintRectsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param result True for showing paint rectangles
   */
  public SetShowPaintRectsParams(boolean result) {
    this.put("result", result);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setShowPaintRects";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
