// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@142888

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Fired when the JavaScript is enabled/disabled on the page
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ScriptsEnabledEventData {
  /**
   Whether script execution is enabled or disabled on the page.
   */
  boolean isEnabled();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.ScriptsEnabledEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.page.ScriptsEnabledEventData>("Page.scriptsEnabled", org.chromium.sdk.internal.wip.protocol.input.page.ScriptsEnabledEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.page.ScriptsEnabledEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parsePageScriptsEnabledEventData(obj);
    }
  };
}
