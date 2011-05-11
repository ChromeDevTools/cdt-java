// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@85751

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Evaluate expression on given object using it as <code>this</code>.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface EvaluateOnData {
  /**
   Evaluation result.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue result();

  /**
   True iff the result was thrown during the evaluation.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean wasThrown();

}
