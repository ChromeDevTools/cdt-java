// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@89368

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Edits JavaScript source live.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface SetScriptSourceData {
  /**
   New stack trace in case editing has happened while VM was stopped.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue> callFrames();

  /**
   VM-specific description of the changes applied.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Result result();

  /**
   VM-specific description of the changes applied.
   */
  @org.chromium.sdk.internal.protocolparser.JsonType(allowsOtherProperties=true)
  public interface Result extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {
  }
}
