// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Fired once navigation of the frame has completed. Frame is now associated with the new loader.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FrameNavigatedEventData {
  /**
   Frame object.
   */
  org.chromium.sdk.internal.wip.protocol.input.page.FrameValue frame();

  /**
   Loader identifier.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.input.network.LoaderIdTypedef*/ loaderId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData>("Page.frameNavigated", org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parsePageFrameNavigatedEventData(obj);
    }
  };
}
