// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@85751

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
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData>("Debugger.paused", org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData.class);
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
