// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.StreamListener;
import org.chromium.sdk.util.SignalRelay;

/**
 * Wraps net socket and provides loggable reader and loggable writer to facilitate optional
 * traffic dumping.
 * @param <INPUT_WRAPPER> type of input stream wrapper
 * @param <OUTPUT_WRAPPER> type of output stream wrapper
 */
public abstract class AbstractSocketWrapper<INPUT_WRAPPER, OUTPUT_WRAPPER> {
  private final Socket socket;

  private final INPUT_WRAPPER loggableInput;
  private final OUTPUT_WRAPPER loggableOutput;

  public AbstractSocketWrapper(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger, WrapperFactory<INPUT_WRAPPER, OUTPUT_WRAPPER> factory)
      throws IOException {
    this.socket = new Socket();
    this.socket.connect(endpoint, connectionTimeoutMs);

    INPUT_WRAPPER originalLogReader = factory.wrapInputStream(socket.getInputStream());

    OUTPUT_WRAPPER originalLogWriter = factory.wrapOutputStream(socket.getOutputStream());

    if (connectionLogger == null) {
      loggableInput = originalLogReader;
      loggableOutput = originalLogWriter;
    } else {
      loggableInput = factory.wrapInputStream(originalLogReader,
          connectionLogger.getIncomingStreamListener());

      loggableOutput = factory.wrapOutputStream(originalLogWriter,
          connectionLogger.getOutgoingStreamListener());

      connectionLogger.setConnectionCloser(new ConnectionLogger.ConnectionCloser() {
        @Override public void closeConnection() {
          shutdownRelay.sendSignal(null, new Exception("Close requested from logger UI"));
        }
      });
    }
  }

  public INPUT_WRAPPER getLoggableInput() {
    return loggableInput;
  }

  public OUTPUT_WRAPPER getLoggableOutput() {
    return loggableOutput;
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

  /**
   * Contains a set of abstract methods that cannot belong to {@link AbstractSocketWrapper}
   * itself because they are needed from constructor.
   * @param <IW>
   * @param <OW>
   */
  protected interface WrapperFactory<IW, OW> {
    IW wrapInputStream(InputStream inputStream);
    OW wrapOutputStream(OutputStream outputStream);

    IW wrapInputStream(IW originalInputWrapper, StreamListener streamListener);
    OW wrapOutputStream(OW originalOutputWrapper, StreamListener streamListener);
  }

  private final SignalRelay<ShutdownSignal> shutdownRelay =
      SignalRelay.create(new SignalRelay.Callback<ShutdownSignal>() {
    @Override
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
