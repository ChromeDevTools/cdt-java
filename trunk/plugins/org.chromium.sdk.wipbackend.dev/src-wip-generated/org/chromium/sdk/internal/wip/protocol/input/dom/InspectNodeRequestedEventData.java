// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@150309 with change #14672031

package org.chromium.sdk.internal.wip.protocol.input.dom;

/**
 Fired when the node should be inspected. This happens after call to <code>setInspectModeEnabled</code>.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface InspectNodeRequestedEventData {
  /**
   Id of the node to inspect.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.dom.NodeIdTypedef*/ nodeId();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.InspectNodeRequestedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.dom.InspectNodeRequestedEventData>("DOM.inspectNodeRequested", org.chromium.sdk.internal.wip.protocol.input.dom.InspectNodeRequestedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.dom.InspectNodeRequestedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDOMInspectNodeRequestedEventData(obj);
    }
  };
}
