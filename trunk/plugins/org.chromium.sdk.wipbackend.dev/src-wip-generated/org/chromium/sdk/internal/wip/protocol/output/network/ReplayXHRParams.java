// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.output.network;

/**
This method sends a new XMLHttpRequest which is identical to the original one. The following parameters should be identical: method, url, async, request body, extra headers, withCredentials attribute, user, password.
 */
public class ReplayXHRParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param requestId Identifier of XHR to replay.
   */
  public ReplayXHRParams(String/*See org.chromium.sdk.internal.wip.protocol.common.network.RequestIdTypedef*/ requestId) {
    this.put("requestId", requestId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.NETWORK + ".replayXHR";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
