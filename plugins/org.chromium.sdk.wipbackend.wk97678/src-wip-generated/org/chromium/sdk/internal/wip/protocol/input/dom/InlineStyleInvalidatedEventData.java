// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when <code>Element</code>'s inline style is modified via a CSS property modification.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface InlineStyleInvalidatedEventData {
  /**
   Ids of the nodes for which the inline styles have been invalidated.
   */
  java.util.List<Long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/> nodeIds();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.InlineStyleInvalidatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.InlineStyleInvalidatedEventData>("DOM.inlineStyleInvalidated", org.chromium.sdk.internal.wip.protocol.input.dom.InlineStyleInvalidatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.InlineStyleInvalidatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMInlineStyleInvalidatedEventData(obj);
    }
  };
}
