// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Enables debugger for the given page. Clients should not assume that the debugging has been enabled until the result for this command is received.
 */
public class EnableParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public EnableParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".enable";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
