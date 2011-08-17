// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Location in the source code.
 */
public class LocationParam extends org.json.simple.JSONObject {
  /**
   @param sourceId Script identifier as reported by the <code>scriptParsed</code>
   @param lineNumber Line number in the script.
   @param columnNumberOpt Column number in the script.
   */
  public LocationParam(String sourceId, long lineNumber, Long columnNumberOpt) {
    this.put("sourceId", sourceId);
    this.put("lineNumber", lineNumber);
    if (columnNumberOpt != null) {
      this.put("columnNumber", columnNumberOpt);
    }
  }

}
