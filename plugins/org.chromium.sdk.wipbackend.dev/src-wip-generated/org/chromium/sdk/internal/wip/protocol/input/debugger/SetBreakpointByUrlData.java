// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@114632

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Sets JavaScript breakpoint at given location specified either by URL or URL regex. Once this command is issued, all existing parsed scripts will have breakpoints resolved and returned in <code>locations</code> property. Further matching script parsing will result in subsequent <code>breakpointResolved</code> events issued. This logical breakpoint will survive page reloads.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SetBreakpointByUrlData {
  /**
   Id of the created breakpoint for further reference.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.BreakpointIdTypedef*/ breakpointId();

  /**
   List of the locations this breakpoint resolved into upon addition.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue> locations();

}
