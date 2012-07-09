// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.console;

/**
Disables console domain, prevents further console messages from being reported to the client.
 */
public class DisableParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public DisableParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.CONSOLE + ".disable";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
