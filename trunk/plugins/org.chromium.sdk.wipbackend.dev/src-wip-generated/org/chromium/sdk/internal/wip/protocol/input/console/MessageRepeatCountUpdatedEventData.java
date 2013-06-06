// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.chromium.sdk.internal.wip.protocol.input.console;

/**
 Issued when subsequent message(s) are equal to the previous one(s).
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface MessageRepeatCountUpdatedEventData {
  /**
   New repeat count value.
   */
  long count();

  /**
   Timestamp of most recent message in batch.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.common.console.TimestampTypedef*/ timestamp();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.console.MessageRepeatCountUpdatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.console.MessageRepeatCountUpdatedEventData>("Console.messageRepeatCountUpdated", org.chromium.sdk.internal.wip.protocol.input.console.MessageRepeatCountUpdatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.console.MessageRepeatCountUpdatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseConsoleMessageRepeatCountUpdatedEventData(obj);
    }
  };
}
