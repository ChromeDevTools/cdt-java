// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when <code>Container</code>'s child node count has changed.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ChildNodeCountUpdatedEventData {
  /**
   Id of the node that has changed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  /**
   New node count.
   */
  long childNodeCount();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeCountUpdatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeCountUpdatedEventData>("DOM.childNodeCountUpdated", org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeCountUpdatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeCountUpdatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMChildNodeCountUpdatedEventData(obj);
    }
  };
}
