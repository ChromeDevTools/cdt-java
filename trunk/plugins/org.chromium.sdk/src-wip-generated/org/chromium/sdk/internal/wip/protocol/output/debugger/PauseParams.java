// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84351

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Stops on the next JavaScript statement.
 */
public class PauseParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public PauseParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".pause";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
