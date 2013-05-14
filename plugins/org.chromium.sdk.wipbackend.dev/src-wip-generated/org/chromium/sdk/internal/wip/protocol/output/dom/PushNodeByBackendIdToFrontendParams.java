// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Requests that the node is sent to the caller given its backend node id.
 */
public class PushNodeByBackendIdToFrontendParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.dom.PushNodeByBackendIdToFrontendData> {
  /**
   @param backendNodeId The backend node id of the node.
   */
  public PushNodeByBackendIdToFrontendParams(long/*See org.chromium.sdk.internal.wip.protocol.common.dom.BackendNodeIdTypedef*/ backendNodeId) {
    this.put("backendNodeId", backendNodeId);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".pushNodeByBackendIdToFrontend";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.dom.PushNodeByBackendIdToFrontendData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parseDOMPushNodeByBackendIdToFrontendData(data.getUnderlyingObject());
  }

}
