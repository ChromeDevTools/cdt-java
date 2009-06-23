// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the JsObject interface.
 */
public class JsObjectImpl extends JsValueImpl implements JsObject {

  private static final JsVariableImpl[] EMPTY_VARIABLES = new JsVariableImpl[0];

  protected JsVariableImpl[] properties;

  protected boolean failedResponse;

  private final JsStackFrameImpl stackFrame;

  private final String parentFqn;

  private Map<String, JsVariableImpl> propertyMap;

  /**
   * This constructor implies the lazy resolution of object properties.
   *
   * @param stackFrame
   * @param parentFqn
   * @param valueState
   */
  public JsObjectImpl(JsStackFrameImpl stackFrame, String parentFqn, ValueMirror valueState) {
    super(valueState);
    this.stackFrame = stackFrame;
    this.parentFqn = parentFqn;
    trySetMirrorProperties();
  }

  public JsObjectImpl(ValueMirror valueState, JsVariableImpl[] properties) {
    super(valueState);
    this.properties = properties;
    createPropertyMap();
    this.stackFrame = null;
    this.parentFqn = "";
    trySetMirrorProperties();
  }

  private void trySetMirrorProperties() {
    ValueMirror mirror = getMirror();
    if (mirror.getProperties() == null) {
      // "this" is an object with PropertyReferences. Resolve them.
      final Long ref = Long.valueOf(mirror.getRef());
      final JSONObject handle = stackFrame.getDebugContext().getHandleManager().getHandle(ref);
      if (handle != null) {
        mirror.setProperties(JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME),
            DebugContextImpl.extractObjectProperties(handle));
      }
    }
  }

  private synchronized void createPropertyMap() {
    if (properties == null) {
      propertyMap = Collections.emptyMap();
      return;
    }
    Map<String, JsVariableImpl> map =
        new HashMap<String, JsVariableImpl>(properties.length * 2, 0.75f);
    for (JsVariableImpl prop : properties) {
      map.put(prop.getName(), prop);
    }
    propertyMap = Collections.unmodifiableMap(map);
  }

  @Override
  public JsVariableImpl[] getProperties() {
    ensureProperties();
    return (properties != null && !failedResponse) ? properties : EMPTY_VARIABLES;
  }

  protected synchronized void ensureProperties() {
    if (properties != null) {
      return;
    }
    DebugContextImpl debugContext = stackFrame.getDebugContext();
    final HandleManager handleManager = debugContext.getHandleManager();
    // Use linked map to preserve the original (somewhat alphabetical)
    // properties order
    final Map<JsVariableImpl, Long> variableToRef = new LinkedHashMap<JsVariableImpl, Long>();
    ValueMirror mirror = getMirror();
    PropertyReference[] mirrorProperties = mirror.getProperties();
    if (mirrorProperties == null) {
      // "this" is an object with PropertyReferences. Resolve them.
      final Long ref = Long.valueOf(mirror.getRef());
      Exception ex = null;
      final JSONObject[] handle = new JSONObject[1];
      handle[0] = handleManager.getHandle(ref);
      if (handle[0] == null) {
        ex = debugContext.getV8Handler().sendV8CommandBlocking(
            DebuggerMessageFactory.lookup(
                Collections.singletonList(ref)),
                false, new BrowserTabImpl.V8HandlerCallback() {
                    public void messageReceived(JSONObject response) {
                      if (!JsonUtil.isSuccessful(response)) {
                        JsObjectImpl.this.failedResponse = true;
                        return;
                      }
                      JSONObject body = JsonUtil.getBody(response);
                      handle[0] = JsonUtil.getAsJSON(body, String.valueOf(ref));
                      if (handle != null) {
                        handleManager.put(ref, handle[0]);
                      }
                    }

                    public void failure(String message) {
                      JsObjectImpl.this.failedResponse = true;
                    }
            });
      }
      if (ex != null || this.failedResponse) {
        return;
      } else {
        mirror.setProperties(JsonUtil.getAsString(handle[0], V8Protocol.REF_CLASSNAME),
            DebugContextImpl.extractObjectProperties(handle[0]));
        mirrorProperties = mirror.getProperties();
      }
    }
    final Collection<Long> handlesToRequest = new HashSet<Long>(mirrorProperties.length);
    prepareLookupData(handleManager, mirrorProperties, variableToRef, handlesToRequest);

    properties =
        variableToRef.keySet().toArray(new JsVariableImpl[variableToRef.values().size()]);

    fillPropertiesFromLookup(handleManager, variableToRef, handlesToRequest);
    createPropertyMap();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("[JsObject: type=").append(getReferenceType());
    for (JsVariable prop : getProperties()) {
      result.append(',').append(prop);
    }
    result.append(']');
    return result.toString();
  }

  @Override
  public JsObjectImpl asObject() {
    return this;
  }

  @Override
  public JsArrayImpl asArray() {
    return null;
  }

  @Override
  public JsVariable getProperty(String name) {
    return propertyMap.get(name);
  }

  @Override
  public String getClassName() {
    return getMirror().getClassName();
  }

  private void fillPropertiesFromLookup(final HandleManager handleManager,
      final Map<JsVariableImpl, Long> variableToRef, final Collection<Long> handlesToRequest) {
    if (handlesToRequest.isEmpty()) {
      // All handles are known, nothing to look up
      return;
    }
    DebuggerMessage message = DebuggerMessageFactory.lookup(
        new ArrayList<Long>(handlesToRequest));
    Exception ex = stackFrame.getDebugContext().getV8Handler().sendV8CommandBlocking(
        message,
        false,
        new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (!JsVariableImpl.fillVariablesFromLookupReply(
                    handleManager, properties, variableToRef, response)) {
              JsObjectImpl.this.failedResponse = true;
            }
          }

          public void failure(String message) {
            JsObjectImpl.this.failedResponse = true;
          }
        });
    if (ex != null) {
      this.failedResponse = true;
    }
  }

  private void prepareLookupData(final HandleManager handleManager,
      PropertyReference[] mirrorProperties, final Map<JsVariableImpl, Long> variableToRef,
      final Collection<Long> handlesToRequest) {
    for (PropertyReference prop : mirrorProperties) {
      String propName = prop.getName();
      if (propName.isEmpty()) {
        // Do not provide a synthetic "hidden properties" property.
        continue;
      }
      ValueMirror mirror = new ValueMirror(propName, prop.getRef());
      String fqn;

      if (JsonUtil.isInteger(propName)) {
        fqn = parentFqn + '[' + propName + ']';
      } else {
        if (propName.startsWith(".")) {
          // ".arguments" is not legal
          continue;
        }
        fqn = parentFqn + '.' + propName;
      }

      JsVariableImpl variable = new JsVariableImpl(stackFrame, mirror, fqn, true);
      Long ref = Long.valueOf(prop.getRef());
      JSONObject handle = handleManager.getHandle(ref);
      if (handle != null) {
        // cache hit
        JsVariableImpl.fillVariable(variable, handle);
      } else {
        handlesToRequest.add(ref);
      }
      variableToRef.put(variable, ref);
    }
  }
}
