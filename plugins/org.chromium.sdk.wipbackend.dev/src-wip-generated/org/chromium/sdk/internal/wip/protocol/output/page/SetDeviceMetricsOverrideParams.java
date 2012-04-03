// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@113013

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Overrides the values of device screen dimensions (window.screen.width, window.screen.height, window.innerWidth, window.innerHeight, and "device-width"/"device-height"-related CSS media query results) and the font scale factor.
 */
public class SetDeviceMetricsOverrideParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param width Overriding width value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   @param height Overriding height value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   @param fontScaleFactor Overriding font scale factor value (must be positive). 1 disables the override.
   */
  public SetDeviceMetricsOverrideParams(long width, long height, Number fontScaleFactor) {
    this.put("width", width);
    this.put("height", height);
    this.put("fontScaleFactor", fontScaleFactor);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setDeviceMetricsOverride";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
