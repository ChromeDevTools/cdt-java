// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Search result for resource.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SearchResultValue {
  /**
   Resource URL.
   */
  String url();

  /**
   Resource frame id.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.FrameIdTypedef*/ frameId();

  /**
   Number of matches in the resource content.
   */
  Number matchesCount();

}
