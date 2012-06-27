// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Determines if scripts can be executed in the page.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface GetScriptExecutionStatusData {
  /**
   Script execution status: "allowed" if scripts can be executed, "disabled" if script execution has been disabled through page settings, "forbidden" if script execution for the given page is not possible for other reasons.
   */
  Result result();

  /**
   Script execution status: "allowed" if scripts can be executed, "disabled" if script execution has been disabled through page settings, "forbidden" if script execution for the given page is not possible for other reasons.
   */
  public enum Result {
    ALLOWED,
    DISABLED,
    FORBIDDEN,
  }
}
