// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@140428

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Reloads given page optionally ignoring the cache.
 */
public class ReloadParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param ignoreCacheOpt If true, browser cache is ignored (as if the user pressed Shift+refresh).
   @param scriptToEvaluateOnLoadOpt If set, the script will be injected into all frames of the inspected page after reload.
   @param scriptPreprocessorOpt Script body that should evaluate to function that will preprocess all the scripts before their compilation.
   */
  public ReloadParams(Boolean ignoreCacheOpt, String scriptToEvaluateOnLoadOpt, String scriptPreprocessorOpt) {
    if (ignoreCacheOpt != null) {
      this.put("ignoreCache", ignoreCacheOpt);
    }
    if (scriptToEvaluateOnLoadOpt != null) {
      this.put("scriptToEvaluateOnLoad", scriptToEvaluateOnLoadOpt);
    }
    if (scriptPreprocessorOpt != null) {
      this.put("scriptPreprocessor", scriptPreprocessorOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".reload";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
