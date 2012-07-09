// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@106352

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Information about the function.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FunctionDetailsValue {
  /**
   Location of the function.
   */
  org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location();

  /**
   Name of the function. Not present for anonymous functions.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String name();

  /**
   Display name of the function(specified in 'displayName' property on the function object).
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String displayName();

  /**
   Name of the function inferred from its initial assignment.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String inferredName();

}
