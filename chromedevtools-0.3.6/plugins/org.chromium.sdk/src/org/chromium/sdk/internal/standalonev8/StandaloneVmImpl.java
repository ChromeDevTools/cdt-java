// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.standalonev8;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.transport.Connection;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.SocketConnection;
import org.chromium.sdk.internal.transport.Connection.NetListener;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.DebugSessionManager;
import org.chromium.sdk.internal.v8native.JavascriptVmImpl;
import org.chromium.sdk.internal.v8native.V8CommandOutput;
import org.chromium.sdk.internal.v8native.V8ContextFilter;
import org.chromium.sdk.internal.v8native.protocol.input.data.ContextHandle;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.util.MethodIsBlockingException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Implementation of {@code StandaloneVm}. Currently knows nothing about
 * contexts, so all existing V8 contexts are presented mixed together.
 */
public class StandaloneVmImpl extends JavascriptVmImpl implements StandaloneVm {

  /** The class logger. */
  private static final Logger LOGGER =
      Logger.getLogger(StandaloneVmImpl.class.getName());

  private static final int WAIT_FOR_HANDSHAKE_TIMEOUT_MS = 3000;

  private static final V8ContextFilter CONTEXT_FILTER = new V8ContextFilter() {
    public boolean isContextOurs(ContextHandle contextHandle) {
      // We do not check context in standalone V8 mode.
      return true;
    }
  };

  private final SocketConnection connection;
  private final Handshaker.StandaloneV8 handshaker;

  private final DebugSession debugSession;

  private DebugEventListener debugEventListener = null;
  private volatile ConnectionState connectionState = ConnectionState.INIT;

  private volatile Exception disconnectReason = null;
  private volatile Handshaker.StandaloneV8.RemoteInfo savedRemoteInfo = NULL_REMOTE_INFO;

  private final Object disconnectMonitor = new Object();

  public StandaloneVmImpl(SocketConnection connection, Handshaker.StandaloneV8 handshaker) {
    this.connection = connection;
    this.handshaker = handshaker;
    V8CommandOutputImpl v8CommandOutput = new V8CommandOutputImpl(connection);
    this.debugSession = new DebugSession(sessionManager, CONTEXT_FILTER, v8CommandOutput, this);
  }

  public void attach(DebugEventListener listener)
      throws IOException, UnsupportedVersionException, MethodIsBlockingException {
    Exception errorCause = null;
    try {
      attachImpl(listener);
    } catch (IOException e) {
      errorCause = e;
      throw e;
    } catch (UnsupportedVersionException e) {
      errorCause = e;
      throw e;
    } finally {
      if (errorCause != null) {
        disconnectReason = errorCause;
        connectionState = ConnectionState.DETACHED;
        connection.close();
      }
    }
  }

  private void attachImpl(DebugEventListener listener) throws IOException,
      UnsupportedVersionException, MethodIsBlockingException {
    connectionState = ConnectionState.CONNECTING;

    NetListener netListener = new NetListener() {
      public void connectionClosed() {
      }

      public void eosReceived() {
        debugSession.getV8CommandProcessor().processEos();
        onDebuggerDetachedImpl(null);
      }

      public void messageReceived(Message message) {
        JSONObject json;
        try {
          json = JsonUtil.jsonObjectFromJson(message.getContent());
        } catch (ParseException e) {
          LOGGER.log(Level.SEVERE, "Invalid JSON received: {0}", message.getContent());
          return;
        }
        debugSession.getV8CommandProcessor().processIncomingJson(json);
      }
    };
    connection.setNetListener(netListener);

    connection.start();

    connectionState = ConnectionState.EXPECTING_HANDSHAKE;

    Handshaker.StandaloneV8.RemoteInfo remoteInfo;
    try {
      remoteInfo = handshaker.getRemoteInfo().get(WAIT_FOR_HANDSHAKE_TIMEOUT_MS,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw newIOException("Failed to get version", e);
    } catch (TimeoutException e) {
      throw newIOException("Timed out waiting for handshake", e);
    }

    String versionString = remoteInfo.getProtocolVersion();
    // TODO(peter.rybin): check version here
    if (versionString == null) {
      throw new UnsupportedVersionException(null, null);
    }

    StandaloneVmImpl.this.savedRemoteInfo = remoteInfo;

    StandaloneVmImpl.this.debugEventListener = listener;

    debugSession.startCommunication();

    connectionState = ConnectionState.CONNECTED;
  }

  public boolean detach() {
    boolean res = onDebuggerDetachedImpl(null);
    if (!res) {
      return false;
    }
    connection.close();
    return true;
  }

  public boolean isAttached() {
    return connectionState == ConnectionState.CONNECTED;
  }

  private boolean onDebuggerDetachedImpl(Exception cause) {
    synchronized (disconnectMonitor) {
      if (!isAttached()) {
        // We've already been notified.
        return false;
      }
      connectionState = ConnectionState.DETACHED;
      disconnectReason = cause;
    }
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
    return true;
  }

  @Override
  protected DebugSession getDebugSession() {
    return debugSession;
  }

  /**
   * @return name of embedding application as it wished to name itself; might be null
   */
  public String getEmbedderName() {
    return savedRemoteInfo.getEmbeddingHostName();
  }

  /**
   * @return version of V8 implementation, format is unspecified; not null
   */
  public String getVmVersion() {
    return savedRemoteInfo.getV8VmVersion();
  }

  public String getDisconnectReason() {
    // Save volatile field in local variable.
    Exception cause = disconnectReason;
    if (cause == null) {
      return null;
    }
    return cause.getMessage();
  }

  private final DebugSessionManager sessionManager = new DebugSessionManager() {
    public DebugEventListener getDebugEventListener() {
      return debugEventListener;
    }

    public void onDebuggerDetached() {
      // Never called for standalone.
    }
  };

  private final static Handshaker.StandaloneV8.RemoteInfo NULL_REMOTE_INFO =
      new Handshaker.StandaloneV8.RemoteInfo() {
    public String getEmbeddingHostName() {
      return null;
    }
    public String getProtocolVersion() {
      return null;
    }
    public String getV8VmVersion() {
      return null;
    }
  };

  private enum ConnectionState {
    INIT,
    CONNECTING,
    EXPECTING_HANDSHAKE,
    CONNECTED,
    DETACHED
  }

  private static class V8CommandOutputImpl implements V8CommandOutput {
    private final Connection outputConnection;

    V8CommandOutputImpl(Connection outputConnection) {
      this.outputConnection = outputConnection;
    }
    public void send(DebuggerMessage debuggerMessage, boolean immediate) {
      String jsonString = JsonUtil.streamAwareToJson(debuggerMessage);
      Message message = new Message(Collections.<String, String>emptyMap(), jsonString);

      outputConnection.send(message);
      // TODO(peter.rybin): support {@code immediate} in protocol
    }
    public void runInDispatchThread(Runnable callback) {
      outputConnection.runInDispatchThread(callback);
    }
  }
}
