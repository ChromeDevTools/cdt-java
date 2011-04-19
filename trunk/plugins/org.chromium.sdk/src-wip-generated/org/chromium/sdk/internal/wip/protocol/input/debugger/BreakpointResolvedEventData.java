// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when breakpoint is resolved to an actual script and location.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface BreakpointResolvedEventData {
  /**
   Breakpoint unique identifier.
   */
  String breakpointId();

  /**
   Actual breakpoint location.
   */
  org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.BreakpointResolvedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.BreakpointResolvedEventData>("Debugger.breakpointResolved", org.chromium.sdk.internal.wip.protocol.input.debugger.BreakpointResolvedEventData.class);
}
