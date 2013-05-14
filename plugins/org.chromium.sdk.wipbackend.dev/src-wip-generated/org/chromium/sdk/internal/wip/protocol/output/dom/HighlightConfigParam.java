// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Configuration data for the highlighting of page elements.
 */
public class HighlightConfigParam extends org.json.simple.JSONObject {
  /**
   @param showInfoOpt Whether the node info tooltip should be shown (default: false).
   @param contentColorOpt The content box highlight fill color (default: transparent).
   @param paddingColorOpt The padding highlight fill color (default: transparent).
   @param borderColorOpt The border highlight fill color (default: transparent).
   @param marginColorOpt The margin highlight fill color (default: transparent).
   @param eventTargetColorOpt The event target element highlight fill color (default: transparent).
   */
  public HighlightConfigParam(Boolean showInfoOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam contentColorOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam paddingColorOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam borderColorOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam marginColorOpt, org.chromium.sdk.internal.wip.protocol.output.dom.RGBAParam eventTargetColorOpt) {
    if (showInfoOpt != null) {
      this.put("showInfo", showInfoOpt);
    }
    if (contentColorOpt != null) {
      this.put("contentColor", contentColorOpt);
    }
    if (paddingColorOpt != null) {
      this.put("paddingColor", paddingColorOpt);
    }
    if (borderColorOpt != null) {
      this.put("borderColor", borderColorOpt);
    }
    if (marginColorOpt != null) {
      this.put("marginColor", marginColorOpt);
    }
    if (eventTargetColorOpt != null) {
      this.put("eventTargetColor", eventTargetColorOpt);
    }
  }

}
