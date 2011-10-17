// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 HTTP request data.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RequestValue {
  /**
   Request URL.
   */
  String url();

  /**
   HTTP request method.
   */
  String method();

  /**
   HTTP request headers.
   */
  org.chromium.sdk.internal.wip.protocol.input.network.HeadersValue headers();

  /**
   HTTP POST request data.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String postData();

}
