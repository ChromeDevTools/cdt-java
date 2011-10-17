// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.dom;

/**
Sets attributes on element with given id. This method is useful when user edits some existing attribute value and types in several attribute name/value pairs.
 */
public class SetAttributesAsTextParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param nodeId Id of the element to set attributes for.
   @param text Text with a number of attributes. Will parse this text using HTML parser.
   @param nameOpt Attribute name to replace with new attributes derived from text in case text parsed successfully.
   */
  public SetAttributesAsTextParams(long/*See org.chromium.sdk.internal.wip.protocol.output.dom.NodeIdTypedef*/ nodeId, String text, String nameOpt) {
    this.put("nodeId", nodeId);
    this.put("text", text);
    if (nameOpt != null) {
      this.put("name", nameOpt);
    }
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DOM + ".setAttributesAsText";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
