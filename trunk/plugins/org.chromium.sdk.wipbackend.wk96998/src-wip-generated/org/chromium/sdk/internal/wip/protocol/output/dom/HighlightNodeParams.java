// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Highlights DOM node with given id.
 */
public class HighlightNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Identifier of the node to highlight.
   @param highlightConfig A descriptor for the highlight appearance.
   */
  public HighlightNodeParams(long/*See org.chromium.sdk.internal.wip.protocol.output.dom.NodeIdTypedef*/ nodeId, org.chromium.sdk.internal.wip.protocol.output.dom.HighlightConfigParam highlightConfig) {
    this.put("nodeId", nodeId);
    this.put("highlightConfig", highlightConfig);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".highlightNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
