// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@92377

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Location in the source code.
 */
public class LocationParam extends org.json.simple.JSONObject {
  /**
   @param scriptId Script identifier as reported in the <code>Debugger.scriptParsed</code>.
   @param lineNumber Line number in the script.
   @param columnNumberOpt Column number in the script.
   */
  public LocationParam(String/*See org.chromium.sdk.internal.wip.protocol.output.debugger.ScriptIdTypedef*/ scriptId, long lineNumber, Long columnNumberOpt) {
    this.put("scriptId", scriptId);
    this.put("lineNumber", lineNumber);
    if (columnNumberOpt != null) {
      this.put("columnNumber", columnNumberOpt);
    }
  }

}
