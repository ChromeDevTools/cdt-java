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
import java.util.List;
import java.util.Map;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the JsObject interface.
 */
public class JsObjectImpl extends JsValueImpl implements JsObject {

  protected Collection<JsVariableImpl> properties;

  protected volatile boolean failedResponse;

  private final CallFrameImpl callFrame;

  private final String parentFqn;

  private Map<String, JsVariableImpl> propertyMap;

  /**
   * A lock for the properties and propertyMap fields access/modification.
   */
  private final Object propertyLock = new Object();

  /**
   * This constructor implies the lazy resolution of object properties.
   *
   * @param callFrame where this instance belongs in
   * @param parentFqn the fully qualified name of the object parent
   * @param valueState the value data from the JS VM
   */
  public JsObjectImpl(CallFrameImpl callFrame, String parentFqn, ValueMirror valueState) {
    super(valueState);
    this.callFrame = callFrame;
    this.parentFqn = parentFqn;
    trySetMirrorProperties();
  }

  public JsObjectImpl(
      CallFrameImpl callFrame, ValueMirror valueState, Collection<JsVariableImpl> properties) {
    super(valueState);
    this.callFrame = callFrame;
    this.properties = Collections.unmodifiableCollection(properties);
    ensurePropertyMap();
    this.parentFqn = "";
    trySetMirrorProperties();
  }

