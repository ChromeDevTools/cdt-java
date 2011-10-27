// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84351

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Releases all remote objects that belong to a given group.
 */
public class ReleaseObjectGroupParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param objectGroup Symbolic object group name.
   */
  public ReleaseObjectGroupParams(String objectGroup) {
    this.put("objectGroup", objectGroup);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".releaseObjectGroup";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
