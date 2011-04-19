// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Removes JavaScript breakpoint.
 */
public class RemoveBreakpointParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".removeBreakpoint";

  public RemoveBreakpointParams(String breakpointId) {
    this.put("breakpointId", breakpointId);
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
