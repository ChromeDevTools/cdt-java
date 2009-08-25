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
import org.chromium.sdk.internal.transport.Handshaker.StandaloneV8.RemoteInfo;
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
  private final Object fieldAccessLock = new Object();
  private boolean isAttached = false;
  private RemoteInfo remoteInfo = null;

  public StandaloneVmSessionManager(JavascriptVmImpl javascriptVmImpl, SocketConnection connection,
      Handshaker.StandaloneV8 handshaker) {
    this.connection = connection;
    this.handshaker = handshaker;
    this.debugSession = new DebugSession(javascriptVmImpl, PROTOCOL_OPTIONS,
        v8CommandOutput);
  }

  public boolean attach(DebugEventListener listener) {
    try {
      attachImpl(listener);
      return true;
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to attach to VM", e);
      return false;
    } catch (UnsupportedVersionException e) {
      LOGGER.log(Level.SEVERE, "Failed to attach to VM", e);
      return false;
    }
  }

  private void attachImpl(DebugEventListener listener) throws IOException,
      UnsupportedVersionException {
    NetListener netListener = new NetListener() {
      public void connectionClosed() {
        onDebuggerDetachedImpl();
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
      public void eosReceived() {
      }
    };
    connection.setNetListener(netListener);

    connection.start();

    RemoteInfo remoteInfo0;
    try {
      remoteInfo0 = handshaker.getRemoteInfo().get(WAIT_FOR_HANDSHAKE_TIMEOUT_MS,
          TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new IOException("Failed to get version", e);
    } catch (TimeoutException e) {
      throw new IOException("Timeout in waiting for version", e);
    }

    String versionString = remoteInfo0.getProtocolVersion();
    // TODO(peter.rybin): check version here
    if (versionString == null) {
      throw new UnsupportedVersionException(null, null);
    }

    synchronized (fieldAccessLock) {
      isAttached  = true;
      this.remoteInfo = remoteInfo0;
    }

    this.debugEventListener = listener;
  }

  public boolean detach() {
    if (!isAttached()) {
      // We've already been notified.
      return false;
    }
    synchronized (fieldAccessLock) {
      isAttached  = false;
    }
    debugSession.getV8CommandProcessor().removeAllCallbacks();
    DebugEventListener debugEventListener = getDebugEventListener();
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
    connection.close();
    return true;
  }

  public boolean isAttached() {
    synchronized (fieldAccessLock) {
      return isAttached;
    }
  }

  private void onDebuggerDetachedImpl() {
    if (!isAttached()) {
      // We've already been notified.
      return;
    }
    synchronized (fieldAccessLock) {
      isAttached = false;
    }
    debugSession.getV8CommandProcessor().removeAllCallbacks();
    DebugEventListener debugEventListener = getDebugEventListener();
    if (debugEventListener != null) {
      debugEventListener.disconnected();
    }
  }


  public DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }

  public void onDebuggerDetached() {
    onDebuggerDetachedImpl();
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

  public RemoteInfo getRemoteInfo() {
    synchronized (fieldAccessLock) {
      if (remoteInfo == null) {
        throw new IllegalStateException();
      }
      return remoteInfo;
    }
  }
}
