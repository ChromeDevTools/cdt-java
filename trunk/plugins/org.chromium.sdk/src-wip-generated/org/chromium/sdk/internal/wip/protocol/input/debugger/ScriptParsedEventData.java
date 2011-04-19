// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Fired when virtual machine parses script. This even is also fired for all known scripts upon enabling debugger.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ScriptParsedEventData {
  /**
   Identifier of the script parsed.
   */
  String sourceID();

  /**
   URL of the script parsed (if any).
   */
  String url();

  /**
   Line offset of the script within the resource with given URL (for script tags).
   */
  long lineOffset();

  /**
   Column offset of the script within the resource with given URL.
   */
  long columnOffset();

  /**
   Length of the script
   */
  long length();

  /**
   Determines whether this script is a user extension script.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean isContentScript();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData>("Debugger.scriptParsed", org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData.class);
}
