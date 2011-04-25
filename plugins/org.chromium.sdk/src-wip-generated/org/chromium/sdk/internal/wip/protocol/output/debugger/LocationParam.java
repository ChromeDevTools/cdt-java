// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84481

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Location in the source code.
 */
public class LocationParam extends org.json.simple.JSONObject {
  /**
   @param sourceID Script identifier as reported by the <code>scriptParsed</code>
   @param lineNumber Line number in the script.
   @param columnNumberOpt Column number in the script.
   */
  public LocationParam(String sourceID, long lineNumber, Long columnNumberOpt) {
    this.put("sourceID", sourceID);
    this.put("lineNumber", lineNumber);
    if (columnNumberOpt != null) {
      this.put("columnNumber", columnNumberOpt);
    }
  }

}
