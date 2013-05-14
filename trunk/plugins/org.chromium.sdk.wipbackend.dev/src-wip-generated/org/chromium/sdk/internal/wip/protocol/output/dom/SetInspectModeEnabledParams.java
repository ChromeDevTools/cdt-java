// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Enters the 'inspect' mode. In this mode, elements that user is hovering over are highlighted. Backend then generates 'inspectNodeRequested' event upon element selection.
 */
public class SetInspectModeEnabledParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param enabled True to enable inspection mode, false to disable it.
   @param highlightConfigOpt A descriptor for the highlight appearance of hovered-over nodes. May be omitted if <code>enabled == false</code>.
   */
  public SetInspectModeEnabledParams(boolean enabled, org.chromium.sdk.internal.wip.protocol.output.dom.HighlightConfigParam highlightConfigOpt) {
    this.put("enabled", enabled);
    if (highlightConfigOpt != null) {
      this.put("highlightConfig", highlightConfigOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".setInspectModeEnabled";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
