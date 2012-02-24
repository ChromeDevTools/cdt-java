// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: Local file Inspector-1.0.json.r107603.manual_fix

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when virtual machine parses script. This event is also fired for all known and uncollected scripts upon enabling debugger.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ScriptParsedEventData {
  /**
   Identifier of the script parsed.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.debugger.ScriptIdTypedef*/ scriptId();

  /**
   URL or name of the script parsed (if any).
   */
  String url();

  /**
   Line offset of the script within the resource with given URL (for script tags).
   */
  long startLine();

  /**
   Column offset of the script within the resource with given URL.
   */
  long startColumn();

  /**
   Last line of the script.
   */
  long endLine();

  /**
   Length of the last line of the script.
   */
  long endColumn();

  /**
   Determines whether this script is a user extension script.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean isContentScript();

  /**
   URL of source map associated with script (if any).
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String sourceMapURL();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData>("Debugger.scriptParsed", org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseDebuggerScriptParsedEventData(obj);
    }
  };
}
