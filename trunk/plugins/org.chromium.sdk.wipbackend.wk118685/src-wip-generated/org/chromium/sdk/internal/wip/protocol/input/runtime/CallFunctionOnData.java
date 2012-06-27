// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Calls function with given declaration on the given object. Object group of the result is inherited from the target object.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CallFunctionOnData {
  /**
   Call result.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue result();

  /**
   True if the result was thrown during the evaluation.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean wasThrown();

}
