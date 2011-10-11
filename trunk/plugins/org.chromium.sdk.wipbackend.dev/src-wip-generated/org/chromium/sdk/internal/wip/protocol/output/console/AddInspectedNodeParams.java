// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.console;

/**
Enables console to refer to the node with given id via $x (see Command Line API for more details $x functions).
 */
public class AddInspectedNodeParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId DOM node id to be accessible by means of $x command line API.
   */
  public AddInspectedNodeParams(long/*See org.chromium.sdk.internal.wip.protocol.output.dom.NodeIdTypedef*/ nodeId) {
    this.put("nodeId", nodeId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.CONSOLE + ".addInspectedNode";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
