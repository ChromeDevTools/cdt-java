// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Sets JavaScript breakpoint at a given location.
 */
public class SetBreakpointParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData> {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setBreakpoint";

  /**
   @param location Location to set breakpoint in.
   @param conditionOpt Expression to use as a breakpoint condition. When specified, debugger will only stop on the breakpoint if this expression evaluates to true.
   */
  public SetBreakpointParams(org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location, String conditionOpt) {
    this.put("location", location);
    this.put("condition", conditionOpt);
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData.class);
  }

}
