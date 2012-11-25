// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@135591

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Controls the visibility of compositing borders.
 */
public class SetCompositingBordersVisibleParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param visible True for showing compositing borders.
   */
  public SetCompositingBordersVisibleParams(boolean visible) {
    this.put("visible", visible);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setCompositingBordersVisible";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
