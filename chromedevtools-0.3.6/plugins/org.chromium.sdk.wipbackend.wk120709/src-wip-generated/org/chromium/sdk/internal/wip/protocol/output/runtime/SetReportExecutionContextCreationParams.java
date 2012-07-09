// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Enables reporting about creation of isolated contexts by means of <code>isolatedContextCreated</code> event. When the reporting gets enabled the event will be sent immediately for each existing isolated context.
 */
public class SetReportExecutionContextCreationParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param enabled Reporting enabled state.
   */
  public SetReportExecutionContextCreationParams(boolean enabled) {
    this.put("enabled", enabled);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".setReportExecutionContextCreation";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
