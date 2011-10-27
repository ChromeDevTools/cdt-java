// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when virtual machine fails to parse the script.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ScriptFailedToParseEventData {
  /**
   URL of the script that failed to parse.
   */
  String url();

  /**
   Source text of the script that failed to parse.
   */
  String scriptSource();

  /**
   Line offset of the script within the resource.
   */
  long startLine();

  /**
   Line with error.
   */
  long errorLine();

  /**
   Parse error message.
   */
  String errorMessage();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData>("Debugger.scriptFailedToParse", org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerScriptFailedToParseEventData(obj);
    }
  };
}
