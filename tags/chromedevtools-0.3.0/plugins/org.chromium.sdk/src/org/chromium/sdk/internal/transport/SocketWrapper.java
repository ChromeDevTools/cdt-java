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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.StreamListener;
import org.chromium.sdk.util.ByteToCharConverter;
import org.chromium.sdk.util.SignalRelay;

/**
 * A wrapper around platform socket that handles logging and closing.
 */
public class SocketWrapper {
  private final Socket socket;

  private final LoggableInputStream loggableInput;
  private final LoggableOutputStream loggableOutput;

  /**
   * @param charset that should be used for log presentation.
   */
  public SocketWrapper(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger, Charset charset) throws IOException {
    this.socket = new Socket();
    this.socket.connect(endpoint, connectionTimeoutMs);

    LoggableInputStream originalLogReader = new LoggableInputStream() {
      final InputStream inputStream = socket.getInputStream();
      @Override public InputStream getInputStream() {
        return inputStream;
      }
      @Override public void markSeparatorForLog() {
        // No-op.
      }
    };

    LoggableOutputStream originalLogWriter = new LoggableOutputStream() {
      final OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
      @Override public OutputStream getOutputStream() {
        return outputStream;
      }
      @Override public void markSeparatorForLog() {
        // No-op.
      }
    };

    if (connectionLogger == null) {
      loggableInput = originalLogReader;
      loggableOutput = originalLogWriter;
    } else {
      loggableInput = wrapReader(originalLogReader, charset,
          connectionLogger.getIncomingStreamListener());

      loggableOutput = wrapWriter(originalLogWriter, charset,
          connectionLogger.getOutgoingStreamListener());

      connectionLogger.setConnectionCloser(new ConnectionLogger.ConnectionCloser() {
        @Override public void closeConnection() {
          shutdownRelay.sendSignal(null, new Exception("Close requested from logger UI"));
        }
      });
    }
  }

  public LoggableInputStream getLoggableInput() {
    return loggableInput;
  }

  public LoggableOutputStream getLoggableOutput() {
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
   * Reader that allows client to add marks to stream. These marks may become visible in log
   * console.
   */
  public static abstract class LoggableInputStream {
    public abstract InputStream getInputStream();

    /**
     * Add log mark at current reader's position.
     */
    public abstract void markSeparatorForLog();
  }

  /**
   * Writer that allows client to add marks to stream. These marks may become visible in log
   * console.
   */
  public static abstract class LoggableOutputStream {
    public abstract OutputStream getOutputStream();

    /**
     * Add log mark at current writer's position.
     */
    public abstract void markSeparatorForLog();
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

  /**
   * Wraps original {@link LoggableOutputStream} with another one that delegates all calls to the
   * original one but additionally sends all the data to StreamListener.
   * @param originalLoggableWriter stream that has to be wrapped
   * @param charset that is used to convert bytes to characters in log
   * @param listener log listener or null
   * @return wrapped stream or original stream if listener is null
   */
  public static LoggableOutputStream wrapWriter(final LoggableOutputStream originalLoggableWriter,
      final Charset charset, final StreamListener listener) {
    if (listener == null) {
      return originalLoggableWriter;
    }
    final OutputStream originalOutputStream = originalLoggableWriter.getOutputStream();
    final OutputStream wrappedOutputStream = new OutputStream() {
      private final ByteToCharConverter byteToCharConverter = new ByteToCharConverter(charset);

      @Override
      public void close() throws IOException {
        originalOutputStream.close();
      }

      @Override
      public void flush() throws IOException {
        originalOutputStream.flush();
      }

      @Override
      public void write(int b) throws IOException {
        originalOutputStream.write(b);
        writeToListener(ByteBuffer.wrap(new byte[] { (byte) b }));
      }

      @Override
      public void write(byte[] b) throws IOException {
        writeImpl(b, 0, b.length);
      }

      @Override
      public void write(byte[] buf, int off, int len) throws IOException {
        writeImpl(buf, off, len);
      }

      private void writeImpl(byte[] buf, int off, int len) throws IOException {
        originalOutputStream.write(buf, off, len);
        writeToListener(ByteBuffer.wrap(buf, off, len));
      }

      private void writeToListener(ByteBuffer byteBuffer) {
        CharBuffer charBuffer = byteToCharConverter.convert(byteBuffer);
        listener.addContent(charBuffer);
      }
    };
    return new LoggableOutputStream() {
      @Override public OutputStream getOutputStream() {
        return wrappedOutputStream;
      }
      @Override public void markSeparatorForLog() {
        listener.addSeparator();
      }
    };
  }

  /**
   * Wraps original {@link LoggableInputStream} with another one that delegates all calls to the
   * original one but additionally sends all the data to StreamListener.
   * @param originalLoggableReader stream that has to be wrapped
   * @param charset that is used to convert bytes to characters in log
   * @param listener log listener or null
   * @return wrapped stream or original stream if listener is null
   */
  public static LoggableInputStream wrapReader(final LoggableInputStream loggableReader,
      final Charset charset, final StreamListener listener) {
    final InputStream originalInputStream = loggableReader.getInputStream();

    final InputStream wrappedInputStream = new InputStream() {
      private final ByteToCharConverter byteToCharConverter = new ByteToCharConverter(charset);
      @Override
      public int read() throws IOException {
        byte[] buffer = new byte[1];
        int res = readImpl(buffer, 0, 1);
        if (res <= 0) {
          return -1;
        } else {
          return buffer[0];
        }
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return readImpl(b, off, len);
      }

      private int readImpl(byte[] buf, int off, int len) throws IOException {
        int res = originalInputStream.read(buf, off, len);
        if (res > 0) {
          CharBuffer charBuffer = byteToCharConverter.convert(ByteBuffer.wrap(buf, off, res));
          listener.addContent(charBuffer);
        }
        return res;
      }
    };
    return new LoggableInputStream() {
      @Override public InputStream getInputStream() {
        return wrappedInputStream;
      }

      @Override public void markSeparatorForLog() {
        listener.addSeparator();
      }
    };
  }
}
