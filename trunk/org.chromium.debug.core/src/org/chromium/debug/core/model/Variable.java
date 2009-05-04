// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.BlockingV8RequestCommand;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler.MessageReplyCallback;
import org.chromium.debug.core.tools.v8.model.mirror.Execution;
import org.chromium.debug.core.tools.v8.model.mirror.HandleManager;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror.PropertyReference;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror.Type;
import org.chromium.debug.core.tools.v8.request.V8Request;
import org.chromium.debug.core.util.JsonUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents a variable in a Chromium V8 VM stack frame.
 */
public class Variable extends DebugElementImpl implements IVariable, IAdaptable {

  private static final String DOT = "."; //$NON-NLS-1$

  private static final String COMMA = ","; //$NON-NLS-1$

  private static final String OPEN_BRACKET = "["; //$NON-NLS-1$

  private static final String CLOSE_BRACKET = "]"; //$NON-NLS-1$

  private final ValueMirror local;

  private final StackFrame stackFrame;

  private Value value;

  private String evalExpr;

  private boolean pendingReq = false;

  // sentry to stop drilling in for props of type object.
  // We'll wait until user clicks on that prop in variable view.
  private boolean waitDrilling = false;

  /**
   * Constructs a variable contained in the given stack frame with the given
   * name.
   *
   * @param frame owning stack frame
   * @param name variable name
   */
  public Variable(StackFrame frame, ValueMirror mirror) {
    super(frame.getHandler());
    this.stackFrame = frame;
    this.local = mirror;
  }

  /**
   * Constructs a variable contained in the given stack frame with the given name.
   *
   * @param frame owning stack frame
   * @param mirror the V8 ValueMirror for this variable
   * @param fullQual fully qualified expression of this variable
   * @param stop whether to halt drilling in for any properties of type object
   */
  public Variable(StackFrame frame, ValueMirror mirror, String fullQual, boolean stop) {
    super(frame.getHandler());
    this.stackFrame = frame;
    this.local = mirror;
    this.evalExpr = fullQual;
    this.waitDrilling = stop;
  }

  public synchronized Value getValue() throws DebugException {
    if (value == null) {
      if (isObjectType()) {
        PropertyReference[] properties = local.getProperties();
        if (properties != null) {
          // This object may not be known completely yet...
          ensureProperties(properties);
        } else {
          this.value = new Value(getHandler(), this.local);
        }
      } else {
        this.value = new Value(getHandler(), this.local);
      }
    } else {
      if (isObjectType()) {
        StringBuilder evalExpr = null;

        List<Variable> sendVars = new ArrayList<Variable>();
        Variable[] vars = value.getVariables();
        if (vars != null) {
          for (Variable v : vars) {
            if (v.getValue() != null && v.local.getType() == Type.JS_OBJECT) {
              Variable[] drillInVars = v.getValue().getVariables();
              for (Variable drillV : drillInVars) {
                if (drillV.getValue() == null
                    && drillV.local.getType() == Type.JS_OBJECT
                    && !drillV.isPendingReq()) {
                  if (evalExpr == null) {
                    evalExpr = new StringBuilder(OPEN_BRACKET);
                  }
                  evalExpr.append(drillV.getFullQualExpression() + COMMA);
                  sendVars.add(drillV);
                }
              }
            }
          }
        }

        if (!sendVars.isEmpty()) {
          evalExpr.append(CLOSE_BRACKET);

          final Variable[] newVars = sendVars.toArray(new Variable[sendVars.size()]);
          V8Request command =
              V8Request.evaluate(evalExpr.toString(), stackFrame.getIdentifier(), null, null);
          this.setPendingReq();
          BlockingV8RequestCommand runner =
              new BlockingV8RequestCommand(getHandler(), command,
                  new MessageReplyCallback() {
                    public void replyReceived(JSONObject reply) {
                      updateVariablesFromEvalReply(newVars, reply);
                    }
                  });
          runner.run();
          if (runner.getException() != null) {
            throw ChromiumDebugPlugin.newDebugException(runner.getException());
          }
        }
      }
    }

    return value;
  }

  public int getRef() {
    return local.getRef();
  }

  public String getName() throws DebugException {
    String name = local.getName();
    if (JsonUtil.isInteger(name)) {
      // Fix array element indices
      name = OPEN_BRACKET + name + CLOSE_BRACKET;
    }
    return name;
  }

