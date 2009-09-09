// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.DebugSessionManager;
import org.chromium.sdk.internal.JavascriptVmImpl;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.ProtocolOptions;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.transport.Handshaker;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.SocketConnection;
import org.chromium.sdk.internal.transport.Connection.NetListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * Operates session of debugging of standalone V8 virtual machine.
 */
public class StandaloneVmSessionManager implements DebugSessionManager {

  /** The class logger. */
  private static final Logger LOGGER =
      Logger.getLogger(StandaloneVmSessionManager.class.getName());

  private static final int WAIT_FOR_HANDSHAKE_TIMEOUT_MS = 3000;

  private static final ProtocolOptions PROTOCOL_OPTIONS = new ProtocolOptions() {
    public boolean requireDataField() {
      return false;
    }
  };

  private final SocketConnection connection;
  private final DebugSession debugSession;
  private final Handshaker.StandaloneV8 handshaker;
  private DebugEventListener debugEventListener = null;
  private volatile AbstractConnectionState connectionState = INIT_STATE;

  private volatile Exception disconnectReason = null;
  private volatile Handshaker.StandaloneV8.RemoteInfo savedRemoteInfo = NULL_REMOTE_INFO;

  private final Object disconnectMonitor = new Object();

  public StandaloneVmSessionManager(JavascriptVmImpl javascriptVmImpl, SocketConnection connection,
      Handshaker.StandaloneV8 handshaker) {
    this.connection = connection;
    this.handshaker = handshaker;
    this.debugSession = new DebugSession(javascriptVmImpl, PROTOCOL_OPTIONS,
        v8CommandOutput);
  }

  public void attach(DebugEventListener listener) throws IOException, UnsupportedVersionException {
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
        connectionState = DETACHED_STATE;
        connection.close();
      }
    }
  }

  private void attachImpl(DebugEventListener listener) throws IOException,
      UnsupportedVersionException {
    connectionState = CONNECTING_STATE;

    NetListener netListener = new NetListener() {
      public void connectionClosed() {
        onDebuggerDetachedImpl(null);
      }

      public void eosReceived() {
        debugSession.getV8CommandProcessor().processEos();
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

    connectionState = EXPECTING_HANDSHAKE_STATE;

    Handshaker.StandaloneV8.RemoteInfo remoteInfo;
    try {
      remoteInfo = handshaker.getRemoteInfo().get(WAIT_FOR_HANDSHAKE_TIMEOUT_MS,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new IOException("Failed to get version", e);
    } catch (TimeoutException e) {
      throw new IOException("Timeout in waiting for version", e);
    }

    String versionString = remoteInfo.getProtocolVersion();
    // TODO(peter.rybin): check version here
    if (versionString == null) {
      throw new UnsupportedVersionException(null, null);
    }

    this.savedRemoteInfo = remoteInfo;

    this.debugEventListener = listener;

    connectionState = CONNECTED_STATE;
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
    return connectionState.isAttached();
  }

  private boolean onDebuggerDetachedImpl(Exception cause) {
    synchronized (disconnectMonitor) {
      if (!connectionState.isAttached()) {
        // We've already been notified.
        return false;
      }
      connectionState = DETACHED_STATE;
      disconnectReason = cause;
    }
    DebugEventListener debugEventListener = getDebugEventListener();
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
    return true;
  }


  public DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }

  public void onDebuggerDetached() {
    onDebuggerDetachedImpl(null);
  }

  private final V8CommandOutput v8CommandOutput = new V8CommandOutput() {
    public void send(DebuggerMessage debuggerMessage, boolean immediate) {
      String jsonString = JsonUtil.streamAwareToJson(debuggerMessage);
      Message message = new Message(Collections.<String, String>emptyMap(), jsonString);

      connection.send(message);
      // TODO(peter.rybin): support {@code immediate} in protocol
    }
  };

  public DebugSession getDebugSession() {
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


  // TODO(peter.rybin): add data fields to this type or convert it to enum
  private static abstract class AbstractConnectionState {
    abstract boolean isAttached();
  }

  private static final AbstractConnectionState INIT_STATE = new AbstractConnectionState() {
    @Override
    boolean isAttached() {
      return false;
    }
  };

  private static final AbstractConnectionState CONNECTING_STATE = new AbstractConnectionState() {
    @Override
    boolean isAttached() {
      return false;
    }
  };

  private static final AbstractConnectionState EXPECTING_HANDSHAKE_STATE =
      new AbstractConnectionState() {
    @Override
    boolean isAttached() {
      return false;
    }
  };

  private static final AbstractConnectionState CONNECTED_STATE = new AbstractConnectionState() {
    @Override
    boolean isAttached() {
      return true;
    }
  };

  private static final AbstractConnectionState DETACHED_STATE = new AbstractConnectionState() {
    @Override
    boolean isAttached() {
      return false;
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

}
