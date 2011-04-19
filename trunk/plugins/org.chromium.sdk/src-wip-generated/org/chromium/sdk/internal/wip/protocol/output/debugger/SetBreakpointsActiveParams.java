// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Activates / deactivates all breakpoints on the page.
 */
public class SetBreakpointsActiveParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setBreakpointsActive";

  /**
   @param active New value for breakpoints active state.
   */
  public SetBreakpointsActiveParams(boolean active) {
    this.put("active", active);
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
