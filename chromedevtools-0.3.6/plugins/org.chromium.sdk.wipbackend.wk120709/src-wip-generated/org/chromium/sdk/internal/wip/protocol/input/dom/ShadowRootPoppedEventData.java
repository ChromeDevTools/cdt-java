// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@108993

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Called when shadow root is popped from the element.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ShadowRootPoppedEventData {
  /**
   Host element id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ hostId();

  /**
   Shadow root id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ rootId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPoppedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPoppedEventData>("DOM.shadowRootPopped", org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPoppedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.ShadowRootPoppedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMShadowRootPoppedEventData(obj);
    }
  };
}
