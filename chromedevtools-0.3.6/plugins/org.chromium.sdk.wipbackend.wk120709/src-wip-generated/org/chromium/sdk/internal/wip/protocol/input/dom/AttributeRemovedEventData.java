// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when <code>Element</code>'s attribute is removed.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface AttributeRemovedEventData {
  /**
   Id of the node that has changed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  /**
   A ttribute name.
   */
  String name();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.AttributeRemovedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.AttributeRemovedEventData>("DOM.attributeRemoved", org.chromium.sdk.internal.wip.protocol.input.dom.AttributeRemovedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.AttributeRemovedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMAttributeRemovedEventData(obj);
    }
  };
}
