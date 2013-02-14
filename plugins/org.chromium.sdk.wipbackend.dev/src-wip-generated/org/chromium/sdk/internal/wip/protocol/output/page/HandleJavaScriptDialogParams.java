// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@142888

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Accepts or dismisses a JavaScript initiated dialog (alert, confirm, prompt, or onbeforeunload).
 */
public class HandleJavaScriptDialogParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param accept Whether to accept or dismiss the dialog.
   */
  public HandleJavaScriptDialogParams(boolean accept) {
    this.put("accept", accept);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".handleJavaScriptDialog";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
