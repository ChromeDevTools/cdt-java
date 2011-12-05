// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Returns search results from given <code>fromIndex</code> to given <code>toIndex</code> from the sarch with the given identifier.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GetSearchResultsData {
  /**
   Ids of the search result nodes.
   */
  java.util.List<Long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/> nodeIds();

}
