// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Runs script with given id in a given context.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RunScriptData {
  /**
   Run result.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue result();

  /**
   True if the result was thrown during the script run.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean wasThrown();

}
