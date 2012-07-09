// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Location in the source code.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface LocationValue {
  /**
   Script identifier as reported in the <code>Debugger.scriptParsed</code>.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.ScriptIdTypedef*/ scriptId();

  /**
   Line number in the script.
   */
  long lineNumber();

  /**
   Column number in the script.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Long columnNumber();

}
