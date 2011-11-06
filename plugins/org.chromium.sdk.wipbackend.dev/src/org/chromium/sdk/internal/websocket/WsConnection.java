// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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