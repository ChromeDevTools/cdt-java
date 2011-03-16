// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;


import org.chromium.sdk.JsVariable;
import org.chromium.sdk.JsValue.Type;

/**
 * A generic implementation of the JsVariable interface.
 */
public class JsVariableImpl implements JsVariable {

  /**
   * The variable value data as reported by the JavaScript VM (is used to
   * construct the variable value.)
   */
  private final ValueMirror valueData;

  /** The context this variable belongs in. */
  private final InternalContext context;

  /** The fully qualified name of this variable. */
  private final String qualifiedName;

  /** The lazily constructed value of this variable. */
  private final JsValueImpl value;

  /** Variable name. */
  private final Object rawName;

  /** Variable name. */
  private final String decoratedName;

  /**
   * Constructs a variable contained in the given context with the given
   * value mirror.
   *
   * @param context that owns this variable
   * @param valueData value data for this variable
   */
  JsVariableImpl(InternalContext context, ValueMirror valueData, String name) {
    this(context, valueData, name, name, name);
  }

  /**
   * Constructs a variable contained in the given context with the given
   * value mirror.
   *
   * @param context that owns this variable
   * @param valueData for this variable
   * @param qualifiedName the fully qualified name of this variable
   */
  JsVariableImpl(InternalContext context, ValueMirror valueData, Object rawName,
      String decoratedName, String qualifiedName) {
    this.context = context;
    this.valueData = valueData;
    this.rawName = rawName;
    this.decoratedName = decoratedName;
    this.qualifiedName = qualifiedName;

    this.value = createValue(context, valueData, qualifiedName);
  }

  public static JsValueImpl createValue(InternalContext context, ValueMirror valueData,
      String qualifiedName) {
    Type type = valueData.getType();
    switch (type) {
      case TYPE_FUNCTION:
        return new JsFunctionImpl(context, qualifiedName, valueData);
      case TYPE_ERROR:
      case TYPE_OBJECT:
        return new JsObjectImpl(context, qualifiedName, valueData);
      case TYPE_ARRAY:
        return new JsArrayImpl(context, qualifiedName, valueData);
      default:
        return new JsValueImpl(valueData);
    }
  }


  /**
   * @return a [probably compound] JsValue corresponding to this variable.
   *         {@code null} if there was an error lazy-loading the value data.
   */
  public JsValueImpl getValue() {
    return value;
  }

  public String getName() {
    return decoratedName;
  }

  public String getRawName() {
    return this.rawName.toString();
  }

  Object getRawNameAsObject() {
    return this.rawName;
  }

  public boolean isMutable() {
    return false; // TODO(apavlov): fix once V8 supports it
  }

  public boolean isReadable() {
    // TODO(apavlov): implement once the readability metadata are available
    return true;
  }

  public synchronized void setValue(String newValue, SetValueCallback callback) {
    // TODO(apavlov): currently V8 does not support it
    if (!isMutable()) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("[JsVariable: name=")
        .append(getName())
        .append(",value=")
        .append(getValue())
        .append(']')
        .toString();
  }

  /**
   * Returns the context owning this variable.
   */
  protected InternalContext getInternalContext() {
    return context;
  }

  public ValueMirror getMirror() {
    return valueData;
  }

  public String getFullyQualifiedName() {
    return qualifiedName != null
        ? qualifiedName
        : getName();
  }

  static class NameDecorator {
    static String decorateVarName(Object rawName) {
      if (rawName instanceof Number) {
        return OPEN_BRACKET + rawName + CLOSE_BRACKET;
      } else {
        return rawName.toString();
      }
    }
    static String buildAccessSuffix(Object rawName) {
      if (rawName instanceof Number) {
        return OPEN_BRACKET + rawName + CLOSE_BRACKET;
      } else {
        return "." + rawName;
      }
    }
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
  }
}
