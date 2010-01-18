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
  private final String variableFqn;

  private final NameDecorator nameDecorator;

  /** The lazily constructed value of this variable. */
  private final JsValueImpl value;

  /** Variable name. */
  private final String rawName;

  /**
   * Constructs a variable contained in the given context with the given
   * value mirror.
   *
   * @param context that owns this variable
   * @param valueData value data for this variable
   */
  JsVariableImpl(InternalContext context, ValueMirror valueData, String name) {
    this(context, valueData, name, null, NameDecorator.NOOP);
  }

  /**
   * Constructs a variable contained in the given context with the given
   * value mirror.
   *
   * @param context that owns this variable
   * @param valueData for this variable
   * @param variableFqn the fully qualified name of this variable
   */
  JsVariableImpl(InternalContext context, ValueMirror valueData, String name, String variableFqn,
      NameDecorator nameDecorator) {
    this.context = context;
    this.valueData = valueData;
    this.rawName = name;
    this.variableFqn = variableFqn;
    this.nameDecorator = nameDecorator;

    Type type = this.valueData.getType();
    switch (type) {
      case TYPE_FUNCTION:
        this.value = new JsFunctionImpl(context, this.variableFqn, this.valueData);
        break;
      case TYPE_ERROR:
      case TYPE_OBJECT:
        this.value = new JsObjectImpl(context, this.variableFqn, this.valueData);
        break;
      case TYPE_ARRAY:
        this.value = new JsArrayImpl(context, this.variableFqn, this.valueData);
        break;
      default:
        this.value = new JsValueImpl(this.valueData);
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
    return nameDecorator.decorateVarName(rawName);
  }

  public String getRawName() {
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
    return variableFqn != null
        ? variableFqn
        : getName();
  }

  static abstract class NameDecorator {
    static final NameDecorator NOOP = new NameDecorator() {
      @Override
      String decorateVarName(String rawName) {
        return rawName;
      }
      @Override
      String buildAccessSuffix(String rawName) {
        return "." + rawName;
      }
    };
    abstract String decorateVarName(String rawName);
    abstract String buildAccessSuffix(String rawName);
  }
}
