// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Mirror object referencing original JavaScript object.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface RemoteObjectValue {
  /**
   String representation of the object.
   */
  String description();

  /**
   True when this object can be queried for children.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Long hasChildren();

  /**
   Unique object identifier (for non-primitive values).
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String objectId();

  /**
   Object type.
   */
  Type type();

  /**
   Object type.
   */
  public enum Type {
    OBJECT,
    ARRAY,
    FUNCTION,
    NULL,
    NODE,
    UNDEFINED,
    STRING,
    NUMBER,
    BOOLEAN,
    ERROR,
    REGEXP,
    DATE,
  }
}
