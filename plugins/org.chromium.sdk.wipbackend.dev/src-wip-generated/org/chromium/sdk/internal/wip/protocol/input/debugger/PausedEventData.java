// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface PausedEventData {
  /**
   Call stack the virtual machine stopped on.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue> callFrames();

  /**
   Pause reason.
   */
  Reason reason();

  /**
   Object containing break-specific auxiliary properties.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Data data();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData>("Debugger.paused", org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerPausedEventData(obj);
    }
  };
  /**
   Pause reason.
   */
  public enum Reason {
    XHR,
    DOM,
    EVENTLISTENER,
    EXCEPTION,
    ASSERT,
    CSPVIOLATION,
    OTHER,
  }
  /**
   Object containing break-specific auxiliary properties.
   */
  @org.chromium.sdk.internal.protocolparser.JsonType(allowsOtherProperties=true)
  public interface Data extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {
  }
}
