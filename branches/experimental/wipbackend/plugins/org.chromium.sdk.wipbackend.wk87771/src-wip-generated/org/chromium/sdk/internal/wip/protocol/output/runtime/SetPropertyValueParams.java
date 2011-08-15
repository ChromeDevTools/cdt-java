// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/87771/trunk/Source/WebCore/inspector/Inspector.json@86959

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Makes property with given name equal to the expression evaluation result.
 */
public class SetPropertyValueParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param objectId Identifier of the object to set property on.
   @param propertyName Property name to set value for.
   @param expression Expression to evaluate.
   */
  public SetPropertyValueParams(String objectId, String propertyName, String expression) {
    this.put("objectId", objectId);
    this.put("propertyName", propertyName);
    this.put("expression", expression);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.RUNTIME + ".setPropertyValue";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
