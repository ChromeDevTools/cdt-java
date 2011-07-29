// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Sets JavaScript breakpoint at a given location specified by URL. This breakpoint will survive page reload.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SetBreakpointByUrlData {
  /**
   Id of the created breakpoint for further manipulations.
   */
  String breakpointId();

  /**
   List of the locations this breakpoint resolved into.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue> locations();

}