  public String getReferenceTypeName() throws DebugException {
    return local.getTypeAsString();
  }

  public boolean hasValueChanged() throws DebugException {
    return false; // we do not track values between suspended states
  }

  public synchronized void setValue(String newValue) throws DebugException {
    // TODO(apavlov): currently V8 does not support it
    String expression = getName() + "=\"" + newValue + "\""; //$NON-NLS-1$ //$NON-NLS-2$

    try {
      getHandler().sendV8Command(
          V8Request.evaluate(expression, stackFrame.getIdentifier(), null, null).getMessage(),
          null);
    } catch (IOException e) {
      throw ChromiumDebugPlugin.newDebugException(e);
    }

    local.setValue(newValue);
    getDebugTarget().fireEvent(new DebugEvent(value, DebugEvent.CHANGE));
  }

  public synchronized void setValue(IValue value) throws DebugException {
    this.value = (Value) value;
  }

  public boolean supportsValueModification() {
    return false; // TODO(apavlov): fix once V8 supports it
  }

  public boolean verifyValue(String expression) throws DebugException {
    switch (local.getType()) {
      case JS_NUMBER:
        return JsonUtil.isInteger(expression);
      default:
        return true;
    }
  }

  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  /**
   * Returns the stack frame owning this variable.
   */
  protected StackFrame getStackFrame() {
    return stackFrame;
  }

  // Used for object properties filling
  public void setTypeValue(Type type, String val) {
    local.setType(type);

    switch (local.getType()) {
      case JS_NUMBER:
      case JS_STRING:
      case JS_BOOLEAN:
      case JS_UNDEFINED:
      case JS_NULL:
      case JS_DATE:
        local.setValue(val);
        try {
          setValue(new Value(getHandler(), local));
        } catch (DebugException e) {
          ChromiumDebugPlugin.log(e);
        }
        break;
      case JS_OBJECT:
        // You cannot set an object value
        break;
    }
  }

  // Used for object prop filling
  public synchronized void setProperties(PropertyReference[] props) {
    if (props != null) {
      try {
        ensureProperties(props);
        local.setProperties(props);
      } catch (DebugException e) {
        ChromiumDebugPlugin.log(e);
      }
    }
  }

