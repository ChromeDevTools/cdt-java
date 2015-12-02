// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;


import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.JsDeclarativeVariable;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObjectProperty;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.V8CommandCallbackBase;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.v8native.protocol.input.FailedCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.SetVariableValueBody;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.ScopeMessage;
import org.chromium.sdk.internal.v8native.protocol.output.SetVariableValueMessage;
import org.chromium.sdk.util.GenericCallback;

/**
 * A generic implementation of the JsVariable interface.
 */
public abstract class JsVariableBase implements JsVariable {
  /** The value of this variable. */
  private volatile JsValueBase value;

  /** Variable name. */
  private final Object rawName;

  /**
   * Constructs a variable contained in the given context with the given value mirror.
   * @param valueLoader that owns this variable
   * @param valueData for this variable
   */
  public JsVariableBase(ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
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
   * A non-abstract class that implements JsVariable.
   */
  public static class Impl extends JsVariableBase {
    public Impl(ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
      super(valueLoader, valueData, rawName);
    }
    @Override public JsObjectProperty asObjectProperty() {
      return null;
    }
    @Override public JsDeclarativeVariable asDeclarativeVariable() {
      return null;
    }
  }

  public static class Declarative extends JsVariableBase implements JsDeclarativeVariable {
    private final VariableChanger changer;

    public Declarative(ValueLoader valueLoader, ValueMirror valueData, Object rawName,
        VariableChanger changer) {
      super(valueLoader, valueData, rawName);
      this.changer = changer;
    }

    @Override public JsObjectProperty asObjectProperty() {
      return null;
    }
    @Override public JsDeclarativeVariable asDeclarativeVariable() {
      return this;
    }

    @Override public boolean isMutable() {
      return changer != null;
    }

    @Override
    public RelayOk setValue(JsValue newValue, final SetValueCallback userCallback,
        SyncCallback syncCallback) throws UnsupportedOperationException {
      JsValueBase jsValueBase = castValueArgument(newValue);
      String variableName = getName();
      GenericCallback<JsValueBase> hostCallback =
          new GenericCallback<JsValueBase>() {
        @Override
        public void success(JsValueBase newValue) {
          JsVariableBase baseThis = Declarative.this;
          // Access to private field of base class.
          baseThis.value = newValue;
          if (userCallback != null) {
            userCallback.success();
          }
        }

        @Override
        public void failure(Exception cause) {
          if (userCallback != null) {
            userCallback.failure(cause);
          }
        }
      };
      return changer.setValue(variableName, jsValueBase, hostCallback, syncCallback);
    }
  }

  /**
   * Responsible for changing declarative variable value. Contains all necessary data.
   */
  public static class VariableChanger {
    private final InternalContext internalContext;
    private final ScopeMessage.Ref scopeRef;

    VariableChanger(InternalContext internalContext, ScopeMessage.Ref scopeRef) {
      this.internalContext = internalContext;
      this.scopeRef = scopeRef;
    }

    RelayOk setValue(String variableName, JsValueBase jsValueBase,
        final GenericCallback<JsValueBase> callback, SyncCallback syncCallback) {
      // TODO: check for host.
      SetVariableValueMessage message = new SetVariableValueMessage(scopeRef, variableName,
          jsValueBase.getJsonParam(internalContext));

      V8CommandCallbackBase innerCallback = new V8CommandCallbackBase() {
        @Override
        public void success(SuccessCommandResponse successResponse) {
          SetVariableValueBody body;
          try {
            body = successResponse.body().asSetVariableValueBody();
          } catch (JsonProtocolParseException e) {
            throw new RuntimeException(e);
          }
          ValueHandle newValueHandle = body.newValue();
          ValueLoaderImpl valueLoader = internalContext.getValueLoader();
          ValueMirror mirror = valueLoader.addDataToMap(newValueHandle);
          JsValueBase value = JsVariableBase.createValue(valueLoader, mirror);
          callback.success(value);
        }

        @Override
        public void failure(String message, FailedCommandResponse.ErrorDetails errorDetails) {
          callback.failure(new Exception(message));
        }
      };
      try {
        return internalContext.sendV8CommandAsync(message, true,
            innerCallback, syncCallback);
      } catch (ContextDismissedCheckedException e) {
        return internalContext.getDebugSession().maybeRethrowContextException(e,
            syncCallback);
      }
    }
  }

  /**
   * An extension to {@link JsVariableBase} that additional provides {@link JsObjectProperty}
   * interface.
   * TODO: properly support getters, setters etc. once supported by protocol.
   */
  static class Property extends JsVariableBase implements JsObjectProperty {
    public Property(ValueLoader valueLoader, ValueMirror valueData, Object rawName) {
      super(valueLoader, valueData, rawName);
    }
    @Override public JsObjectProperty asObjectProperty() {
      return this;
    }
    @Override public JsDeclarativeVariable asDeclarativeVariable() {
      return null;
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
