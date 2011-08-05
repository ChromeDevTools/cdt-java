// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Fired when frame has been detached from its parent.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FrameDetachedEventData {
  /**
   Id of the frame that has been detached.
   */
  String frameId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameDetachedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameDetachedEventData>("Page.frameDetached", org.chromium.sdk.internal.wip.protocol.input.page.FrameDetachedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.page.FrameDetachedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parsePageFrameDetachedEventData(obj);
    }
  };
}
