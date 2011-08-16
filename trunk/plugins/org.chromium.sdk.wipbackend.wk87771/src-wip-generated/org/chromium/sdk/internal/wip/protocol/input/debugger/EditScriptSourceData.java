// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/87771/trunk/Source/WebCore/inspector/Inspector.json@86959

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Edits JavaScript script live.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface EditScriptSourceData {
  /**
   New stack trace in case editing has happened while VM was stopped.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue> callFrames();

}
