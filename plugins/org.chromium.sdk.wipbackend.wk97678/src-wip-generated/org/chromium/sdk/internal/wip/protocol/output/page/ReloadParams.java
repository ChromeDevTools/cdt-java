// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Reloads given page optionally ignoring the cache.
 */
public class ReloadParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param ignoreCacheOpt If true, browser cache is ignored (as if the user pressed Shift+refresh).
   */
  public ReloadParams(Boolean ignoreCacheOpt) {
    if (ignoreCacheOpt != null) {
      this.put("ignoreCache", ignoreCacheOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".reload";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
