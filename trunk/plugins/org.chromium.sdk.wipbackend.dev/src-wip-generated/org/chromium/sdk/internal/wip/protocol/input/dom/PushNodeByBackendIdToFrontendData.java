// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Requests that the node is sent to the caller given its backend node id.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface PushNodeByBackendIdToFrontendData {
  /**
   The pushed node's id.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

}
