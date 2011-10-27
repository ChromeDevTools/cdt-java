// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Requests that the node is sent to the caller given the JavaScript node object reference. All nodes that form the path from the node to the root are also sent to the client as a series of <code>setChildNodes</code> notifications.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RequestNodeData {
  /**
   Node id for given object.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.input.dom.NodeIdTypedef*/ nodeId();

}
