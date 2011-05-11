// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@85751

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Evaluate expression on global object.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface EvaluateData {
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
