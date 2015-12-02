// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.websocket;

import java.io.IOException;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.SignalRelay;

/**
 * Abstract interface to WebSocket implementation that hides a particular specification
 * version.
 */
public interface WsConnection {

  void startListening(Listener listener);

  void sendTextualMessage(String message) throws IOException;

  RelayOk runInDispatchThread(Runnable runnable, SyncCallback syncCallback);

  SignalRelay<?> getCloser();

  interface Listener {
    void textMessageRecieved(String text);

    /**
     * Some non-fatal error happened.
     */
    void errorMessage(Exception ex);

    /**
     * Connection has been closed. Message is called from Dispatch thread.
     */
    void eofMessage();
  }
}