// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Sets JavaScript breakpoint at given location specified either by URL or URL regex. Once this command is issued, all existing parsed scripts will have breakpoints resolved and returned in <code>locations</code> property. Further matching script parsing will result in subsequent <code>breakpointResolved</code> events issued. This logical breakpoint will survive page reloads.
 */
public class SetBreakpointByUrlParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData> {
  /**
   @param lineNumber Line number to set breakpoint at.
   @param urlOpt URL of the resources to set breakpoint on.
   @param urlRegexOpt Regex pattern for the URLs of the resources to set breakpoints on. Either <code>url</code> or <code>urlRegex</code> must be specified.
   @param columnNumberOpt Offset in the line to set breakpoint at.
   @param conditionOpt Expression to use as a breakpoint condition. When specified, debugger will only stop on the breakpoint if this expression evaluates to true.
   */
  public SetBreakpointByUrlParams(long lineNumber, String urlOpt, String urlRegexOpt, Long columnNumberOpt, String conditionOpt) {
    this.put("lineNumber", lineNumber);
    if (urlOpt != null) {
      this.put("url", urlOpt);
    }
    if (urlRegexOpt != null) {
      this.put("urlRegex", urlRegexOpt);
    }
    if (columnNumberOpt != null) {
      this.put("columnNumber", columnNumberOpt);
    }
    if (conditionOpt != null) {
      this.put("condition", conditionOpt);
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
