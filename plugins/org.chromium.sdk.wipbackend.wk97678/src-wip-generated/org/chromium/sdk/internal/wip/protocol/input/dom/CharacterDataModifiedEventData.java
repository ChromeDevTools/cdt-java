// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Mirrors <code>DOMCharacterDataModified</code> event.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CharacterDataModifiedEventData {
  /**
   Id of the node that has changed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/ nodeId();

  /**
   New text value.
   */
  String characterData();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.CharacterDataModifiedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.CharacterDataModifiedEventData>("DOM.characterDataModified", org.chromium.sdk.internal.wip.protocol.input.dom.CharacterDataModifiedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.CharacterDataModifiedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMCharacterDataModifiedEventData(obj);
    }
  };
}
