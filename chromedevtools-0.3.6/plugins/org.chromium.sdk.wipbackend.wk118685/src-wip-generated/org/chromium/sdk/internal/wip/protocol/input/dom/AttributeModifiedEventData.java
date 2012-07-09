// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when <code>Element</code>'s attribute is modified.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface AttributeModifiedEventData {
  /**
   Id of the node that has changed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  /**
   Attribute name.
   */
  String name();

  /**
   Attribute value.
   */
  String value();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.AttributeModifiedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.AttributeModifiedEventData>("DOM.attributeModified", org.chromium.sdk.internal.wip.protocol.input.dom.AttributeModifiedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.AttributeModifiedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMAttributeModifiedEventData(obj);
    }
  };
}
