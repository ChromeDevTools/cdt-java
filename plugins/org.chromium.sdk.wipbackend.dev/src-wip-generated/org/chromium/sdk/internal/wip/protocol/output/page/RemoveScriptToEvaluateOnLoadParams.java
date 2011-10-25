// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@98328

package org.chromium.sdk.internal.wip.protocol.output.page;

public class RemoveScriptToEvaluateOnLoadParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public RemoveScriptToEvaluateOnLoadParams(String/*See org.chromium.sdk.internal.wip.protocol.output.page.ScriptIdentifierTypedef*/ identifier) {
    this.put("identifier", identifier);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".removeScriptToEvaluateOnLoad";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
