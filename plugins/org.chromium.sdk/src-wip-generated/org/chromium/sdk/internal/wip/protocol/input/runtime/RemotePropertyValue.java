// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Mirror object property.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RemotePropertyValue {
  /**
   Property name.
   */
  String name();

  /**
   Property value.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue value();

  /**
   True if this property is getter.
   */
  String isGetter();

}
