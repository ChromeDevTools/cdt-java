// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.chromium.sdk.JsDataType;
import org.chromium.sdk.BrowserTab.ScriptsCallback;
import org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ScriptsMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * A helper class for performing complex V8-related operations.
 */
class V8Helper {

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
  void reloadAllScripts(V8HandlerCallback callback) {
    // A quick check goes first
    try {
      scriptsReloadSemaphore.acquire();
    } catch (InterruptedException e) {
      if (callback != null) {
        callback.failure("Interrupted");
      }
      return;
    }
    final V8HandlerCallback finalCallback = (callback != null)
        ? callback
        : V8HandlerCallback.NULL_CALLBACK;
    context.getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, false),
        new V8HandlerCallback() {
          public void failure(String message) {
            scriptsReloadSemaphore.release();
            finalCallback.failure(message);
          }

          public void messageReceived(JSONObject response) {
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            ScriptManager scriptManager = context.getScriptManager();
            boolean hasNewScripts = false;
            for (int i = 0; i < body.size(); ++i) {
              JSONObject scriptJson = (JSONObject) body.get(i);
              if (JsonUtil.getAsString(scriptJson, V8Protocol.BODY_NAME) != null &&
                  !scriptManager.hasScript(scriptJson)) {
                // Do reload all scripts with sources if something has been
                // added
                doReloadAllScripts(finalCallback);
                hasNewScripts = true;
                break;
              }
            }
            if (!hasNewScripts) {
              scriptsReloadSemaphore.release();
              finalCallback.messageReceived(response);
            }
          }
        });
    context.evaluateJavascript();
  }

  void doReloadAllScripts(final V8HandlerCallback callback) {
    context.getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
        new V8HandlerCallback() {
          public void failure(String message) {
            scriptsReloadSemaphore.release();
            callback.failure(message);
          }

          public void messageReceived(JSONObject response) {
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            ScriptManager scriptManager = context.getScriptManager();
            for (int i = 0; i < body.size(); ++i) {
              JSONObject scriptJson = (JSONObject) body.get(i);
              Long id = V8ProtocolUtil.getScriptIdFromResponse(scriptJson);
              if (scriptManager.findById(id) == null) {
                scriptManager.addScript(scriptJson);
              }
            }
            scriptsReloadSemaphore.release();
            callback.messageReceived(response);
          }
        });
    context.evaluateJavascript();
  }

  /**
   * Traverses scripts from the "refs" array of a "backtrace" response and
   * reloads scripts if new ones are found. Does NOT remove GC'ed scripts
   * that are now absent in the "refs" array.
   *
   * @param refs array from the "backtrace" response
   * @param callback to invoke when the scripts are reloaded
   */
  void updateScriptsIfNeeded(JSONArray refs, final ScriptsCallback callback) {
    try {
      scriptsReloadSemaphore.acquire();
    } catch (InterruptedException e) {
      if (callback != null) {
        callback.failure("Interrupted");
      }
      return;
    }
    int size = refs.size();
    final ScriptManager scriptManager = context.getScriptManager();
    boolean scriptsChanged = false;
    for (int i = 0; i < size; i++) {
      JSONObject ref = (JSONObject) refs.get(i);
      if (!V8Protocol.FRAME_SCRIPT.key.equals(JsonUtil.getAsString(ref, V8Protocol.KEY_TYPE))) {
        continue;
      }
      if (scriptManager.findById(V8ProtocolUtil.getScriptIdFromResponse(ref)) == null) {
        scriptsChanged = true;
        break;
      }
    }
    if (!scriptsChanged) {
      scriptsReloadSemaphore.release();
      callback.success(scriptManager.allScripts());
      return;
    }
    doReloadAllScripts(new V8HandlerCallback(){
      public void messageReceived(JSONObject response) {
        callback.success(scriptManager.allScripts());
      }
      public void failure(String message) {
        callback.failure(message);
      }
    });
  }

  /**
   * Gets all resolved locals for the stack frame, caches scripts and objects in
   * the scriptManager and handleManager.
   *
   * @param frame to get the data for
   * @return the mirrors corresponding to the frame locals
   */
  ValueMirror[] computeLocals(JSONObject frame) {
    JSONArray args = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_ARGUMENTS);
    JSONArray locals = JsonUtil.getAsJSONArray(frame, V8Protocol.BODY_LOCALS);

    int maxLookups = args.size() + locals.size() + 1 /* "this" */;

    final List<ValueMirror> values = new ArrayList<ValueMirror>(maxLookups);

    // Receiver ("this")
    JSONObject receiver = JsonUtil.getAsJSON(frame, V8Protocol.FRAME_RECEIVER);
    putMirror(values, thisName, receiver);

    // Arguments
    for (int i = 0; i < args.size(); i++) {
      JSONObject arg = (JSONObject) args.get(i);
      String name = JsonUtil.getAsString(arg, V8Protocol.ARGUMENT_NAME);
      if (name == null) {
        // an unnamed actual argument (there is no formal counterpart in the
        // method signature) that will be available in the "arguments" object
        continue;
      }
      putMirror(values, name, JsonUtil.getAsJSON(arg, V8Protocol.ARGUMENT_VALUE));
    }

    // Locals
    for (int i = 0; i < locals.size(); i++) {
      JSONObject local = (JSONObject) locals.get(i);
      String localName = JsonUtil.getAsString(local, V8Protocol.LOCAL_NAME);

      if (!V8ProtocolUtil.isInternalProperty(localName)) {
        putMirror(values, localName, JsonUtil.getAsJSON(local, V8Protocol.LOCAL_VALUE));
      }
    }

    return values.toArray(new ValueMirror[values.size()]);
  }

  private void putMirror(final List<ValueMirror> values,
      String name, JSONObject valueObject) {
    Long objectRef = JsonUtil.getAsLong(valueObject, V8Protocol.REF);
    JSONObject object = context.getHandleManager().getHandle(objectRef);
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
   *         from handle
   */
  static ValueMirror createValueMirror(JSONObject handle, String name) {
    String value = JsonUtil.getAsString(handle, V8Protocol.REF_TEXT);
    String typeString = JsonUtil.getAsString(handle, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(handle, V8Protocol.REF_CLASSNAME);
    JsDataType type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (JsDataType.isObjectType(type)) {
      JSONObject protoObj = JsonUtil.getAsJSON(handle, V8Protocol.REF_PROTOOBJECT);
      int parentRef = JsonUtil.getAsLong(protoObj, V8Protocol.REF).intValue();
      PropertyReference[] propertyRefs = V8ProtocolUtil.extractObjectProperties(handle);
      return new ValueMirror(name, parentRef, propertyRefs, className);
    } else {
      return new ValueMirror(name, value, type);
    }
  }

  static ValueMirror createValueMirrorFromValue(JSONObject valueObject, String name) {
    String typeString = JsonUtil.getAsString(valueObject, V8Protocol.REF_TYPE);
    String className = JsonUtil.getAsString(valueObject, V8Protocol.REF_CLASSNAME);
    JsDataType type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (type == null) {
      return null; // bad value object
    }
    if (JsDataType.isObjectType(type)) {
      return new ValueMirror(name, JsonUtil.getAsLong(valueObject, V8Protocol.REF).intValue());
    } else {
      String value = JsonUtil.getAsString(valueObject, V8Protocol.REF_VALUE);
      if (value == null) {
        value = typeString; // e.g. "undefined"
      }
      return new ValueMirror(name, value, type);
    }
  }

}
