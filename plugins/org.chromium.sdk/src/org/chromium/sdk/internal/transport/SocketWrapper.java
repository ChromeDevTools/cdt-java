// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.StreamListener;
import org.chromium.sdk.util.ByteToCharConverter;

/**
 * A wrapper around platform socket that handles logging and closing.
 * TODO: consider renaming into AutoLoggingSocketWrapper
 */
public class SocketWrapper extends AbstractSocketWrapper<SocketWrapper.LoggableInputStream,
        SocketWrapper.LoggableOutputStream> {
  public SocketWrapper(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger, Charset charset) throws IOException {
    super(endpoint, connectionTimeoutMs, connectionLogger, new FactoryImpl(charset));
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

  private static class FactoryImpl
      implements WrapperFactory<LoggableInputStream, LoggableOutputStream> {
    private final Charset charset;

    FactoryImpl(Charset charset) {
      this.charset = charset;
    }

    @Override
    public LoggableInputStream wrapInputStream(final InputStream inputStream) {
      return new LoggableInputStream() {
        @Override public InputStream getInputStream() {
          return inputStream;
        }
        @Override public void markSeparatorForLog() {
          // No-op.
        }
      };
    }

    @Override
    public LoggableOutputStream wrapOutputStream(final OutputStream outputStream) {
      return new LoggableOutputStream() {
        @Override public OutputStream getOutputStream() {
          return outputStream;
        }
        @Override public void markSeparatorForLog() {
          // No-op.
        }
      };
    }

    /**
     * Wraps original {@link LoggableInputStream} with another one that delegates all calls to the
     * original one but additionally sends all the data to StreamListener.
     *
     * @param originalLoggableReader stream that has to be wrapped
     * @param charset that is used to convert bytes to characters in log
     * @param listener log listener or null
     * @return wrapped stream or original stream if listener is null
     */
    @Override
    public LoggableInputStream wrapInputStream(final LoggableInputStream loggableReader,
        final StreamListener listener) {
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

    /**
     * Wraps original {@link LoggableOutputStream} with another one that delegates all calls to the
     * original one but additionally sends all the data to StreamListener.
     *
     * @param originalLoggableWriter stream that has to be wrapped
     * @param charset that is used to convert bytes to characters in log
     * @param listener log listener or null
     * @return wrapped stream or original stream if listener is null
     */
    @Override
    public LoggableOutputStream wrapOutputStream(final LoggableOutputStream originalLoggableWriter,
        final StreamListener listener) {
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
  }
}