  private void trySetMirrorProperties() {
    ValueMirror mirror = getMirror();
    if (mirror.getProperties() == null && isTokenValid()) {
      // "this" is an object with PropertyReferences. Resolve them.
      final Long ref = Long.valueOf(mirror.getRef());
      final JSONObject handle = callFrame.getDebugContext().getHandleManager().getHandle(ref);
      if (handle != null) {
        mirror.setProperties(
            JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME),
            V8ProtocolUtil.extractObjectProperties(handle));
      }
    }
  }

  /**
   * @return whether the context token is valid
   */
  private boolean isTokenValid() {
    return callFrame != null && callFrame.getToken().isValid();
  }

  /**
   * Calls to this method must be synchronized on propertyLock.
   */
  private void ensurePropertyMap() {
    if (propertyMap != null) {
      return;
    }
    if (properties == null || properties.size() == 0) {
      propertyMap = Collections.emptyMap();
      return;
    }
    Map<String, JsVariableImpl> map =
        new HashMap<String, JsVariableImpl>(properties.size() * 2, 0.75f);
    for (JsVariableImpl prop : properties) {
      map.put(prop.getName(), prop);
    }
    propertyMap = Collections.unmodifiableMap(map);
  }

  public Collection<JsVariableImpl> getProperties() {
    ensureProperties();
    synchronized (propertyLock) {
      return (properties != null && !isFailedResponse())
          ? properties
          : Collections.<JsVariableImpl> emptySet();
    }
  }

  protected void ensureProperties() {
    synchronized (propertyLock) {
      if (properties != null) {
        return;
      }

      final ContextToken token = callFrame.getToken();
      if (!token.isValid()) {
        setFailedResponse();
        properties = Collections.<JsVariableImpl> emptySet();
        return;
      }
      InternalContext debugContext = callFrame.getDebugContext();
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
            properties = Collections.<JsVariableImpl> emptySet();
            return;
          } else {
            mirrorProperties = V8ProtocolUtil.extractObjectProperties(handle[0]);
            mirror.setProperties(
                JsonUtil.getAsString(handle[0], V8Protocol.REF_CLASSNAME),
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
  private void fillPropertiesFromMirror(final HandleManager handleManager,
      PropertyReference[] mirrorProperties) {
    if (!isTokenValid()) {
      properties = Collections.<JsVariableImpl> emptySet();
      return;
    }
    List<JsVariableImpl> propertyList = new ArrayList<JsVariableImpl>(mirrorProperties.length);
    for (int i = 0, size = mirrorProperties.length; i < size; i++) {
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
      propertyList.add(
          new JsVariableImpl(
              callFrame,
              V8Helper.createValueMirror(handleObject, ref.getName()),
              fqn,
              true));
    }
    properties = Collections.unmodifiableCollection(propertyList);
  }

  private Exception resolveThisHandle(final InternalContext debugContext,
      final HandleManager handleManager, final Long ref, final JSONObject[] targetHandle) {
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    debugContext.getMessageSender().sendMessageAsync(
        DebuggerMessageFactory.lookup(
            Collections.singletonList(ref), true, callFrame.getToken()),
        true,
        new V8CommandProcessor.V8HandlerCallback() {
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
          handleManager.putAll(V8ProtocolUtil.getRefHandleMap(JsonUtil.getAsJSONArray(
              response, V8Protocol.FRAME_REFS)));
          }

          public void failure(String message) {
            setFailedResponse();
          }
        },
        callbackSemaphore);
    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      return new Exception("Timeout");
    }
    return null;
  }

  /**
   * The method must be called only when the token is valid.
   */
  private void processOriginalFormatPropertyRefs(
      HandleManager handleManager, PropertyReference[] mirrorProperties) {
    Map<JsVariableImpl, Long> variableToRefMap = new LinkedHashMap<JsVariableImpl, Long>();
    Collection<Long> handlesToRequest = new HashSet<Long>(mirrorProperties.length);
    prepareLookupData(handleManager, mirrorProperties, variableToRefMap, handlesToRequest);

    properties = Collections.unmodifiableCollection(variableToRefMap.keySet());
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
    result.append("[JsObject: type=").append(getType());
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
    DebuggerMessage message =
        DebuggerMessageFactory.lookup(
        new ArrayList<Long>(handlesToRequest), true, callFrame.getToken());
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    callFrame.getDebugContext().getMessageSender().sendMessageAsync(
        message, true,
        new V8CommandProcessor.V8HandlerCallback() {
          public void messageReceived(JSONObject response) {
            if (!fillVariablesFromLookupReply(handleManager, properties, variableToRef,
                response)) {
              setFailedResponse();
            }
          }

          public void failure(String message) {
            setFailedResponse();
          }
        },
        callbackSemaphore);

    boolean res = callbackSemaphore.tryAcquireDefault();

    if (!res) {
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

      JsVariableImpl variable = new JsVariableImpl(callFrame, mirror, fqn, true);
      Long ref = Long.valueOf(prop.getRef());
      JSONObject handle = handleManager.getHandle(ref);
      if (handle != null) {
        // cache hit
        fillVariable(variable, handle);
      } else {
        handlesToRequest.add(ref);
      }
      variableToRef.put(variable, ref);
    }
  }

  /**
   * Fills in a variable from a handle object.
   *
   * @param variable to fill in
   * @param handleObject to get the data from
   */
  private static void fillVariable(JsVariableImpl variable, JSONObject handleObject) {
    String typeString = null;
    String valueString = null;
    if (handleObject != null) {
      typeString = JsonUtil.getAsString(handleObject, V8Protocol.REF_TYPE);
      valueString = JsonUtil.getAsString(handleObject, V8Protocol.REF_TEXT);
    }
    if ("error".equals(typeString)) {
      // Report the JS VM error.
      if (valueString == null) {
        valueString = "An error occurred while retrieving the value.";
      }
      variable.setTypeValue(Type.TYPE_STRING, valueString);
      variable.resetPending();
      return;
    }
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(
        typeString, JsonUtil.getAsString(handleObject, V8Protocol.REF_CLASSNAME));
    if (Type.isObjectType(type)) {
      if (!variable.isPendingReq()) {
        if (!variable.isWaitDrilling()) {
          PropertyReference[] propertyRefs = V8ProtocolUtil.extractObjectProperties(handleObject);
          variable.setProperties(
              JsonUtil.getAsString(handleObject, V8Protocol.REF_CLASSNAME),
              propertyRefs);
        }
        variable.resetDrilling();
      }
    } else if (valueString != null && type != null) {
      variable.setTypeValue(type, valueString);
      variable.resetPending();
    } else {
      variable.setTypeValue(Type.TYPE_STRING, "<Error>");
      variable.resetPending();
    }
  }

  /**
   * @param vars all the variables for the properties
   * @param variableToRef variable to ref map
   * @param reply with the requested handles (some of {@code vars} may be
   *        missing if they were known at the moment of ensuring the properties)
   */
  private static boolean fillVariablesFromLookupReply(HandleManager handleManager,
      Collection<JsVariableImpl> vars, Map<JsVariableImpl, Long> variableToRef, JSONObject reply) {
    if (!JsonUtil.isSuccessful(reply)) {
      return false;
    }
    JSONObject body = JsonUtil.getBody(reply);
    for (JsVariableImpl var : vars) {
      Long ref = variableToRef.get(var);
      JSONObject object = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (object != null) {
        handleManager.put(ref, object);
        fillVariable(var, JsonUtil.getAsJSON(body, String.valueOf(variableToRef.get(var))));
      }
    }
    return true;
  }
}
