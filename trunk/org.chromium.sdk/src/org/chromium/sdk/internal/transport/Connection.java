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
     *
     * @param message from the browser instance the connection is associated
     *        with
     */
    void messageReceived(Message message);

    /**
     * Gets invoked when the physical connection has been terminated.
     */
    void connectionClosed();
  }

  /**
   * Objects of this class should be returned by any
   * {@link Connection#getEndpoint()} implementation.
   */
  public class Endpoint {

    private final String host;

    private final int port;

    public Endpoint(String host, int port) {
      this.host = host;
      this.port = port;
    }

    /**
     * @return the host of this endpoint
     */
    public String getHost() {
      return host;
    }

    /**
     * @return the port of this endpoint
     */
    public int getPort() {
      return port;
    }

    @Override
    public int hashCode() {
      return host.hashCode() + port;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Endpoint that = (Endpoint) obj;
      return this.port == that.port && this.host.equals(that.host);
    }

  }

  /**
   * Sets a callback that will be notified of network events. The callback will
   * be set before calling {@link #start()} and it cannot be changed over the
   * connection lifetime (even though it should be possible to set the same
   * listener once again.
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

  /**
   * Returns an Endpoint specifying which host and port this connection is
   * associated with.
   *
   * @return an Endpoint instance describing this connection
   */
  Endpoint getEndpoint();
}
