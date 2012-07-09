// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@108993

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Called when shadow root is pushed into the element.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ShadowRootPushedEventData {
  /**
   Host element id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ hostId();

  /**
   Shadow root.
   */
  org.chromium.sdk.internal.wip.protocol.input.dom.NodeValue root();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPushedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPushedEventData>("DOM.shadowRootPushed", org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPushedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPushedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMShadowRootPushedEventData(obj);
    }
  };
}
