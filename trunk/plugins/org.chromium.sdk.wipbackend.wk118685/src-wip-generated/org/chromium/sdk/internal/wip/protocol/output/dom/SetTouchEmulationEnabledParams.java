// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@106811

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Toggles mouse event-based touch event emulation.
 */
public class SetTouchEmulationEnabledParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param enabled Whether the touch event emulation should be enabled.
   */
  public SetTouchEmulationEnabledParams(boolean enabled) {
    this.put("enabled", enabled);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".setTouchEmulationEnabled";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
