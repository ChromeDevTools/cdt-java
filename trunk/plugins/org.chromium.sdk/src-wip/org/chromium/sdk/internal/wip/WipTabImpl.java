// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.EvaluateWithContextExtension;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.EnableParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.PauseParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetBreakpointsActiveParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetPauseOnExceptionsParams;
import org.chromium.sdk.util.SignalRelay;
import org.chromium.sdk.util.SignalRelay.AlreadySignalledException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * {@link BrowserTab} implementation that attaches to remote tab via WebInspector
 * protocol (WIP).
 */
public class WipTabImpl implements BrowserTab {
  private static final Logger LOGGER = Logger.getLogger(WipTabImpl.class.getName());

  private final WsConnection socket;
  private final WipBrowserImpl browserImpl;
  private final TabDebugEventListener tabListener;
  private final WipCommandProcessor commandProcessor;
  private final WipScriptManager scriptManager = new WipScriptManager(this);
  private final WipBreakpointManager breakpointManager = new WipBreakpointManager(this);
  private final WipContextBuilder contextBuilder = new WipContextBuilder(this);
  private final WipFrameManager frameManager = new WipFrameManager(this);

  private final VmState vmState = new VmState();
  private final SignalRelay<Void> closeSignalRelay;

  private volatile String url;

