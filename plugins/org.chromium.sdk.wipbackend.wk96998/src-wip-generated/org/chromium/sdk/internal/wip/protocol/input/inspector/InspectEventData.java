// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/96998/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.inspector;

@org.chromium.sdk.internal.protocolparser.JsonType
public interface InspectEventData {
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue object();

  Hints hints();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.InspectEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.InspectEventData>("Inspector.inspect", org.chromium.sdk.internal.wip.protocol.input.inspector.InspectEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.inspector.InspectEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseInspectorInspectEventData(obj);
    }
  };
  @org.chromium.sdk.internal.protocolparser.JsonType(allowsOtherProperties=true)
  public interface Hints extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {
  }
}
