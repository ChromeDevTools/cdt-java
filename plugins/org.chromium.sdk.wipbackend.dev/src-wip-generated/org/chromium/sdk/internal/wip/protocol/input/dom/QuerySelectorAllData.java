// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@102140

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Executes <code>querySelectorAll</code> on a given node.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface QuerySelectorAllData {
  /**
   Query selector result.
   */
  java.util.List<Long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/> nodeIds();

}
