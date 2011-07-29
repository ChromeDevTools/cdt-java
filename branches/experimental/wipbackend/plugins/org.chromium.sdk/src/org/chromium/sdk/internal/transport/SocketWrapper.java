// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.util.SignalRelay;

/**
 * A wrapper around platform socket that handles logging and closing.
 */
public class SocketWrapper {
  private final Socket socket;

  private final ConnectionLogger.LoggableReader loggableReader;
  private final ConnectionLogger.LoggableWriter loggableWriter;

  /**
   * @param charset that should be used for log presentation.
   */
  public SocketWrapper(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger, Charset charset) throws IOException {
    this.socket = new Socket();
    this.socket.connect(endpoint, connectionTimeoutMs);

    ConnectionLogger.LoggableReader originalLogReader = new ConnectionLogger.LoggableReader() {
      final InputStream inputStream = socket.getInputStream();
      public InputStream getInputStream() {
        return inputStream;
      }
      public void markSeparatorForLog() {
        // No-op.
      }
    };

    ConnectionLogger.LoggableWriter originalLogWriter = new ConnectionLogger.LoggableWriter() {
      final OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
      public OutputStream getOutputStream() {
        return outputStream;
      }
      public void markSeparatorForLog() {
        // No-op.
      }
    };

    if (connectionLogger == null) {
      loggableReader = originalLogReader;
      loggableWriter = originalLogWriter;
    } else {
      loggableReader = connectionLogger.wrapReader(originalLogReader, charset);
      loggableWriter = connectionLogger.wrapWriter(originalLogWriter, charset);
      connectionLogger.setConnectionCloser(new ConnectionLogger.ConnectionCloser() {
        public void closeConnection() {
          shutdownRelay.sendSignal(null, new Exception("Close requested from logger UI"));
        }
      });
    }
  }

  public ConnectionLogger.LoggableReader getLoggableReader() {
    return loggableReader;
  }

  public ConnectionLogger.LoggableWriter getLoggableWriter() {
    return loggableWriter;
  }

  public SignalRelay<ShutdownSignal> getShutdownRelay() {
    return shutdownRelay;
  }

  /**
   * More a symbolic type than a really used type. Later it may gain some members.
   * It is created because we need a generic parameter for SignalRelay.
   */
  public interface ShutdownSignal {
  }

  private final SignalRelay<ShutdownSignal> shutdownRelay =
      SignalRelay.create(new SignalRelay.Callback<ShutdownSignal>() {
    public void onSignal(ShutdownSignal param, Exception cause) {
      try {
        closeImpl();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    private void closeImpl() throws IOException {
      try {
        socket.shutdownInput();
      } catch (IOException e) {
        // ignore
      }
      try {
        socket.shutdownOutput();
      } catch (IOException e) {
        // ignore
      }

      try {
        socket.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      socket.close();
    }
  });
}
