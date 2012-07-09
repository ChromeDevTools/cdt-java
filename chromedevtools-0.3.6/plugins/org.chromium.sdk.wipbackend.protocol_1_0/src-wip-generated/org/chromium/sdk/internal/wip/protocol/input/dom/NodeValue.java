// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: Local file Inspector-1.0.json.r107603.manual_fix

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 DOM interaction is implemented in terms of mirror objects that represent the actual DOM nodes. DOMNode is a base node mirror type.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface NodeValue {
  /**
   Node identifier that is passed into the rest of the DOM messages as the <code>nodeId</code>. Backend will only push node with given <code>id</code> once. It is aware of all requested nodes and will only fire DOM events for nodes known to the client.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  /**
   <code>Node</code>'s nodeType.
   */
  long nodeType();

  /**
   <code>Node</code>'s nodeName.
   */
  String nodeName();

  /**
   <code>Node</code>'s localName.
   */
  String localName();

  /**
   <code>Node</code>'s nodeValue.
   */
  String nodeValue();

  /**
   Child count for <code>Container</code> nodes.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Long childNodeCount();

  /**
   Child nodes of this node when requested with children.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.dom.NodeValue> children();

  /**
   Attributes of the <code>Element</code> node in the form of flat array <code>[name1, value1, name2, value2]</code>.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<String> attributes();

  /**
   Document URL that <code>Document</code> or <code>FrameOwner</code> node points to.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String documentURL();

  /**
   <code>DocumentType</code>'s publicId.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String publicId();

  /**
   <code>DocumentType</code>'s systemId.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String systemId();

  /**
   <code>DocumentType</code>'s internalSubset.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String internalSubset();

  /**
   <code>Document</code>'s XML version in case of XML documents.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String xmlVersion();

  /**
   <code>Attr</code>'s name.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String name();

  /**
   <code>Attr</code>'s value.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String value();

  /**
   Content document for frame owner elements.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.dom.NodeValue contentDocument();

}
