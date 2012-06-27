// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Returns event listeners relevant to the node.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GetEventListenersForNodeData {
  /**
   Array of relevant listeners.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.dom.EventListenerValue> listeners();

}
