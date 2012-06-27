// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Searches for a given string in the DOM tree. Use <code>getSearchResults</code> to access search results or <code>cancelSearch</code> to end this search session.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface PerformSearchData {
  /**
   Unique search session identifier.
   */
  String searchId();

  /**
   Number of search results.
   */
  long resultCount();

}
