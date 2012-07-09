// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.console;

/**
 Issued when new console message is added.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface MessageAddedEventData {
  /**
   Console message that has been added.
   */
  org.chromium.sdk.internal.wip.protocol.input.console.ConsoleMessageValue message();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.console.MessageAddedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.console.MessageAddedEventData>("Console.messageAdded", org.chromium.sdk.internal.wip.protocol.input.console.MessageAddedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.console.MessageAddedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseConsoleMessageAddedEventData(obj);
    }
  };
}
