// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Starts asynchronous search for a given string in the DOM tree. Use <code>cancelSearch</code> to stop given asynchronous search task.
 */
public class PerformSearchParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param query Plain text or query selector or XPath search query.
   @param runSynchronouslyOpt When set to true, performing search synchronously (can block user interaction).
   */
  public PerformSearchParams(String query, Boolean runSynchronouslyOpt) {
    this.put("query", query);
    if (runSynchronouslyOpt != null) {
      this.put("runSynchronously", runSynchronouslyOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".performSearch";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
