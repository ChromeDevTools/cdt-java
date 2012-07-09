// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@121014

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Compiles expression.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CompileScriptData {
  /**
   Id of the script.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.ScriptIdTypedef*/ scriptId();

  /**
   Syntax error message if compilation failed.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String syntaxErrorMessage();

}
