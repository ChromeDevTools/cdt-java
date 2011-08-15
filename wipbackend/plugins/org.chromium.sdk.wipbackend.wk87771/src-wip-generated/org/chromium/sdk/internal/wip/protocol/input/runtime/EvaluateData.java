// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/87771/trunk/Source/WebCore/inspector/Inspector.json@86959

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