  public String getFullQualExpression() {
    try {
      return evalExpr != null ? evalExpr : getName();
    } catch (DebugException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  public boolean isWaitDrilling() {
    return waitDrilling;
  }

  public void resetDrilling() {
    waitDrilling = false;
  }

  public void setPendingReq() {
    pendingReq = true;
  }

  public boolean isPendingReq() {
    return pendingReq;
  }

  public void resetPending() {
    pendingReq = false;
  }

  private void ensureProperties(PropertyReference[] properties) throws DebugException {
    HandleManager handleManager = getHandleManager();
    // Use linked map to preserve the original (somewhat alphabetical) properties order
    final Map<Long, Variable> refToVariable = new LinkedHashMap<Long, Variable>();

    for (PropertyReference prop : properties) {
      String propName = prop.getName();
      if (propName.isEmpty()) {
        // Do not provide a synthetic "hidden properties" property.
        continue;
      }
      ValueMirror valueMirror = new ValueMirror(propName, prop.getRef(), local.getRef());
      String fullQual;

      try {
        Integer.parseInt(propName);
        fullQual = getFullQualExpression() + OPEN_BRACKET + propName + CLOSE_BRACKET;
      } catch (NumberFormatException nfe) {
        if (propName.startsWith(DOT)) {
          // ".arguments" is not legal
          continue;
        }
        fullQual = getFullQualExpression() + DOT + propName;
      }

      Variable variable = new Variable(stackFrame, valueMirror, fullQual, false);
      JSONObject handle = handleManager.getHandle(Long.valueOf(prop.getRef()));
      if (handle != null) {
        // do not re-request from V8
        fillVariable(variable, handle);
      }
      refToVariable.put(Long.valueOf(prop.getRef()), variable);
    }

    final Variable[] vars = refToVariable.values().toArray(
        new Variable[refToVariable.values().size()]);

    this.value = new Value(getHandler(), this.local, vars);
    V8Request command = V8Request.lookup(new ArrayList<Long>(refToVariable.keySet()));
    BlockingV8RequestCommand runner =
        new BlockingV8RequestCommand(getHandler(), command,
            new MessageReplyCallback() {
              @Override
              public void replyReceived(JSONObject reply) {
                updateVariablesFromLookupReply(refToVariable, reply);
              }
            });
    runner.run();
    if (runner.getException() != null) {
      ChromiumDebugPlugin.log(runner.getException());
    }
  }

  private HandleManager getHandleManager() {
    return getStackFrame().getHandler().getExecution().getHandleManager();
  }

  private void updateVariablesFromLookupReply(Map<Long, Variable> refToVariable, JSONObject reply) {
    boolean success = JsonUtil.getAsBoolean(reply, Protocol.KEY_SUCCESS);
    if (!success) {
      ChromiumDebugPlugin.logWarning(reply.toString());
      return;
    }
    JSONObject body = JsonUtil.getAsJSON(reply, Protocol.FRAME_BODY);
    for (Map.Entry<Long, Variable> entry : refToVariable.entrySet()) {
      fillVariable(entry.getValue(), JsonUtil.getAsJSON(body, String.valueOf(entry.getKey())));
    }
  }

  private void updateVariablesFromEvalReply(Variable[] vars, JSONObject reply) {
    boolean success = JsonUtil.getAsBoolean(reply, Protocol.KEY_SUCCESS);
    if (!success) {
      ChromiumDebugPlugin.logWarning(reply.toString());
      return;
    }
    JSONObject body = JsonUtil.getAsJSON(reply, Protocol.FRAME_BODY);
    String className = JsonUtil.getAsString(body, Protocol.REF_CLASSNAME);
    JSONArray props = JsonUtil.getAsJSONArray(body, Protocol.REF_PROPERTIES);
    JSONArray refs = JsonUtil.getAsJSONArray(reply, Protocol.FRAME_REFS);

    if (props != null && Protocol.CLASSNAME_ARRAY.equals(className)) {
      // Find all the handles we might need.
      Map<Long, JSONObject> refToHandle = Protocol.getRefHandleMap(refs);

      int idx = 1; // Skip first entry (it's the length prop)
      for (Variable var : vars) {
        JSONObject entry = (JSONObject) props.get(idx++);
        Long ref = JsonUtil.getAsLong(entry, Protocol.REF_PROP_REF);
        JSONObject handleObject = refToHandle.get(ref);
        fillVariable(var, handleObject);
      }
    }
  }

  private void fillVariable(Variable var, JSONObject handleObject) {
    if (handleObject == null) {
      // it has not been requested and var data are already known
      return;
    }
    String typeString = null;
    String valueString = null;
    if (handleObject != null) {
      typeString = JsonUtil.getAsString(handleObject, Protocol.REF_TYPE);
      valueString = JsonUtil.getAsString(handleObject, Protocol.REF_TEXT);
    }
    if (typeString != null) {
      Type type = Type.fromJsonTypeAndClassName(typeString,
          JsonUtil.getAsString(handleObject, Protocol.REF_CLASSNAME));
      if (isObjectType(type)) {
        if (!var.isPendingReq()) {
          PropertyReference[] propertyRefs = Execution.extractObjectProperties(handleObject);
          if (!var.isWaitDrilling()) {
            var.setProperties(propertyRefs);
          }
          var.resetDrilling();
        }
      } else if (valueString != null) {
        var.setTypeValue(type, valueString);
        var.resetPending();
      } else {
        ChromiumDebugPlugin.logWarning(
            Messages.Variable_NotScalarOrObjectFormat, type.jsonType);
      }
    } else {
      ChromiumDebugPlugin.logWarning(Messages.Variable_NullTypeForAVariable);
    }
  }

  private boolean isObjectType() {
    return isObjectType(local.getType());
  }

  private static boolean isObjectType(Type type) {
    return type == Type.JS_OBJECT || type == Type.JS_ARRAY;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object getAdapter(Class adapter) {
    if (IWatchExpressionFactoryAdapter.class == adapter) {
      return new IWatchExpressionFactoryAdapter() {
        public String createWatchExpression(IVariable variable) throws CoreException {
          String expression = ((Variable) variable).getFullQualExpression();
          if (expression == null) {
            expression = variable.getName();
          }
          return expression;
        }
      };
    }
    return super.getAdapter(adapter);
  }


}
