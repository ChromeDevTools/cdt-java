// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: Local file Inspector-1.0.json.r107603.manual_fix

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Overrides the values of window.screen.width, window.screen.height, window.innerWidth, window.innerHeight, and "device-width"/"device-height"-related CSS media query results
 */
public class SetScreenSizeOverrideParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param width Overriding width value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   @param height Overriding height value in pixels (minimum 0, maximum 10000000). 0 disables the override.
   */
  public SetScreenSizeOverrideParams(long width, long height) {
    this.put("width", width);
    this.put("height", height);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setScreenSizeOverride";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
