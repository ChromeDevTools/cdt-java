// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/93101/trunk/Source/WebCore/inspector/Inspector.json@93101

package org.chromium.sdk.internal.wip.protocol.input.inspector;

@org.chromium.sdk.internal.protocolparser.JsonType
public interface BringToFrontEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.BringToFrontEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.BringToFrontEventData>("Inspector.bringToFront", org.chromium.sdk.internal.wip.protocol.input.inspector.BringToFrontEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.inspector.BringToFrontEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseInspectorBringToFrontEventData(obj);
    }
  };
}
