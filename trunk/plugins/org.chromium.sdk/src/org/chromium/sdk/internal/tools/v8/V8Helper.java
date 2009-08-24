// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.internal.DebugContextImpl;
import org.chromium.sdk.internal.HandleManager;
import org.chromium.sdk.internal.InternalContext;
import org.chromium.sdk.internal.JsDataTypeUtil;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.ScriptManager;
import org.chromium.sdk.internal.ValueMirror;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A helper class for performing complex V8-related operations.
 */
public class V8Helper {

  /**
   * The debug context in which the operations are performed.
   */
  private final DebugContextImpl context;

  /**
   * The "receiver" variable name (usually "this").
   */
  private final String thisName;

  /**
   * A semaphore that prevents concurrent script reloading (which may effectively
   * double the efforts.)
   */
  private final Semaphore scriptsReloadSemaphore = new Semaphore(1);

  public V8Helper(DebugContextImpl context, String thisName) {
    this.context = context;
    this.thisName = thisName;
  }

  /**
   * Reloads all normal scripts found in the page. First, all scripts without
   * their sources are retrieved to save bandwidth (script list change during a
   * page lifetime is a relatively rare event.) If at least one script has been
   * added, the script cache is dropped and re-populated with new scripts that
   * are re-requested together with their sources.
   *
   * @param callback to invoke when the script reloading has completed
   */
  public void reloadAllScriptsAsync(V8CommandProcessor.V8HandlerCallback callback,
      SyncCallback syncCallback) {
    final V8CommandProcessor.V8HandlerCallback finalCallback = callback != null
        ? callback
        : V8CommandProcessor.V8HandlerCallback.NULL_CALLBACK;
    lock();
    context.sendMessageAsync(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
        true,
        new V8CommandProcessor.V8HandlerCallback() {
          public void failure(String message) {
            unlock();
            finalCallback.failure(message);
          }

          public void messageReceived(JSONObject response) {
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            ScriptManager scriptManager = context.getScriptManager();
            for (int i = 0; i < body.size(); ++i) {
              JSONObject scriptJson = (JSONObject) body.get(i);
              Long id = V8ProtocolUtil.getScriptIdFromResponse(scriptJson);
              if (scriptManager.findById(id) == null &&
                  !ChromeDevToolSessionManager.JAVASCRIPT_VOID.equals(
                      JsonUtil.getAsString(scriptJson, V8Protocol.SOURCE_CODE))) {
                scriptManager.addScript(
                    scriptJson, JsonUtil.getAsJSONArray(response, V8Protocol.FRAME_REFS));
              }
            }
            unlock();
            finalCallback.messageReceived(response);
          }
        },
        syncCallback);
  }

  protected void lock() {
    try {
      scriptsReloadSemaphore.acquire();
    } catch (InterruptedException e) {
      // consider it a successful acquisition
    }
  }

  protected void unlock() {
    scriptsReloadSemaphore.release();
  }

  /**
   * Gets all resolved locals for the call frame, caches scripts and objects in
   * the scriptManager and handleManager.
   *
   * @param frame to get the data for
   * @return the mirrors corresponding to the frame locals
   */
  public ValueMirror[] computeLocals(JSONObject frame, InternalContext internalContext) {
    HandleManager handleManager = internalContext.getHandleManager();
    JSONArray args = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_ARGUMENTS);
    JSONArray locals = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_LOCALS);

    int maxLookups = args.size() + locals.size() + 1 /* "this" */;

    final List<ValueMirror> values = new ArrayList<ValueMirror>(maxLookups);

    // Receiver ("this")
    JSONObject receiver = JsonUtil.getAsJSON(frame, V8Protocol.FRAME_RECEIVER);
    putMirror(values, thisName, receiver, handleManager);

    // Arguments
    for (int i = 0; i < args.size(); i++) {
      JSONObject arg = (JSONObject) args.get(i);
      String name = JsonUtil.getAsString(arg, V8Protocol.ARGUMENT_NAME);
      if (name == null) {
        // an unnamed actual argument (there is no formal counterpart in the
        // method signature) that will be available in the "arguments" object
        continue;
      }
      putMirror(values, name, JsonUtil.getAsJSON(arg, V8Protocol.ARGUMENT_VALUE), handleManager);
    }

    // Locals
    for (int i = 0; i < locals.size(); i++) {
      JSONObject local = (JSONObject) locals.get(i);
      String localName = JsonUtil.getAsString(local, V8Protocol.LOCAL_NAME);

      if (!V8ProtocolUtil.isInternalProperty(localName)) {
        putMirror(values, localName, JsonUtil.getAsJSON(local, V8Protocol.LOCAL_VALUE),
            handleManager);
      }
    }

    return values.toArray(new ValueMirror[values.size()]);
  }

  private void putMirror(final List<ValueMirror> values,
      String name, JSONObject valueObject, HandleManager handleManager) {
    Long objectRef = JsonUtil.getAsLong(valueObject, V8Protocol.REF);
    JSONObject object = handleManager.getHandle(objectRef);
    if (object != null) {
      values.add(createValueMirror(object, name));
    } else {
      values.add(createValueMirrorFromValue(valueObject, name));
    }
  }

  /**
   * Constructs a ValueMirror given a V8 debugger object specification and the
   * value name.
   *
   * @param handle containing the object specification from the V8 debugger
   * @param name of the value to construct
   * @return a ValueMirror instance with the specified name, containing data
   *         from handle, or {@code null} if {@code handle} is not a handle
   */
  public static ValueMirror createValueMirror(JSONObject handle, String name) {
    String value = JsonUtil.getAsString(handle, V8Protocol.REF_TEXT);
    if (value == null) { // try another format
      return createValueMirrorFromValue(handle, name);
    }
    String typeString = JsonUtil.getAsString(handle, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME);
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (Type.isObjectType(type)) {
      JSONObject protoObj = JsonUtil.getAsJSON(handle, V8Protocol.REF_PROTOOBJECT);
      int parentRef = JsonUtil.getAsLong(protoObj, V8Protocol.REF).intValue();
      PropertyReference[] propertyRefs = V8ProtocolUtil.extractObjectProperties(handle);
      return new ValueMirror(name, parentRef, propertyRefs, className);
    } else {
      return new ValueMirror(name, value, type, className);
    }
  }

  /**
   * Constructs a ValueMirror given a value object (the one received with the
   * "inlineRefs" or "compactFormat" argument).
   *
   * @param valueObject the value object
   * @param name of the ValueMirror to construct
   * @return a ValueMirror instance with the specified name, containing data
   *         from valueObject, or {@code null} if the valueObject format is
   *         not valid
   */
  private static ValueMirror createValueMirrorFromValue(JSONObject valueObject, String name) {
    String typeString = JsonUtil.getAsString(valueObject, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(valueObject, V8Protocol.REF_CLASSNAME);
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (type == null) {
      return null; // bad value object
    }
    if (Type.isObjectType(type)) {
      return new ValueMirror(
          name, JsonUtil.getAsLong(valueObject, V8Protocol.REF).intValue(), null, className);
    } else {
      String value = JsonUtil.getAsString(valueObject, V8Protocol.REF_VALUE);
      if (value == null) {
        value = typeString; // e.g. "undefined"
      }
      return new ValueMirror(name, value, type, className);
    }
  }

}
