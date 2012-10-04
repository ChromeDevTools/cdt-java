// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Overrides the Device Orientation.
 */
public class SetDeviceOrientationOverrideParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param alpha Mock alpha
   @param beta Mock beta
   @param gamma Mock gamma
   */
  public SetDeviceOrientationOverrideParams(Number alpha, Number beta, Number gamma) {
    this.put("alpha", alpha);
    this.put("beta", beta);
    this.put("gamma", gamma);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setDeviceOrientationOverride";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
