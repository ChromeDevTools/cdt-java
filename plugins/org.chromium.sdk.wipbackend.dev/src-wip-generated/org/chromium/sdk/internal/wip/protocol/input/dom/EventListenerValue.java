// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 DOM interaction is implemented in terms of mirror objects that represent the actual DOM nodes. DOMNode is a base node mirror type.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface EventListenerValue {
  /**
   <code>EventListener</code>'s type.
   */
  String type();

  /**
   <code>EventListener</code>'s useCapture.
   */
  boolean useCapture();

  /**
   <code>EventListener</code>'s isAttribute.
   */
  boolean isAttribute();

  /**
   Target <code>DOMNode</code> id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  /**
   Event handler function body.
   */
  String handlerBody();

  /**
   Handler code location.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location();

  /**
   Source script URL.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String sourceName();

}
