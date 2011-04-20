// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/src-dynamic-impl/parser/org/chromium/sdk/internal/wip/tools/protocolgenerator/Inspector.84172.json@607

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
    if (columnNumberOpt == null) {
      this.put("columnNumber", columnNumberOpt);
    }
  }

}
