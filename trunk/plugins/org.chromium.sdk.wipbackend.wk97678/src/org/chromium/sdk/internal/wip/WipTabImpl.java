// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.FunctionScopeExtension;
import org.chromium.sdk.IgnoreCountBreakpointExtension;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RestartFrameExtension;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.PauseParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetBreakpointsActiveParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetPauseOnExceptionsParams;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;
import org.chromium.sdk.util.RelaySyncCallback;
import org.chromium.sdk.util.SignalRelay;
import org.chromium.sdk.util.SignalRelay.AlreadySignalledException;
import org.chromium.sdk.wip.EvaluateToMappingExtension;
import org.chromium.sdk.wip.PermanentRemoteValueMapping;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserTab;
import org.chromium.sdk.wip.WipJavascriptVm;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * {@link BrowserTab} implementation that attaches to remote tab via WebInspector
 * protocol (WIP).
 */
public class WipTabImpl implements WipBrowserTab, WipJavascriptVm {
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
        commandProcessor.processEos();
      }
    };

    socket.startListening(socketListener);

    init();
  }

  private void init() {
    SyncCallback syncCallback = new SyncCallback() {
      @Override
      public void callbackDone(RuntimeException e) {
        // This statement suits sync callback more rather than a regular callback:
        // it's safe enough and we prefer to execute it event if the command failed.
        scriptManager.endPopulateScriptMode();
      }
    };

    commandProcessor.send(
        new org.chromium.sdk.internal.wip.protocol.output.debugger.EnableParams(),
        null, syncCallback);

    commandProcessor.send(
        new org.chromium.sdk.internal.wip.protocol.output.page.EnableParams(),
        null, null);

    frameManager.readFrames();
  }

  void updateUrl(String url, boolean silent) {
    this.url = url;
    if (silent) {
      return;
    }
    scriptManager.pageReloaded();
    breakpointManager.clearNonProvisionalBreakpoints();
    WipTabImpl.this.tabListener.navigated(this.url);
    contextBuilder.getEvaluateHack().pageReloaded();
  }

  WipScriptManager getScriptManager() {
    return scriptManager;
  }

  WipBreakpointManager getBreakpointManager() {
    return breakpointManager;
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
  public PermanentRemoteValueMapping createPermanentValueMapping(String id) {
    return new PermanentRemoteValueMappingImpl(this, id);
  }

  @Override
  public RelayOk enableBreakpoints(Boolean enabled,
      GenericCallback<Boolean> callback, SyncCallback syncCallback) {
    return updateVmVariable(enabled, VmState.BREAKPOINTS_ACTIVE, callback, syncCallback);
  }

  @Override
  public RelayOk setBreakOnException(ExceptionCatchMode catchMode,
      GenericCallback<ExceptionCatchMode> callback, SyncCallback syncCallback) {

    VmState.Variable<ExceptionCatchMode> variable = VmState.BREAK_ON_EXCEPTION;
    return updateVmVariable(catchMode, variable, callback, syncCallback);
  }

  /**
   * Updates locally saved variables state and send request to remote. If user only calls
   * the method to learn the current value, request is sent anyway, to keep responses in sequence.
   * @return
   */
  private <T> RelayOk updateVmVariable(T value, VmState.Variable<T> variable,
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
      return commandProcessor.send(params, wrappedCallback, syncCallback);
    }
  }

  @Override
  public Version getVersion() {
    // TODO(peter.rybin): support it.
    return new Version(Arrays.asList(0, 0), " <Unknown V8 version>");
  }

  @Override
  public BreakpointTypeExtension getBreakpointTypeExtension() {
    return WipBreakpointImpl.TYPE_EXTENSION;
  }

  @Override
  public IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtension() {
    return WipBreakpointImpl.getIgnoreCountBreakpointExtensionImpl();
  }

  @Override
  public EvaluateToMappingExtension getEvaluateWithDestinationMappingExtension() {
    return WipEvaluateContextBase.EVALUATE_TO_MAPPING_EXTENSION;
  }

  @Override public FunctionScopeExtension getFunctionScopeExtension() {
    return null;
  }

  @Override
  public RestartFrameExtension getRestartFrameExtension() {
    return null;
  }

  @Override
  public void getScripts(final ScriptsCallback callback)
      throws MethodIsBlockingException {

    final CallbackSemaphore callbackSemaphore = new CallbackSemaphore();

    GenericCallback<Collection<Script>> innerCallback;
    if (callback == null) {
      innerCallback = null;
    } else {
      innerCallback = new GenericCallback<Collection<Script>>() {
        @Override public void success(Collection<Script> value) {
          callback.success(value);
        }
        @Override public void failure(Exception exception) {
          callback.failure(exception.getMessage());
        }
      };
    }

    RelayOk relayOk = scriptManager.getScripts(innerCallback, callbackSemaphore);

    callbackSemaphore.acquireDefault(relayOk);
  }

  @Override
  public RelayOk setBreakpoint(Breakpoint.Target target, int line, int column,
      boolean enabled, String condition,
      BreakpointCallback callback, SyncCallback syncCallback) {
    return breakpointManager.setBreakpoint(target, line, column, enabled, condition,
        callback, syncCallback);
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
  public RelayOk listBreakpoints(ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    if (callback != null) {
      callback.success(breakpointManager.getAllBreakpoints());
    }
    return RelaySyncCallback.finish(syncCallback);
  }

  @Override
  public WipBrowser getBrowser() {
    return browserImpl;
  }

  @Override
  public WipJavascriptVm getJavascriptVm() {
    return this;
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
    // TODO: get protocol declare this default value explicitly.
    private static final boolean DEFAULT_BREAKPOINTS_ACTIVE = true;

    // TODO: get protocol declare this default value explicitly.
    private static final ExceptionCatchMode DEFAULT_CATCH_MODE = ExceptionCatchMode.NONE;

    boolean breakpointsActive = DEFAULT_BREAKPOINTS_ACTIVE;

    // TODO: do we know default value?
    ExceptionCatchMode breakOnExceptionMode = DEFAULT_CATCH_MODE;

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

    static final Variable<ExceptionCatchMode> BREAK_ON_EXCEPTION =
        new Variable<ExceptionCatchMode>() {
      @Override ExceptionCatchMode getValue(VmState vmState) {
        return vmState.breakOnExceptionMode;
      }
      @Override void setValue(VmState vmState, ExceptionCatchMode value) {
        vmState.breakOnExceptionMode = value;
      }
      @Override WipParams createRequestParams(VmState vmState) {
        return vmState.createPauseOnExceptionRequest();
      }
    };

    private SetPauseOnExceptionsParams createPauseOnExceptionRequest() {
      SetPauseOnExceptionsParams.State state = SDK_TO_WIP_CATCH_MODE.get(breakOnExceptionMode);
      return new SetPauseOnExceptionsParams(state);
    }

    private static Map<ExceptionCatchMode, SetPauseOnExceptionsParams.State> SDK_TO_WIP_CATCH_MODE;
    static {
      SDK_TO_WIP_CATCH_MODE = new EnumMap<ExceptionCatchMode, SetPauseOnExceptionsParams.State>(
          ExceptionCatchMode.class);

      SDK_TO_WIP_CATCH_MODE.put(ExceptionCatchMode.ALL, SetPauseOnExceptionsParams.State.ALL);
      SDK_TO_WIP_CATCH_MODE.put(ExceptionCatchMode.UNCAUGHT,
          SetPauseOnExceptionsParams.State.UNCAUGHT);
      SDK_TO_WIP_CATCH_MODE.put(ExceptionCatchMode.NONE, SetPauseOnExceptionsParams.State.NONE);

      assert SDK_TO_WIP_CATCH_MODE.size() == ExceptionCatchMode.values().length;
    }
  }
}
