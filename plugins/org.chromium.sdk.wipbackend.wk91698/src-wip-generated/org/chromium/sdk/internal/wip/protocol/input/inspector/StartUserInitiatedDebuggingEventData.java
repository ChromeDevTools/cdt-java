// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.input.inspector;

@org.chromium.sdk.internal.protocolparser.JsonType
public interface StartUserInitiatedDebuggingEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.StartUserInitiatedDebuggingEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.StartUserInitiatedDebuggingEventData>("Inspector.startUserInitiatedDebugging", org.chromium.sdk.internal.wip.protocol.input.inspector.StartUserInitiatedDebuggingEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.inspector.StartUserInitiatedDebuggingEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseInspectorStartUserInitiatedDebuggingEventData(obj);
    }
  };
}
