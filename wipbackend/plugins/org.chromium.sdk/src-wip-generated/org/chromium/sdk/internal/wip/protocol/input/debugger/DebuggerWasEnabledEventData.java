// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when debugger gets enabled (deprecated).
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface DebuggerWasEnabledEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasEnabledEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasEnabledEventData>("Debugger.debuggerWasEnabled", org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasEnabledEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasEnabledEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerDebuggerWasEnabledEventData(obj);
    }
  };
}
