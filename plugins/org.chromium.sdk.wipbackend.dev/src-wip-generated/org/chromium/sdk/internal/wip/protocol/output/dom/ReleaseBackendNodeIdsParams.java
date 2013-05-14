// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@150309 with change #14672031

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Requests that group of <code>BackendNodeIds</code> is released.
 */
public class ReleaseBackendNodeIdsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeGroup The backend node ids group name.
   */
  public ReleaseBackendNodeIdsParams(String nodeGroup) {
    this.put("nodeGroup", nodeGroup);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".releaseBackendNodeIds";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
