// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Mirrors <code>DOMNodeInserted</code> event.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ChildNodeInsertedEventData {
  /**
   Id of the node that has changed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/ parentNodeId();

  /**
   If of the previous siblint.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/ previousNodeId();

  /**
   Inserted node data.
   */
  org.chromium.sdk.internal.wip.protocol.input.dom.NodeValue node();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeInsertedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeInsertedEventData>("DOM.childNodeInserted", org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeInsertedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeInsertedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMChildNodeInsertedEventData(obj);
    }
  };
}
