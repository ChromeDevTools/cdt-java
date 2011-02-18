// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.util.ByteToCharConverter;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ITerminate;

/**
 * Connection logger that writes both incoming and outgoing streams into
 * logWriter with simple annotations.
 */
public class ConnectionLoggerImpl implements ConnectionLogger {
  /**
   * Additional interface logger sends its output to.
   */
  public interface LogLifecycleListener {
    /**
     * Notifies about logging start. Before this call {@link ConnectionLoggerImpl}
     * is considered to be simply garbage-collectible. After this call
     * {@link ConnectionLoggerImpl} must call {@link #logClosed()}.
     *
     * @param connectionLogger instance of host {@link ConnectionLoggerImpl}, which is nice
     *        to have because theoretically we may receive this call before constructor of
     *        {@link ConnectionLoggerImpl} returned
     */
    void logStarted(ConnectionLoggerImpl connectionLogger);

    /**
     * Notifies about log stream being closed. Technically, last messages may arrive
     * even after this. It is supposed that log representation may be closed on this call
     * because we are not 100% accurate.
     */
    void logClosed();
  }


  public ConnectionLoggerImpl(Writer logWriter, LogLifecycleListener lifecycleListener) {
    this.logWriter = logWriter;
    this.lifecycleListener = lifecycleListener;
  }

  /**
   * We mix 2 streams into a single console. This type helps to annotate them textually.
   */
  private interface StreamId {
    String getStreamName();
  }

  public LoggableWriter wrapWriter(final LoggableWriter originalLoggableWriter,
      final Charset charset) {
    final StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_SentToChrome;
      }
    };
    final OutputStream originalOutputStream = originalLoggableWriter.getOutputStream();
    final OutputStream wrappedOutputStream = new OutputStream() {
      private final ByteToCharConverter byteToCharConverter = new ByteToCharConverter(charset);

      @Override
      public void close() throws IOException {
        originalOutputStream.close();
        flushLogWriter();
      }

      @Override
      public void flush() throws IOException {
        originalOutputStream.flush();
        flushLogWriter();
      }

      @Override
      public void write(int b) throws IOException {
        originalOutputStream.write(b);
      }

      @Override
      public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
      }

      @Override
      public void write(byte[] buf, int off, int len) throws IOException {
        originalOutputStream.write(buf, off, len);
        CharBuffer charBuffer = byteToCharConverter.convert(ByteBuffer.wrap(buf, off, len));
        writeToLog(charBuffer, streamId);
      }
    };
    return new LoggableWriter() {
      public OutputStream getOutputStream() {
        return wrappedOutputStream;
      }
      public void markSeparatorForLog() {
        writeToLog(MESSAGE_SEPARATOR, streamId);
        flushLogWriter();
      }
    };
  }

  public LoggableReader wrapReader(final LoggableReader loggableReader, final Charset charset) {
    final StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_ReceivedFromChrome;
      }
    };

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
          writeToLog(charBuffer, streamId);
          flushLogWriter();
        }
        return res;
      }
    };
    return new LoggableReader() {
      public InputStream getInputStream() {
        return wrappedInputStream;
      }

      public void markSeparatorForLog() {
        writeToLog(MESSAGE_SEPARATOR, streamId);
        flushLogWriter();
      }
    };
  }

  public void start() {
    lifecycleListener.logStarted(this);
  }

  public void handleEos() {
    isClosed = true;
    lifecycleListener.logClosed();
  }

  public ITerminate getConnectionTerminate() {
    return connectionTerminate;
  }

  public void setConnectionCloser(ConnectionCloser connectionCloser) {
    this.connectionCloser = connectionCloser;
  }

  private synchronized void writeToLog(String str, StreamId streamId) {
    try {
      printHead(streamId);
      logWriter.append(str);
    } catch (IOException e) {
      DebugPlugin.log(e);
    }
  }
  private synchronized void writeToLog(CharBuffer buf, StreamId streamId) {
    try {
      printHead(streamId);
      logWriter.write(buf.array(), buf.arrayOffset(), buf.remaining());
    } catch (IOException e) {
      DebugPlugin.log(e);
    }
  }
  private void printHead(StreamId streamId) throws IOException {
    if (lastSource != streamId) {
      if (lastSource != null) {
        logWriter.append('\n');
      }
      logWriter.append("> ").append(streamId.getStreamName()).append('\n'); //$NON-NLS-1$
      lastSource = streamId;
    }
  }
  private void flushLogWriter() {
    try {
      logWriter.flush();
    } catch (IOException e) {
      DebugPlugin.log(e);
    }
  }

  private final Writer logWriter;
  private final LogLifecycleListener lifecycleListener;
  private StreamId lastSource = null;
  private volatile ConnectionCloser connectionCloser = null;
  private volatile boolean isClosed = false;

  private final ITerminate connectionTerminate = new ITerminate() {
    public boolean canTerminate() {
      return !isClosed && connectionCloser != null;
    }

    public boolean isTerminated() {
      return isClosed;
    }

    public void terminate() {
      ConnectionCloser connectionCloser0 = ConnectionLoggerImpl.this.connectionCloser;
      if (connectionCloser0 == null) {
        throw new IllegalStateException();
      }
      connectionCloser0.closeConnection();
    }
  };

  private static final String MESSAGE_SEPARATOR = Messages.ConnectionLoggerImpl_MessageSeparator;
}
