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

  private static final String DOT = ".";

  private static final String OPEN_BRACKET = "[";

  private static final String CLOSE_BRACKET = "]";

  /**
   * The variable value data as reported by the JavaScript VM (is used to
   * construct the variable value.)
   */
  private final ValueMirror valueData;

  /** The call frame this variable belongs in. */
  private final CallFrameImpl callFrame;

  /** The fully qualified name of this variable. */
  private final String variableFqn;

  /** The lazily constructed value of this variable. */
  private JsValueImpl value;

  /** Variable name. */
  private final String varName;

  // The access is synchronized
  private boolean pendingReq = false;

  // Sentry to stop drilling in for props of type object.
  private boolean waitDrilling;

  protected volatile boolean failedResponse = false;

  /**
   * Constructs a variable contained in the given call frame with the given
   * value mirror.
   *
   * @param callFrame that owns this variable
   * @param valueData value data for this variable
   */
  JsVariableImpl(CallFrameImpl callFrame, ValueMirror valueData, String varName) {
    this(callFrame, valueData, varName, null, false);
  }

  /**
   * Constructs a variable contained in the given call frame with the given
   * value mirror.
   *
   * @param callFrame that owns this variable
   * @param valueData for this variable
   * @param variableFqn the fully qualified name of this variable
   * @param waitDrilling whether to halt drilling in for any properties of type "object"
   */
  JsVariableImpl(CallFrameImpl callFrame, ValueMirror valueData, String varName, String variableFqn,
      boolean waitDrilling) {
    this.callFrame = callFrame;
    this.valueData = valueData;
    this.varName = varName;
    this.variableFqn = variableFqn;
    this.waitDrilling = waitDrilling;
  }

  /**
   * @return a [probably compound] JsValue corresponding to this variable.
   *         {@code null} if there was an error lazy-loading the value data.
   */
  public synchronized JsValueImpl getValue() {
    if (isFailedResponse()) {
      return null;
    }
    if (value == null) {
      Type type = this.valueData.getType();
      switch (type) {
        case TYPE_OBJECT:
          this.value = new JsObjectImpl(callFrame, getFullyQualifiedName(), this.valueData);
          break;
        case TYPE_ARRAY:
          this.value = new JsArrayImpl(callFrame, getFullyQualifiedName(), this.valueData);
          break;
        default:
          this.value = new JsValueImpl(this.valueData);
      }
    }
    return value;
  }

  public String getName() {
    String name = varName;
    if (JsonUtil.isInteger(name)) {
      // Fix array element indices
      name = OPEN_BRACKET + name + CLOSE_BRACKET;
    }
    return name;
  }

  public String getReferenceTypeName() {
    return valueData.getTypeAsString();
  }

  public boolean hasValueChanged() {
    return false; // we do not track values between suspended states
  }

  public synchronized void setValue(JsValueImpl value) {
    this.value = value;
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

  public Type getType() {
    return valueData.getType();
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("[JsVariable: name=")
        .append(getName())
        .append(",type=")
        .append(getType())
        .append(",value=")
        .append(getValue())
        .append(']')
        .toString();
  }

  /**
   * Returns the call frame owning this variable.
   */
  protected CallFrameImpl getCallFrame() {
    return callFrame;
  }

  // Used for object properties filling
  public void setTypeValue(Type type, String val) {
    valueData.setType(type);

    Type existingType = valueData.getType();
    if (existingType == null) {
      valueData.setValue(val);
      setValue(new JsValueImpl(valueData));
      return;
    }
    switch (existingType) {
      case TYPE_NUMBER:
      case TYPE_STRING:
      case TYPE_BOOLEAN:
      case TYPE_UNDEFINED:
      case TYPE_NULL:
      case TYPE_DATE:
        valueData.setValue(val);
        setValue(new JsValueImpl(valueData));
        break;
      case TYPE_OBJECT:
        // You cannot set an object value
        break;
    }
  }

  /**
   * Resolves property references and sets the class name of an Object variable.
   *
   * @param className of this object
   * @param properties of the Object variable
   */
  public synchronized void setProperties(String className, PropertyReference[] properties) {
    if (properties != null) {
      valueData.setProperties(className, properties);
    }
  }

  public ValueMirror getMirror() {
    return valueData;
  }

  public String getFullyQualifiedName() {
    return variableFqn != null
        ? variableFqn
        : getName();
  }

  synchronized boolean isWaitDrilling() {
    return waitDrilling;
  }

  synchronized void resetDrilling() {
    waitDrilling = false;
  }

  synchronized void setPendingReq() {
    pendingReq = true;
  }

  synchronized boolean isPendingReq() {
    return pendingReq;
  }

  synchronized void resetPending() {
    pendingReq = false;
  }

  protected void setFailedResponse() {
    this.failedResponse = true;
  }

  protected boolean isFailedResponse() {
    return failedResponse;
  }
}
