// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Logger facility for the Chromium debugger connection. It can eavesdrop both
 * incoming and outgoing streams and log them somewhere.
 * {@link ConnectionLogger} instance is <strong>not</strong> reconnectable.
 */
public interface ConnectionLogger {

  /**
   * Listens to stream data traffic. Traffic is a series of {@link CharSequence}s and
   * separators. It is a connection's responsibility to convert actual bytes transmitted into
   * characters and separators. Connection should try to make output human-readable.
   * All calls to the interface must be serialized (for example, but not necessarily, made from
   * a single thread).
   */
  interface StreamListener {
    void addContent(CharSequence text);
    void addSeparator();
  }

  /**
   * @return listener for incoming socket stream or null
   */
  StreamListener getIncomingStreamListener();

  /**
   * @return listener for outgoing socket stream or null
   */
  StreamListener getOutgoingStreamListener();

  /**
   * Connection may allow the logger to close it. It is nice for UI, where
   * user sees logger and the corresponding stop button.
   */
  // TODO: consider removing it out of logging.
  void setConnectionCloser(ConnectionCloser connectionCloser);

  /**
   * Interface that gives you control over underlying connection.
   */
  interface ConnectionCloser {
    void closeConnection();
  }

  /**
   * Notifies logger that actual transmission is starting. After this {@link #handleEos()}
   * is guaranteed to be called.
   */
  void start();

  /**
   * Notifies logger that EOS has been received from remote. Technically some
   * traffic still may go through writer (i.e. be sent to remote) after this.
   */
  void handleEos();

  /**
   * Factory for connection logger.
   */
  interface Factory {
    /**
     * Creates new instance of {@link ConnectionLogger}.
     */
    ConnectionLogger newConnectionLogger();
  }
}
