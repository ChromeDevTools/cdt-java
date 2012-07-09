// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@106352

package org.chromium.sdk.internal.wip.protocol.input.network;

/**
 Information about the cached resource.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CachedResourceValue {
  /**
   Resource URL.
   */
  String url();

  /**
   Type of this resource.
   */
  org.chromium.sdk.internal.wip.protocol.input.page.ResourceTypeEnum type();

  /**
   Cached response data.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.network.ResponseValue response();

  /**
   Cached response body size.
   */
  Number bodySize();

}
