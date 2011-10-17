// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Object property descriptor.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface PropertyDescriptorValue {
  /**
   Property name.
   */
  String name();

  /**
   The value associated with the property.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue value();

  /**
   True if the value associated with the property may be changed (data descriptors only).
   */
  boolean writable();

  /**
   A function which serves as a getter for the property, or <code>undefined</code> if there is no getter (accessor descriptors only).
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue get();

  /**
   A function which serves as a setter for the property, or <code>undefined</code> if there is no setter (accessor descriptors only).
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue set();

  /**
   True if the type of this property descriptor may be changed and if the property may be deleted from the corresponding object.
   */
  boolean configurable();

  /**
   True if this property shows up during enumeration of the properties on the corresponding object.
   */
  boolean enumerable();

  /**
   True if the result was thrown during the evaluation.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  Boolean wasThrown();

}
