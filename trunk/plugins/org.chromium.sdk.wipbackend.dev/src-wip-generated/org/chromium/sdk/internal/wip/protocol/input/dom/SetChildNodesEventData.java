// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when backend wants to provide client with the missing DOM structure. This happens upon most of the calls requesting node ids.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SetChildNodesEventData {
  /**
   Parent node id to populate with children.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/ parentId();

  /**
   Child nodes array.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.dom.NodeValue> nodes();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.SetChildNodesEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.SetChildNodesEventData>("DOM.setChildNodes", org.chromium.sdk.internal.wip.protocol.input.dom.SetChildNodesEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.SetChildNodesEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMSetChildNodesEventData(obj);
    }
  };
}
