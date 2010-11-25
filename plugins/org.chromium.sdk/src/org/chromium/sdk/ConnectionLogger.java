// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Logger facility for the Chromium debugger connection. It can eavesdrop both
 * incoming and outgoing streams and log them somewhere. To make other code
 * less dependent on this interface, it works by wrapping reader/writer and is
 * only visible at start-up time. This approach has its disadvantages, because
 * it works with raw data streams, which are not perfectly formatted for human
 * reading. E.g. adjacent  requests or responses are not separated even
 * by EOL.
 */
public interface ConnectionLogger {
  /**
   * @return new writer that should pass all data to {@code streamWriter} and
   * silently copy it elsewhere (without additional exceptions).
   */
  LoggableWriter wrapWriter(LoggableWriter streamWriter);

  /**
   * @return new reader that should give access to all data
   * from {@code streamReader} and silently copy it elsewhere (without
   * additional exceptions).
   */
  LoggableReader wrapReader(LoggableReader streamReader);

  /**
   * Connection may allow the logger to close it. It is nice for UI, where
   * user sees logger and the corresponding stop button.
   * TODO(peter.rybin): consider removing it out of logging.
   */
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
   * Factory for connection logger. ConnectionLogger is NOT reconnectable.
   */
  interface Factory {
    /**
     * Creates new instance of {@link ConnectionLogger}.
     */
    ConnectionLogger newConnectionLogger();
  }

  /**
   * Reader that allows client to add marks to stream. These marks may become visible in log
   * console.
   * TODO(peter.rybin): rename to LoggableInputStream to better reflect semantics.
   */
  interface LoggableReader {
    InputStream getInputStream();

    /**
     * Add log mark at current reader's position.
     */
    void markSeparatorForLog();
  }

  /**
   * Writer that allows client to add marks to stream. These marks may become visible in log
   * console.
   * TODO(peter.rybin): rename to LoggableOutputStream to better reflect semantics.
   */
  interface LoggableWriter {
    OutputStream getOutputStream();

    /**
     * Add log mark at current writer's position.
     */
    void markSeparatorForLog();
  }
}
