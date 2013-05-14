// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Highlights given quad. Coordinates are absolute with respect to the main frame viewport.
 */
public class HighlightQuadParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param quad Quad to highlight
   @param colorOpt The highlight fill color (default: transparent).
   @param outlineColorOpt The highlight outline color (default: transparent).
   */
  public HighlightQuadParams(java.util.List<Number>/*See org.chromium.sdk.internal.wip.protocol.output.dom.QuadTypedef*/ quad, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam colorOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam outlineColorOpt) {
    this.put("quad", quad);
    if (colorOpt != null) {
      this.put("color", colorOpt);
    }
    if (outlineColorOpt != null) {
      this.put("outlineColor", outlineColorOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".highlightQuad";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
