// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: Local file Inspector-1.0.json.r107603.manual_fix

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Sets node HTML markup, returns new node id.
 */
public class SetOuterHTMLParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Id of the node to set markup for.
   @param outerHTML Outer HTML markup to set.
   */
  public SetOuterHTMLParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId, String outerHTML) {
    this.put("nodeId", nodeId);
    this.put("outerHTML", outerHTML);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".setOuterHTML";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
