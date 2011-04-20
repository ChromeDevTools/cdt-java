// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84351

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Sets JavaScript breakpoint at a given location specified by URL. This breakpoint will survive page reload.
 */
public class SetBreakpointByUrlParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData> {
  /**
   @param url URL of the resource to set breakpoint on.
   @param lineNumber Line number to set breakpoint at.
   @param columnNumberOpt Offset in the line to set breakpoint at.
   @param conditionOpt Expression to use as a breakpoint condition. When specified, debugger will only stop on the breakpoint if this expression evaluates to true.
   */
  public SetBreakpointByUrlParams(String url, long lineNumber, Long columnNumberOpt, String conditionOpt) {
    this.put("url", url);
    this.put("lineNumber", lineNumber);
    if (columnNumberOpt == null) {
      this.put("columnNumber", columnNumberOpt);
    }
    if (conditionOpt == null) {
      this.put("condition", conditionOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setBreakpointByUrl";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parse(data.getUnderlyingObject(), org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData.class);
  }

}
