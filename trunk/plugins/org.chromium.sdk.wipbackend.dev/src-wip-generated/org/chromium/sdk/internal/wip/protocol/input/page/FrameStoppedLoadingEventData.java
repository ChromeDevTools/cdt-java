// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@142888

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Fired when frame has stopped loading.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FrameStoppedLoadingEventData {
  /**
   Id of the frame that has stopped loading.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.FrameIdTypedef*/ frameId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameStoppedLoadingEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameStoppedLoadingEventData>("Page.frameStoppedLoading", org.chromium.sdk.internal.wip.protocol.input.page.FrameStoppedLoadingEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.page.FrameStoppedLoadingEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parsePageFrameStoppedLoadingEventData(obj);
    }
  };
}
