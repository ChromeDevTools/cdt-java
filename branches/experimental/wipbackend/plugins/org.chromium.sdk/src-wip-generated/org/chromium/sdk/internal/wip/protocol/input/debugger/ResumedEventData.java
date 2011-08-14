// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when the virtual machine resumed execution.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ResumedEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData>("Debugger.resumed", org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerResumedEventData(obj);
    }
  };
}
