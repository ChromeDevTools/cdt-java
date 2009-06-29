// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.concurrent.Semaphore;

import org.chromium.sdk.JsDataType;
import org.chromium.sdk.internal.BrowserTabImpl.V8HandlerCallback;
import org.chromium.sdk.internal.ValueMirror.PropertyReference;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
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
          @Override
          public void failure(String message) {
            scriptsReloadSemaphore.release();
            finalCallback.failure(message);
          }

          @Override
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

  protected void doReloadAllScripts(final V8HandlerCallback finalCallback) {
    context.getScriptManager().reset();
    context.getV8Handler().sendV8Command(
        DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true),
        new V8HandlerCallback() {
          @Override
          public void failure(String message) {
            scriptsReloadSemaphore.release();
            finalCallback.failure(message);
          }

          @Override
          public void messageReceived(JSONObject response) {
            JSONArray body = JsonUtil.getAsJSONArray(response, V8Protocol.KEY_BODY);
            ScriptManager scriptManager = context.getScriptManager();
            for (int i = 0; i < body.size(); ++i) {
              JSONObject scriptJson = (JSONObject) body.get(i);
              scriptManager.addScript(scriptJson);
            }
            scriptsReloadSemaphore.release();
            finalCallback.messageReceived(response);
          }
        });
    context.evaluateJavascript();
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
      PropertyReference[] propertyRefs = DebugContextImpl.extractObjectProperties(handle);
      return new ValueMirror(name, parentRef, propertyRefs, className);
    } else {
      return new ValueMirror(name, value, type);
    }
  }

}
