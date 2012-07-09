// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.IOException;

/**
 * An interface to be implemented by an agent performing the communications with
 * the debugged browser instance.
 */
public interface Connection {

  /**
   * An interface to be used for notification of messages coming in from the
   * browser.
   */
  public interface NetListener {

    /**
     * Gets invoked whenever a message from the browser arrives.
     * Invoked from DispatchThread.
     * @param message from the browser instance the connection is associated
     *        with
     */
    void messageReceived(Message message);

    /**
     * Gets invoked from DispatchThread whenever EOS message arrives. This method
     * must not be called more than once. Method {@link #messageReceived} must
     * not be called after it.
     */
    void eosReceived();

    /**
     * Gets invoked when the physical connection has been terminated.
     * Called from whatever thread that connection is terminated from.
     */
    void connectionClosed();
  }

  /**
   * Sets a listener that will be notified of network events. The listener must
   * be set before calling {@link #start()} and cannot be changed over the
   * connection lifetime.
   *
   * @param netListener to set
   */
  void setNetListener(NetListener netListener);

  /**
   * Sends the specified message to the associated browser instance.
   *
   * @param message to send
   */
  void send(Message message);

  /**
   * Asynchronously runs the callback in Connection's Dispatch thread.
   */
  void runInDispatchThread(Runnable callback);

  /**
   * Starts up the transport and acquire all needed resources. Does nothing if
   * the connection has already been started.
   *
   * @throws IOException
   */
  void start() throws IOException;

  /**
   * Shuts down the transport freeing all acquired resources. Does nothing if
   * the connection has already been shut down.
   */
  void close();

  /**
   * Determines the connection state.
   *
   * @return whether start() has been successfully invoked and close() has not
   *         been invoked yet
   */
  boolean isConnected();
}
