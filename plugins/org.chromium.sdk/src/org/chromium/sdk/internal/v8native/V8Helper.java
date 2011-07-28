// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.protocol.V8ProtocolUtil;
import org.chromium.sdk.internal.v8native.protocol.input.CommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.PropertyObject;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.ContextlessDebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.protocol.output.ScriptsMessage;
import org.chromium.sdk.internal.v8native.value.JsDataTypeUtil;
import org.chromium.sdk.internal.v8native.value.LoadableString;
import org.chromium.sdk.internal.v8native.value.PropertyReference;
import org.chromium.sdk.internal.v8native.value.ValueLoadException;

/**
 * A helper class for performing complex V8-related operations.
 */
public class V8Helper {

  public interface ScriptLoadCallback {
    void success();
    void failure(String message);
  }

  /**
   * Loads all scripts and stores them in ScriptManager.
   * @param callback to invoke when the script reloading has completed
   * @param syncCallback to invoke after callback whether it normally returned
   *     or threw an exception
   */
  public static RelayOk reloadAllScriptsAsync(final DebugSession debugSession,
      final ScriptLoadCallback callback, SyncCallback syncCallback) {
    return reloadScriptAsync(debugSession, null, callback, syncCallback);
  }

  /**
   * Loads specified scripts or all existing scripts and stores them in ScriptManager.
   * @param ids ids of requested scripts or null for all scripts
   * @param callback to invoke when the script reloading has completed
   * @param syncCallback to invoke after callback, regardless of whether it has returned normally
   *        or thrown an exception
   */
  public static RelayOk reloadScriptAsync(final DebugSession debugSession, final List<Long> ids,
      final ScriptLoadCallback callback, SyncCallback syncCallback) {
    ContextlessDebuggerMessage message = DebuggerMessageFactory.scripts(ids, true);
    if (ids == null) {
      message = DebuggerMessageFactory.scripts(ScriptsMessage.SCRIPTS_NORMAL, true);
    } else {
      message = DebuggerMessageFactory.scripts(ids, true);
    }
    return debugSession.sendMessageAsync(
        message,
        true,
        new V8CommandCallbackBase() {
          @Override
          public void failure(String message) {
            if (callback != null) {
              callback.failure(message);
            }
          }

          @Override
          public void success(SuccessCommandResponse successResponse) {
            List<ScriptHandle> body;
            try {
              body = successResponse.body().asScripts();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }
            ScriptManager scriptManager = debugSession.getScriptManager();
            for (int i = 0; i < body.size(); ++i) {
              ScriptHandle scriptHandle = body.get(i);
              if (V8Helper.JAVASCRIPT_VOID.equals(scriptHandle.source())) {
                continue;
              }
              Long id = V8ProtocolUtil.getScriptIdFromResponse(scriptHandle);
              ScriptImpl scriptById = scriptManager.findById(id);
              if (scriptById == null) {
                scriptManager.addScript(scriptHandle, successResponse.refs());
              } else {
                // A scrupulous refactoring note:
                // do not call setSource in a legacy case, when ids parameter is null.
                if (ids != null) {
                  scriptById.setSource(scriptHandle.source());
                }
              }
            }
            if (callback != null) {
              callback.success();
            }
          }
        },
        syncCallback);
  }

  public static PropertyReference computeReceiverRef(FrameObject frame) {
    SomeRef receiverObject = frame.receiver();
    return V8ProtocolUtil.extractProperty(receiverObject,
        V8ProtocolUtil.PropertyNameGetter.THIS);
  }

  public static LoadableString createLoadableString(ValueHandle handle,
      LoadableString.Factory stringFactory) {
    Long len = handle.length();
    Long toIndex = handle.toIndex();
    if (len != null && toIndex != null && len.longValue() != toIndex.longValue()) {
      // String is not fully loaded.
      return stringFactory.create(handle);
    }
    return new LoadableString.Immutable(handle.text());
  }

  public static Type calculateType(String typeString, String className, boolean tolerateNullType) {
    Type type = JsDataTypeUtil.fromJsonTypeAndClassName(typeString, className);
    if (type == null) {
      if (tolerateNullType) {
        type = Type.TYPE_OBJECT;
      } else {
        throw new ValueLoadException("Bad value object");
      }
    }
    return type;
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
      @Override
      public void failure(String message) {
        exBuff[0] = new Exception("Failure: " + message);
      }

      @Override
      public void messageReceived(CommandResponse response) {
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
   * A no-op JavaScript to evaluate.
   */
  public static final String JAVASCRIPT_VOID = "javascript:void(0);";

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
