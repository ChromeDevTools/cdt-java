// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Called when global has been cleared and debugger client should reset its state. Happens upon navigation or reload.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GlobalObjectClearedEventData {
  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.GlobalObjectClearedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.GlobalObjectClearedEventData>("Debugger.globalObjectCleared", org.chromium.sdk.internal.wip.protocol.input.debugger.GlobalObjectClearedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.GlobalObjectClearedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerGlobalObjectClearedEventData(obj);
    }
  };
}
