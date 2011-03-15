// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.EvaluateWithContextExtension;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.websocket.WsConnection.CloseReason;
import org.chromium.sdk.internal.wip.protocol.BasicConstants;
import org.chromium.sdk.internal.wip.protocol.output.WipArguments;
import org.chromium.sdk.internal.wip.protocol.output.WipRequest;
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

  private volatile String url = "<no url>";

  public WipTabImpl(WsConnection socket, WipBrowserImpl browserImpl,
      TabDebugEventListener tabListener) throws IOException {
    this.socket = socket;
    this.browserImpl = browserImpl;
    this.tabListener = tabListener;

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
        // TODO(peter.rybin): implement
      }

      @Override
      public void eofMessage() {
        // TODO(peter.rybin): implement
      }

      @Override
      public void closed(CloseReason reason, Exception cause) {
        // TODO(peter.rybin): implement
      }
    };

    socket.startListening(socketListener);

    init();
  }

  private void init() throws IOException {
    socket.sendTextualMessage("loaded");

    commandProcessor.send(new WipRequest(BasicConstants.Domain.INSPECTOR,
        "populateScriptObjects", new WipArguments()), null, null);

    commandProcessor.send(
        new WipRequest(BasicConstants.Domain.DEBUGGER, "enable", new WipArguments()),
        null, null);
  }

  void updateUrl(String url) {
    this.url = url;
    WipTabImpl.this.tabListener.navigated(this.url);
  }

  WipScriptManager getScriptManager() {
    return scriptManager;
  }

  @Override
  public boolean detach() {
    socket.close();
    return true;
  }

  @Override
  public boolean isAttached() {
    // TODO(peter.rybin): implement
    return true;
  }

  public void enableBreakpoints(Boolean enabled,
      GenericCallback<Boolean> callback, SyncCallback syncCallback) {
    // TODO(peter.rybin): implement
    if (callback != null) {
      throw new UnsupportedOperationException();
    }
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
  }

  public void setBreakOnException(ExceptionCatchType catchType,
      Boolean enabled, GenericCallback<Boolean> callback,
      SyncCallback syncCallback) {
    // TODO(peter.rybin): implement
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
  }

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

    commandProcessor.runInDispatchThread(new Runnable() {
      @Override public void run() {
        try {
          callback.success(scriptManager.getScripts());
        } finally {
          callbackSemaphore.callbackDone(null);
        }
      }
    });
    callbackSemaphore.acquireDefault();
  }

  @Override
  public void setBreakpoint(Type type, String target, int line, int column,
      boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback, SyncCallback syncCallback) {
    breakpointManager.setBreakpoint(type, target, line, column, enabled, condition,
        ignoreCount, callback, syncCallback);
  }

  public void suspend(SuspendCallback callback) {
    WipBrowserImpl.throwUnsupported();
  }

  @Override
  public void listBreakpoints(ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    if (callback != null) {
      // TODO: stub result; implement the true list.
      List<Breakpoint> stubEmptyList = Collections.emptyList();
      callback.success(stubEmptyList);
    }
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
  }

  public void enableBreakpoints(boolean enabled, Void callback,
      SyncCallback syncCallback) {
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
  }

  public Browser getBrowser() {
    return WipBrowserImpl.throwUnsupported();
  }

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

  TabDebugEventListener getTabListener() {
    return tabListener;
  }
}
