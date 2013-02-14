// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@142888

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Requests that backend enables continuous painting
 */
public class SetContinuousPaintingEnabledParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param enabled True for enabling cointinuous painting
   */
  public SetContinuousPaintingEnabledParams(boolean enabled) {
    this.put("enabled", enabled);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setContinuousPaintingEnabled";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
