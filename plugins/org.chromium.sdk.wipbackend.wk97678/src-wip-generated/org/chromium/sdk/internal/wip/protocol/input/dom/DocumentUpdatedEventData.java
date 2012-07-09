// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when <code>Document</code> has been totally updated. Node ids are no longer valid.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface DocumentUpdatedEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.DocumentUpdatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.DocumentUpdatedEventData>("DOM.documentUpdated", org.chromium.sdk.internal.wip.protocol.input.dom.DocumentUpdatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.DocumentUpdatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMDocumentUpdatedEventData(obj);
    }
  };
}
