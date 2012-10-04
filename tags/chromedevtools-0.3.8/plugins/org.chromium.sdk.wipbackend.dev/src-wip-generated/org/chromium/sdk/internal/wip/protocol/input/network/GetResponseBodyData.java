// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Returns content served for the given request.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GetResponseBodyData {
  /**
   Response body.
   */
  String body();

  /**
   True, if content was sent as base64.
   */
  boolean base64Encoded();

}
