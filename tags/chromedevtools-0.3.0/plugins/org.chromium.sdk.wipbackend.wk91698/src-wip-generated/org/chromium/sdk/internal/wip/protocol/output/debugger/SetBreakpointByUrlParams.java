// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

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
   @param isRegexOpt If true, given <code>url</code> is considered to be a regular expression.
   */
  public SetBreakpointByUrlParams(String url, long lineNumber, Long columnNumberOpt, String conditionOpt, Boolean isRegexOpt) {
    this.put("url", url);
    this.put("lineNumber", lineNumber);
    if (columnNumberOpt != null) {
      this.put("columnNumber", columnNumberOpt);
    }
    if (conditionOpt != null) {
      this.put("condition", conditionOpt);
    }
    if (isRegexOpt != null) {
      this.put("isRegex", isRegexOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setBreakpointByUrl";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDebuggerSetBreakpointByUrlData(data.getUnderlyingObject());
  }

}
