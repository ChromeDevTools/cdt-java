// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.EvaluateWithContextExtension;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TabDebugEventListener;
import org.chromium.sdk.Version;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.rynda.protocol.BasicConstants;
import org.chromium.sdk.internal.rynda.protocol.output.RyndaArguments;
import org.chromium.sdk.internal.rynda.protocol.output.RyndaRequest;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.internal.websocket.WsConnection;
import org.chromium.sdk.internal.websocket.WsConnection.CloseReason;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * {@link BrowserTab} implementation that attaches to remote tab via WebInspector('Rynda')
 * protocol.
 */
public class RyndaTabImpl implements BrowserTab {
  private static final Logger LOGGER = Logger.getLogger(RyndaTabImpl.class.getName());

  private final WsConnection socket;
  private final RyndaBrowserImpl browserImpl;
  private final TabDebugEventListener tabListener;
  private final RyndaCommandProcessor ryndaCommandProcessor;
  private final RyndaScriptManager scriptManager = new RyndaScriptManager(this);
  private final RyndaContextBuilder contextBuilder = new RyndaContextBuilder(this);

  private volatile String url = "<no url>";

  public RyndaTabImpl(WsConnection socket, RyndaBrowserImpl browserImpl,
      TabDebugEventListener tabListener) throws IOException {
    this.socket = socket;
    this.browserImpl = browserImpl;
    this.tabListener = tabListener;

    ryndaCommandProcessor = new RyndaCommandProcessor(this, socket);

    WsConnection.Listener socketListener = new WsConnection.Listener() {
      @Override
      public void textMessageRecieved(String text) {
        JSONObject json;
        try {
          json = JsonUtil.jsonObjectFromJson(text);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
        ryndaCommandProcessor.acceptResponse(json);
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

    ryndaCommandProcessor.send(new RyndaRequest(BasicConstants.Domain.INSPECTOR,
        "populateScriptObjects", new RyndaArguments()), null, null);

    ryndaCommandProcessor.send(
        new RyndaRequest(BasicConstants.Domain.DEBUGGER, "enable", new RyndaArguments()),
        null, null);
  }

  void updateUrl(String url) {
    this.url = url;
    RyndaTabImpl.this.tabListener.navigated(this.url);
  }

  RyndaScriptManager getScriptManager() {
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

    ryndaCommandProcessor.runInDispatchThread(new Runnable() {
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
    RyndaBrowserImpl.throwUnsupported();
  }

  public void suspend(SuspendCallback callback) {
    RyndaBrowserImpl.throwUnsupported();
  }

  public void listBreakpoints(ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    if (callback != null) {
      callback.failure(new UnsupportedOperationException());
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
    return RyndaBrowserImpl.throwUnsupported();
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

  RyndaContextBuilder getContextBuilder() {
    return contextBuilder;
  }

  RyndaCommandProcessor getCommandProcessor() {
    return ryndaCommandProcessor;
  }

  TabDebugEventListener getTabListener() {
    return tabListener;
  }
}
