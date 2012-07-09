// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: Local file Inspector-1.0.json.r107603.manual_fix

package org.chromium.sdk.internal.wip.protocol.input.console;

/**
 Stack entry for console errors and assertions.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CallFrameValue {
  /**
   JavaScript function name.
   */
  String functionName();

  /**
   JavaScript script name or url.
   */
  String url();

  /**
   JavaScript script line number.
   */
  long lineNumber();

  /**
   JavaScript script column number.
   */
  long columnNumber();

}
