// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Switches script execution in the page.
 */
public class SetScriptExecutionDisabledParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param value Whether script execution should be disabled in the page.
   */
  public SetScriptExecutionDisabledParams(boolean value) {
    this.put("value", value);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".setScriptExecutionDisabled";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
