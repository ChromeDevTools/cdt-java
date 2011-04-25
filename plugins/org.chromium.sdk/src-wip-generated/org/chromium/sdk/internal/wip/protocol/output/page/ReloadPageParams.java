// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84481

package org.chromium.sdk.internal.wip.protocol.output.page;

public class ReloadPageParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public ReloadPageParams(Boolean ignoreCacheOpt) {
    if (ignoreCacheOpt != null) {
      this.put("ignoreCache", ignoreCacheOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".reloadPage";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
