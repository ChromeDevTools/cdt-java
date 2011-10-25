// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/93101/trunk/Source/WebCore/inspector/Inspector.json@93101

package org.chromium.sdk.internal.wip.protocol.input.inspector;

@org.chromium.sdk.internal.protocolparser.JsonType
public interface DisconnectFromBackendEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.DisconnectFromBackendEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.DisconnectFromBackendEventData>("Inspector.disconnectFromBackend", org.chromium.sdk.internal.wip.protocol.input.inspector.DisconnectFromBackendEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.inspector.DisconnectFromBackendEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseInspectorDisconnectFromBackendEventData(obj);
    }
  };
}
