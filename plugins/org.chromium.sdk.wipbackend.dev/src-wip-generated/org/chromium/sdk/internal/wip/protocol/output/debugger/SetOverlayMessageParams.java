// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Sets overlay message.
 */
public class SetOverlayMessageParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param messageOpt Overlay message to display when paused in debugger.
   */
  public SetOverlayMessageParams(String messageOpt) {
    if (messageOpt != null) {
      this.put("message", messageOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setOverlayMessage";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
