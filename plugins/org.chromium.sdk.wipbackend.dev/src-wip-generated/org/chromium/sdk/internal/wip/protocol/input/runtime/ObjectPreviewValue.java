// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Object containing abbreviated remote object value.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ObjectPreviewValue {
  /**
   Determines whether preview is lossless (contains all information of the original object).
   */
  boolean lossless();

  /**
   True iff some of the properties of the original did not fit.
   */
  boolean overflow();

  /**
   List of the properties.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.runtime.PropertyPreviewValue> properties();

}
