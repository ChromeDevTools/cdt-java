// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
A structure holding an RGBA color.
 */
public class RGBAParam extends org.json.simple.JSONObject {
  /**
   @param r The red component, in the [0-255] range.
   @param g The green component, in the [0-255] range.
   @param b The blue component, in the [0-255] range.
   @param aOpt The alpha component, in the [0-1] range (default: 1).
   */
  public RGBAParam(long r, long g, long b, Number aOpt) {
    this.put("r", r);
    this.put("g", g);
    this.put("b", b);
    if (aOpt != null) {
      this.put("a", aOpt);
    }
  }

}
