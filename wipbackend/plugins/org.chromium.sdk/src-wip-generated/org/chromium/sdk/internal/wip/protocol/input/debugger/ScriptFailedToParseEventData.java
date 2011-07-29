// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

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
  String data();

  /**
   Line offset of the script within the resource.
   */
  long firstLine();

  /**
   Line with error.
   */
  long errorLine();

  /**
   Parse error message.
   */
  String errorMessage();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData>("Debugger.scriptFailedToParse", org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData.class);
}
