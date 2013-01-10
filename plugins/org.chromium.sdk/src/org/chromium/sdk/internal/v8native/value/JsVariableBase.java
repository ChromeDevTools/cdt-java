// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;


import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObjectProperty;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.JsEvaluateContextImpl;

/**
 * A generic implementation of the JsVariable interface.
 */
public abstract class JsVariableBase implements JsVariable {

  private final Host host;

  /** The value of this variable. */
  private volatile JsValueBase value;

  /** Variable name. */
  private final Object rawName;

  /**
   * Constructs a variable contained in the given context with the given value mirror.
   * @param valueLoader that owns this variable
   * @param valueData for this variable
   */
  public JsVariableBase(Host host, ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
    this.host = host;
    this.rawName = rawName;
    this.value = createValue(valueLoader, valueData);
  }

  public static JsValueBase createValue(ValueLoader valueLoader, ValueMirror valueData) {
    Type type = valueData.getType();
    switch (type) {
      case TYPE_FUNCTION:
        return new JsFunctionImpl(valueLoader, valueData);
      case TYPE_ERROR:
      case TYPE_OBJECT:
      case TYPE_DATE:
      case TYPE_REGEXP:
        return new JsObjectBase.Impl(valueLoader, valueData);
      case TYPE_ARRAY:
        return new JsArrayImpl(valueLoader, valueData);
      default:
        return new JsValueBase.Impl(valueData);
    }
  }


  /**
   * @return a [probably compound] JsValue corresponding to this variable.
   *         {@code null} if there was an error lazy-loading the value data.
   */
  @Override
  public JsValueBase getValue() {
    return value;
  }

  @Override
  public String getName() {
    return rawName.toString();
  }

  Object getRawNameAsObject() {
    return this.rawName;
  }

  @Override
  public boolean isMutable() {
    return host != null && host.isMutable();
  }

  @Override
  public boolean isReadable() {
    // TODO(apavlov): implement once the readability metadata are available
    return true;
  }

  @Override
  public RelayOk setValue(JsValue newValue, final SetValueCallback userCallback,
      SyncCallback syncCallback) throws UnsupportedOperationException {
    JsValueBase jsValueBase = castValueArgument(newValue);
    String variableName = rawName.toString();
    JsEvaluateContextImpl.CallbackInternal hostCallback =
        new JsEvaluateContextImpl.CallbackInternal() {
      @Override
      public void success(JsValueBase newValue) {
        value = newValue;
        if (userCallback != null) {
          userCallback.success();
        }
      }

      @Override
      public void exception(JsValueBase exception) {
        if (userCallback != null) {
          userCallback.exceptionThrown(exception);
        }
      }

      @Override
      public void failure(Exception cause) {
        if (userCallback != null) {
          userCallback.failure(cause);
        }
      }
    };
    return host.setValue(variableName, jsValueBase, hostCallback, syncCallback);
  }

  private static JsValueBase castValueArgument(JsValue value) {
    try {
      return (JsValueBase) value;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Incorrect value argument", e);
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
   * A host of variable. It's responsible for changing variable value.
   */
  static abstract class Host {
    private final InternalContext internalContext;

    protected Host(InternalContext internalContext) {
      this.internalContext = internalContext;
    }

    InternalContext getInternalContext() {
      return internalContext;
    }

    abstract boolean isMutable();

    abstract RelayOk setValue(String variableName, JsValueBase jsValueBase,
        JsEvaluateContextImpl.CallbackInternal callback, SyncCallback syncCallback);
  }

  /**
   * A non-abstract class that implements JsVariable.
   */
  public static class Impl extends JsVariableBase {
    public Impl(Host host, ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
      super(host, valueLoader, valueData, rawName);
    }

    @Override public JsObjectProperty asObjectProperty() {
      return null;
    }
  }

  /**
   * An extension to {@link JsVariableBase} that additional provides {@link JsObjectProperty}
   * interface.
   * TODO: properly support getters, setters etc. once supported by protocol.
   */
  static class Property extends JsVariableBase implements JsObjectProperty {
    public Property(Host host, ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
      super(host, valueLoader, valueData, rawName);
    }
    @Override public JsObjectProperty asObjectProperty() {
      return this;
    }
    @Override public boolean isWritable() {
      return true;
    }
    @Override public JsValue getGetter() {
      return null;
    }
    @Override public JsFunction getGetterAsFunction() {
      return null;
    }
    @Override public JsValue getSetter() {
      return null;
    }
    @Override public boolean isConfigurable() {
      return true;
    }
    @Override public boolean isEnumerable() {
      return true;
    }
    @Override public RelayOk evaluateGet(EvaluateCallback callback, SyncCallback syncCallback) {
      throw new RuntimeException();
    }
  }
}
