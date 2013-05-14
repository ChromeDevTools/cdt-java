// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@150309 with change #14672031

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Sets files for the given file input element.
 */
public class SetFileInputFilesParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Id of the file input node to set files for.
   @param files Array of file paths to set.
   */
  public SetFileInputFilesParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId, java.util.List<String> files) {
    this.put("nodeId", nodeId);
    this.put("files", files);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".setFileInputFiles";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
