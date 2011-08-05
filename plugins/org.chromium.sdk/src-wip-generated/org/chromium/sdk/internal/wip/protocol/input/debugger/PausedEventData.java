// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/92284/trunk/Source/WebCore/inspector/Inspector.json@92284

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when the virtual machine stopped on breakpoint or exception or any other stop criteria.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface PausedEventData {
  /**
   Call stack information.
   */
  Details details();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData>("Debugger.paused", org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerPausedEventData(obj);
    }
  };
  /**
   Call stack information.
   */
  @org.chromium.sdk.internal.protocolparser.JsonType
  public interface Details {
    /**
     Call stack the virtual machine stopped on.
     */
    java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue> callFrames();

    /**
     Current exception object if script execution is paused when an exception is being thrown.
     */
    @org.chromium.sdk.internal.protocolparser.JsonOptionalField
    org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue exception();

  }
}
