// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@135591

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when the virtual machine resumed execution.
 */
@org.chromium.sdk.internal.protocolparser.JsonType(allowsOtherProperties=true)
public interface ResumedEventData extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData>("Debugger.resumed", org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerResumedEventData(obj);
    }
  };
}