  public WipTabImpl(WsConnection socket, WipBrowserImpl browserImpl,
      TabDebugEventListener tabListener, String preliminaryUrl) throws IOException {
    this.socket = socket;
    this.browserImpl = browserImpl;
    this.tabListener = tabListener;
    this.url = preliminaryUrl;

    this.closeSignalRelay = SignalRelay.create(new SignalRelay.Callback<Void>() {
      @Override
      public void onSignal(Void signal, Exception cause) {
        WipTabImpl.this.tabListener.closed();
      }
    });

    try {
      closeSignalRelay.bind(socket.getCloser(), null, null);
    } catch (AlreadySignalledException e) {
      throw new IOException("Connection is closed", e);
    }

    commandProcessor = new WipCommandProcessor(this, socket);

    WsConnection.Listener socketListener = new WsConnection.Listener() {
      @Override
      public void textMessageRecieved(String text) {
        JSONObject json;
        try {
          json = JsonUtil.jsonObjectFromJson(text);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
        commandProcessor.acceptResponse(json);
      }

      @Override
      public void errorMessage(Exception ex) {
        LOGGER.log(Level.SEVERE, "WebSocket protocol error", ex);
      }

      @Override
      public void eofMessage() {
        // Unused.
      }
    };

    socket.startListening(socketListener);

    init();
  }

  private void init() throws IOException {
    SyncCallback syncCallback = new SyncCallback() {
      @Override
      public void callbackDone(RuntimeException e) {
        // This statement suits sync callback more rather than a regular callback:
        // it's safe enough and we prefer to execute it event if the command failed.
        scriptManager.endPopulateScriptMode();
      }
    };
    commandProcessor.send(new EnableParams(), null, syncCallback);

    frameManager.readFrames();
  }

  void updateUrl(String url, boolean silent) {
    this.url = url;
    if (silent) {
      return;
    }
    scriptManager.pageReloaded();
    WipTabImpl.this.tabListener.navigated(this.url);
  }

  WipScriptManager getScriptManager() {
    return scriptManager;
  }

  @Override
  public boolean detach() {
    closeSignalRelay.sendSignal(null, null);
    return true;
  }

  @Override
  public boolean isAttached() {
    return !closeSignalRelay.isSignalled();
  }

  @Override
  public void enableBreakpoints(Boolean enabled,
      GenericCallback<Boolean> callback, SyncCallback syncCallback) {
    updateVmVariable(enabled, VmState.BREAKPOINTS_ACTIVE, callback, syncCallback);
  }

  @Override
  public void setBreakOnException(ExceptionCatchType catchType,
      Boolean enabled, GenericCallback<Boolean> callback, SyncCallback syncCallback) {

    VmState.Variable<Boolean> variable;
    switch (catchType) {
    case CAUGHT:
      variable = VmState.BREAK_ON_CAUGHT;
      break;
    case UNCAUGHT:
      variable = VmState.BREAK_ON_UNCAUGHT;
      break;
    default:
      throw new RuntimeException();
    }
    updateVmVariable(enabled, variable, callback, syncCallback);
  }

  /**
   * Updates locally saved variables state and send request to remote. If user only calls
   * the method to learn the current value, request is sent anyway, to keep responses in sequence.
   */
  private <T> void updateVmVariable(T value, VmState.Variable<T> variable,
      final GenericCallback<T> callback, SyncCallback syncCallback) {
    synchronized (vmState) {
      final T newValue;
      if (value == null) {
        newValue = variable.getValue(vmState);
      } else {
        variable.setValue(vmState, value);
        newValue = value;
      }
      WipParams params = variable.createRequestParams(vmState);
      WipCommandCallback wrappedCallback;
      if (callback == null) {
        wrappedCallback = null;
      } else {
        wrappedCallback = new WipCommandCallback.Default() {
          @Override protected void onSuccess(Success success) {
            callback.success(newValue);
          }
          @Override protected void onError(String message) {
            callback.failure(new Exception(message));
          }
        };
      }
      commandProcessor.send(params, wrappedCallback, syncCallback);
    }
  }

  @Override
  public Version getVersion() {
    // TODO(peter.rybin): support it.
    return new Version(Collections.<Integer>emptyList(), " <Unknown V8 version>");
  }

  @Override
  public EvaluateWithContextExtension getEvaluateWithContextExtension() {
    // TODO(peter.rybin): implement
    return null;
  }

  @Override
  public void getScripts(final ScriptsCallback callback)
      throws MethodIsBlockingException {

    final CallbackSemaphore callbackSemaphore = new CallbackSemaphore();

    JavascriptVm.GenericCallback<Collection<Script>> innerCallback;
    if (callback == null) {
      innerCallback = null;
    } else {
      innerCallback = new JavascriptVm.GenericCallback<Collection<Script>>() {
        @Override public void success(Collection<Script> value) {
          callback.success(value);
        }
        @Override public void failure(Exception exception) {
          callback.failure(exception.getMessage());
        }
      };
    }

    scriptManager.getScripts(innerCallback, callbackSemaphore);

    callbackSemaphore.acquireDefault();
  }

  @Override
  public void setBreakpoint(Type type, String target, int line, int column,
      boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback, SyncCallback syncCallback) {
    breakpointManager.setBreakpoint(type, target, line, column, enabled, condition,
        ignoreCount, callback, syncCallback);
  }

  @Override
  public void suspend(final SuspendCallback callback) {
    PauseParams params = new PauseParams();
    WipCommandCallback wrappedCallback;
    if (callback == null) {
      wrappedCallback = null;
    } else {
      wrappedCallback = new WipCommandCallback.Default() {
        @Override protected void onSuccess(Success success) {
          callback.success();
        }
        @Override protected void onError(String message) {
          callback.failure(new Exception(message));
        }
      };
    }
    commandProcessor.send(params, wrappedCallback, null);
  }

  @Override
  public void listBreakpoints(ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    if (callback != null) {
      callback.success(breakpointManager.getAllBreakpoints());
    }
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
  }

  @Override
  public Browser getBrowser() {
    return browserImpl;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public TabDebugEventListener getDebugListener() {
    return this.tabListener;
  }

  public WsConnection getWsSocket() {
    return this.socket;
  }

  WipContextBuilder getContextBuilder() {
    return contextBuilder;
  }

  WipCommandProcessor getCommandProcessor() {
    return commandProcessor;
  }

  WipFrameManager getFrameManager() {
    return frameManager;
  }

  TabDebugEventListener getTabListener() {
    return tabListener;
  }

  /**
   * Saves currently set VM parameters. Default values must correspond to those
   * of WebInspector protocol.
   */
  private static class VmState {
    boolean breakpointsActive = false;

    boolean breakOnCaughtExceptions = false;
    boolean breakOnUncaughtExceptions = false;

    static abstract class Variable<T> {
      abstract T getValue(VmState vmState);
      abstract void setValue(VmState vmState, T value);
      abstract WipParams createRequestParams(VmState vmState);
    }

    static final Variable<Boolean> BREAKPOINTS_ACTIVE = new Variable<Boolean>() {
      @Override Boolean getValue(VmState vmState) {
        return vmState.breakpointsActive;
      }
      @Override void setValue(VmState vmState, Boolean value) {
        vmState.breakpointsActive = value;
      }
      @Override WipParams createRequestParams(VmState vmState) {
        return new SetBreakpointsActiveParams(vmState.breakpointsActive);
      }
    };

    static final Variable<Boolean> BREAK_ON_CAUGHT = new Variable<Boolean>() {
      @Override Boolean getValue(VmState vmState) {
        return vmState.breakOnCaughtExceptions;
      }
      @Override void setValue(VmState vmState, Boolean value) {
        vmState.breakOnCaughtExceptions = value;
      }
      @Override WipParams createRequestParams(VmState vmState) {
        return vmState.createPauseOnExceptionRequest();
      }
    };

    static final Variable<Boolean> BREAK_ON_UNCAUGHT = new Variable<Boolean>() {
      @Override Boolean getValue(VmState vmState) {
        return vmState.breakOnUncaughtExceptions;
      }
      @Override void setValue(VmState vmState, Boolean value) {
        vmState.breakOnUncaughtExceptions = value;
      }
      @Override WipParams createRequestParams(VmState vmState) {
        return vmState.createPauseOnExceptionRequest();
      }
    };

    private SetPauseOnExceptionsParams createPauseOnExceptionRequest() {
      // We try to push 2 bits (inherited from V8 internal flags)
      // into 3-state flag (WIP type).
      // TODO: change SDK and reduce 2 flags to 1 3-state value.
      SetPauseOnExceptionsParams.State protocolState;
      if (breakOnCaughtExceptions) {
        protocolState = SetPauseOnExceptionsParams.State.ALL;
      } else {
        if (breakOnUncaughtExceptions) {
          protocolState = SetPauseOnExceptionsParams.State.UNCAUGHT;
        } else {
          protocolState = SetPauseOnExceptionsParams.State.NONE;
        }
      }
      return new SetPauseOnExceptionsParams(protocolState);
    }
  }
}
