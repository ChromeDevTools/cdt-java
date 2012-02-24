// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Mirrors <code>DOMNodeRemoved</code> event.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ChildNodeRemovedEventData {
  /**
   Parent id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ parentNodeId();

  /**
   Id of the node that has been removed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeRemovedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeRemovedEventData>("DOM.childNodeRemoved", org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeRemovedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeRemovedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMChildNodeRemovedEventData(obj);
    }
  };
}
