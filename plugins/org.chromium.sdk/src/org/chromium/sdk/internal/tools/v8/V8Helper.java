// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.FrameMirror;
import org.chromium.sdk.internal.InternalContext;
import org.chromium.sdk.internal.JsDataTypeUtil;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.PropertyHoldingValueMirror;
import org.chromium.sdk.internal.PropertyReference;
import org.chromium.sdk.internal.ScriptManager;
import org.chromium.sdk.internal.ValueLoadException;
import org.chromium.sdk.internal.ValueMirror;
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
  private final DebugSession debugSession;

  /**
   * A semaphore that prevents concurrent script reloading (which may effectively
   * double the efforts.)
   */
  private final Semaphore scriptsReloadSemaphore = new Semaphore(1);

  public V8Helper(DebugSession debugSession) {
    this.debugSession = debugSession;
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
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
        true,
        new V8CommandProcessor.V8HandlerCallback() {
          public void failure(String message) {
            unlock();
            finalCallback.failure(message);
          }

          public void messageReceived(JSONObject response) {
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            ScriptManager scriptManager = debugSession.getScriptManager();
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
  public FrameMirror.Locals computeLocals(JSONObject frame, InternalContext internalContext) {
    JSONArray args = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_ARGUMENTS);
    JSONArray locals = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_LOCALS);

    int maxLookups = args.size() + locals.size() + 1 /* "this" */;

    final List<PropertyReference> localRefs = new ArrayList<PropertyReference>(maxLookups);

    {
      // Receiver ("this")
      JSONObject receiverObject = JsonUtil.getAsJSON(frame, V8Protocol.FRAME_RECEIVER);
      V8ProtocolUtil.putMirror(localRefs, receiverObject, null,
          V8ProtocolUtil.PropertyNameGetter.THIS);
    }

    // Arguments
    for (int i = 0; i < args.size(); i++) {
      JSONObject arg = (JSONObject) args.get(i);
      V8ProtocolUtil.putMirror(localRefs, arg, V8Protocol.ARGUMENT_VALUE,
          V8ProtocolUtil.PropertyNameGetter.ARGUMENT);
    }

    // Locals
    for (int i = 0; i < locals.size(); i++) {
      JSONObject local = (JSONObject) locals.get(i);
      V8ProtocolUtil.putMirror(localRefs, local, V8Protocol.LOCAL_VALUE,
          V8ProtocolUtil.PropertyNameGetter.LOCAL);
    }

    return new FrameMirror.Locals() {
      public List<PropertyReference> getLocalRefs() {
        return localRefs;
      }
    };
  }

  /**
   * Constructs a ValueMirror given a V8 debugger object specification.
   *
   * @param jsonValue containing the object specification from the V8 debugger
   * @return a {@link PropertyHoldingValueMirror} instance, containing data
   *         from jsonValue; not null
   */
  public static PropertyHoldingValueMirror createMirrorFromLookup(JSONObject jsonValue) {
    String text = JsonUtil.getAsString(jsonValue, V8Protocol.REF_TEXT);
    if (text == null) {
      throw new ValueLoadException("Bad lookup result");
    }
    String typeString = JsonUtil.getAsString(jsonValue, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(jsonValue, V8Protocol.REF_CLASSNAME);
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (type == null) {
      throw new ValueLoadException("Bad lookup result: type field not specified");
    }
    return createMirrorFromLookup(jsonValue, className, type, text);
  }

  /**
   * Constructs a {@link ValueMirror} given a V8 debugger object specification if it's possible.
   * @return a {@link ValueMirror} instance, containing data
   *         from {@code jsonValue}; or {@code null} if {@code jsonValue} is not a handle
   */
  public static ValueMirror createValueMirrorOptional(JSONObject jsonValue) {
    String typeString = JsonUtil.getAsString(jsonValue, V8Protocol.REF_TYPE);
    if (typeString == null) {
      return null;
    }
    return createValueMirror(jsonValue, typeString);
  }

  private static ValueMirror createValueMirror(JSONObject jsonValue, String typeString) {
    String className = JsonUtil.getAsString(jsonValue, V8Protocol.REF_CLASSNAME);
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (type == null) {
      throw new ValueLoadException("Bad value object");
    }
    String text = JsonUtil.getAsString(jsonValue, V8Protocol.REF_TEXT);
    if (text == null) { // try another format
      if (Type.isObjectType(type)) {
        int refId = JsonUtil.getAsLong(jsonValue, V8Protocol.REF).intValue();
        return ValueMirror.createObjectUnknownProperties(refId, className);
      } else {
        // try another format
        String value = JsonUtil.getAsString(jsonValue, V8Protocol.REF_VALUE);
        if (value == null) {
          value = typeString; // e.g. "undefined"
        }
        return ValueMirror.createScalar(value, type, className).getValueMirror();
      }
    } else {
      return createMirrorFromLookup(jsonValue, className, type, text).getValueMirror();
    }
  }

  private static PropertyHoldingValueMirror createMirrorFromLookup(JSONObject jsonValue,
      String className, Type type, String text) {
    if (Type.isObjectType(type)) {
      int refId = JsonUtil.getAsLong(jsonValue, V8Protocol.REF_HANDLE).intValue();
      List<? extends PropertyReference> propertyRefs
          = V8ProtocolUtil.extractObjectProperties(jsonValue);
      return ValueMirror.createObject(refId, propertyRefs, className);
    } else {
      return ValueMirror.createScalar(text, type, className);
    }
  }

  public static <MESSAGE, RES, EX extends Exception> RES callV8Sync(
      V8CommandSender<MESSAGE, EX> commandSender, MESSAGE message,
      V8BlockingCallback<RES> callback) throws EX {
    return callV8Sync(commandSender, message, callback,
        CallbackSemaphore.OPERATION_TIMEOUT_MS);
  }

  public static <MESSAGE, RES, EX extends Exception> RES callV8Sync(
      V8CommandSender<MESSAGE, EX> commandSender,
      MESSAGE message, final V8BlockingCallback<RES> callback, long timeoutMs) throws EX {
    CallbackSemaphore syncCallback = new CallbackSemaphore();
    final Exception [] exBuff = { null };
    // A long way of creating buffer for generic type without warnings.
    final List<RES> resBuff = new ArrayList<RES>(Collections.nCopies(1, (RES)null));
    V8CommandProcessor.V8HandlerCallback callbackWrapper =
        new V8CommandProcessor.V8HandlerCallback() {
      public void failure(String message) {
        exBuff[0] = new Exception("Failure: " + message);
      }

      public void messageReceived(JSONObject response) {
        RES result = callback.messageReceived(response);
        resBuff.set(0, result);
      }
    };
    commandSender.sendV8CommandAsync(message, true, callbackWrapper, syncCallback);

    boolean waitRes;
    try {
      waitRes = syncCallback.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (RuntimeException e) {
      throw new CallbackException(e);
    }

    if (!waitRes) {
      throw new CallbackException("Timeout");
    }

    if (exBuff[0] != null) {
      throw new CallbackException(exBuff[0]);
    }

    return resBuff.get(0);
  }

  /**
   * Special kind of exceptions for problems in receiving or waiting for the answer.
   * Clients may try to catch it.
   */
  public static class CallbackException extends RuntimeException {
    CallbackException() {
    }
    CallbackException(String message, Throwable cause) {
      super(message, cause);
    }
    CallbackException(String message) {
      super(message);
    }
    CallbackException(Throwable cause) {
      super(cause);
    }
  }
}
