// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.chromium.sdk.internal.wip.protocol.input.console;

/**
 Console message.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ConsoleMessageValue {
  /**
   Message source.
   */
  Source source();

  /**
   Message severity.
   */
  Level level();

  /**
   Message text.
   */
  String text();

  /**
   Console message type.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Type type();

  /**
   URL of the message origin.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String url();

  /**
   Line number in the resource that generated this message.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Long line();

  /**
   Repeat count for repeated messages.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Long repeatCount();

  /**
   Message parameters in case of the formatted message.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue> parameters();

  /**
   JavaScript stack trace for assertions and error messages.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.console.CallFrameValue>/*See org.chromium.sdk.internal.wip.protocol.input.console.StackTraceTypedef*/ stackTrace();

  /**
   Identifier of the network request associated with this message.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ networkRequestId();

  /**
   Timestamp, when this message was fired.
   */
  Number/*See org.chromium.sdk.internal.wip.protocol.common.console.TimestampTypedef*/ timestamp();

  /**
   Message source.
   */
  public enum Source {
    XML,
    JAVASCRIPT,
    NETWORK,
    CONSOLE_API,
    STORAGE,
    APPCACHE,
    RENDERING,
    CSS,
    SECURITY,
    OTHER,
    DEPRECATION,
  }
  /**
   Message severity.
   */
  public enum Level {
    LOG,
    WARNING,
    ERROR,
    DEBUG,
  }
  /**
   Console message type.
   */
  public enum Type {
    LOG,
    DIR,
    DIRXML,
    TABLE,
    TRACE,
    CLEAR,
    STARTGROUP,
    STARTGROUPCOLLAPSED,
    ENDGROUP,
    ASSERT,
    TIMING,
    PROFILE,
    PROFILEEND,
  }
}
