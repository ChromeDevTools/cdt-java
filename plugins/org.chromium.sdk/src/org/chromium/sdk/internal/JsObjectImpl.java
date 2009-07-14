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
import org.chromium.sdk.internal.DebugContextImpl.SendingType;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the JsObject interface.
 */
public class JsObjectImpl extends JsValueImpl implements JsObject {

  private static final JsVariableImpl[] EMPTY_VARIABLES = new JsVariableImpl[0];

  protected JsVariableImpl[] properties;

  protected volatile boolean failedResponse;

  private final JsStackFrameImpl stackFrame;

  private final String parentFqn;

  private Map<String, JsVariableImpl> propertyMap;

  /**
   * A lock for the properties and propertyMap fields access/modification.
   */
  private final Object propertyLock = new Object();

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

  public JsObjectImpl(
      JsStackFrameImpl stackFrame, ValueMirror valueState, JsVariableImpl[] properties) {
    super(valueState);
    this.stackFrame = stackFrame;
    this.properties = properties;
    ensurePropertyMap();
    this.parentFqn = "";
    trySetMirrorProperties();
  }

  private void trySetMirrorProperties() {
    ValueMirror mirror = getMirror();
    if (mirror.getProperties() == null && isTokenValid()) {
      // "this" is an object with PropertyReferences. Resolve them.
      final Long ref = Long.valueOf(mirror.getRef());
      final JSONObject handle = stackFrame.getDebugContext().getHandleManager().getHandle(ref);
      if (handle != null) {
        mirror.setProperties(JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME),
            V8ProtocolUtil.extractObjectProperties(handle));
      }
    }
  }

  /**
   * @return whether the context token is valid
   */
  private boolean isTokenValid() {
    return stackFrame != null && stackFrame.getToken().isValid();
  }

  /**
   * Calls to this method must be synchronized on propertyLock.
   */
  private void ensurePropertyMap() {
    if (propertyMap != null) {
      return;
    }
    if (properties == null || properties.length == 0) {
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

  public JsVariableImpl[] getProperties() {
    ensureProperties();
    synchronized (propertyLock) {
      return (properties != null && !isFailedResponse()) ? properties : EMPTY_VARIABLES;
    }
  }

  protected void ensureProperties() {
    synchronized (propertyLock) {
      if (properties != null) {
        return;
      }

      final ContextToken token = stackFrame.getToken();
      if (!token.isValid()) {
        setFailedResponse();
        properties = new JsVariableImpl[0];
        return;
      }
      DebugContextImpl debugContext = stackFrame.getDebugContext();
      final HandleManager handleManager = debugContext.getHandleManager();
      ValueMirror mirror = getMirror();
      PropertyReference[] mirrorProperties = mirror.getProperties();
      try {
        if (mirrorProperties == null) {
          // "this" is an object with PropertyReferences. Resolve them.
          final Long ref = Long.valueOf(mirror.getRef());
          Exception ex = null;
          final JSONObject[] handle = new JSONObject[1];
          handle[0] = handleManager.getHandle(ref);
          if (handle[0] == null) {
            ex = resolveThisHandle(debugContext, handleManager, ref, handle);
          }
          if (ex != null || isFailedResponse()) {
            properties = new JsVariableImpl[0];
            return;
          } else {
            mirrorProperties = V8ProtocolUtil.extractObjectProperties(handle[0]);
            mirror.setProperties(JsonUtil.getAsString(handle[0], V8Protocol.REF_CLASSNAME),
                mirrorProperties);
          }
        }
        fillPropertiesFromMirror(handleManager, mirrorProperties);
      } finally {
        ensurePropertyMap();
      }
    }
  }

  /**
   * Calls to this method must be synchronized on propertyLock.
   */
  private void fillPropertiesFromMirror(
      final HandleManager handleManager, PropertyReference[] mirrorProperties) {
    if (!isTokenValid()) {
      properties = new JsVariableImpl[0];
      return;
    }
    properties = new JsVariableImpl[mirrorProperties.length];
    for (int i = 0, size = properties.length; i < size; i++) {
      PropertyReference ref = mirrorProperties[i];
      String propName = ref.getName();
      String fqn = getFullyQualifiedName(propName);
      if (fqn == null) {
        continue;
      }
      JSONObject handleObject = handleManager.getHandle(Long.valueOf(ref.getRef()));
      if (handleObject == null) {
        handleObject = ref.getValueObject();
      }
      if (handleObject == null) {
        processOriginalFormatPropertyRefs(handleManager, mirrorProperties);
        return;
      }
      properties[i] = new JsVariableImpl(
          stackFrame, V8Helper.createValueMirror(handleObject, ref.getName()),
          fqn, true);
    }
  }

  private Exception resolveThisHandle(final DebugContextImpl debugContext,
      final HandleManager handleManager, final Long ref, final JSONObject[] targetHandle) {
    Exception ex = debugContext.sendMessage(
        SendingType.SYNC,
        DebuggerMessageFactory.lookup(
            Collections.singletonList(ref), true, stackFrame.getToken()),
        new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (!JsonUtil.isSuccessful(response)) {
              setFailedResponse();
              return;
            }
            JSONObject body = JsonUtil.getBody(response);
            targetHandle[0] = JsonUtil.getAsJSON(body, String.valueOf(ref));
            if (targetHandle[0] != null) {
              handleManager.put(ref, targetHandle[0]);
            }
            handleManager.putAll(V8ProtocolUtil.getRefHandleMap(
                JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS)));
          }

          public void failure(String message) {
            setFailedResponse();
          }
        });
    return ex;
  }

  /**
   * The method must be called only when the token is valid.
   */
  private void processOriginalFormatPropertyRefs(
      HandleManager handleManager, PropertyReference[] mirrorProperties) {
    Map<JsVariableImpl, Long> variableToRefMap = new LinkedHashMap<JsVariableImpl, Long>();
    Collection<Long> handlesToRequest = new HashSet<Long>(mirrorProperties.length);
    prepareLookupData(handleManager, mirrorProperties, variableToRefMap, handlesToRequest);

    properties = variableToRefMap.keySet().toArray(
        new JsVariableImpl[variableToRefMap.values().size()]);
    fillPropertiesFromLookup(handleManager, variableToRefMap, handlesToRequest);
  }

  private String getFullyQualifiedName(String propName) {
    String fqn;
    if (JsonUtil.isInteger(propName)) {
      fqn = parentFqn + '[' + propName + ']';
    } else {
      if (propName.startsWith(".")) {
        // ".arguments" is not legal
        fqn = null;
      }
      fqn = parentFqn + '.' + propName;
    }
    return fqn;
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

  public JsArrayImpl asArray() {
    return null;
  }

  public JsVariable getProperty(String name) {
    return propertyMap.get(name);
  }

  public String getClassName() {
    return getMirror().getClassName();
  }

  /**
   * The method must be called only when the token is valid.
   */
  private void fillPropertiesFromLookup(final HandleManager handleManager,
      final Map<JsVariableImpl, Long> variableToRef, final Collection<Long> handlesToRequest) {
    if (handlesToRequest.isEmpty()) {
      // All handles are known, nothing to look up
      return;
    }
    DebuggerMessage message = DebuggerMessageFactory.lookup(
        new ArrayList<Long>(handlesToRequest), true, stackFrame.getToken());
    Exception ex = stackFrame.getDebugContext().sendMessage(
        SendingType.SYNC,
        message,
        new BrowserTabImpl.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (!JsVariableImpl.fillVariablesFromLookupReply(
                    handleManager, properties, variableToRef, response)) {
              setFailedResponse();
            }
          }

          public void failure(String message) {
            setFailedResponse();
          }
        });
    if (ex != null) {
      setFailedResponse();
    }
  }

  protected void setFailedResponse() {
    this.failedResponse = true;
  }

  protected boolean isFailedResponse() {
    return this.failedResponse;
  }

  /**
   * The method must be called only when the token is valid.
   */
  private void prepareLookupData(final HandleManager handleManager,
      PropertyReference[] mirrorProperties, final Map<JsVariableImpl, Long> variableToRef,
      final Collection<Long> handlesToRequest) {
    for (PropertyReference prop : mirrorProperties) {
      String propName = prop.getName();
      if (propName.length() == 0) {
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
