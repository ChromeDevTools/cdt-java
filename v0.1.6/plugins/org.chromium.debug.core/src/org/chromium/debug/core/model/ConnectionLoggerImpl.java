// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.io.Writer;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.LineReader;
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

  public LoggableWriter wrapWriter(final LoggableWriter originalLoggableWriter) {
    final StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_SentToChrome;
      }
    };
    final Writer originalWriter = originalLoggableWriter.getWriter();
    final Writer wrappedWriter = new Writer() {
      @Override
      public void close() throws IOException {
        originalWriter.close();
        flushLogWriter();
      }
      @Override
      public void flush() throws IOException {
        originalWriter.flush();
        flushLogWriter();
      }
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        originalWriter.write(cbuf, off, len);
        writeToLog(cbuf, off, len, streamId);
      }
    };
    return new LoggableWriter() {
      public Writer getWriter() {
        return wrappedWriter;
      }
      public void markSeparatorForLog() {
        writeToLog(MESSAGE_SEPARATOR, streamId);
        flushLogWriter();
      }
    };
  }

  public LoggableReader wrapReader(final LoggableReader loggableReader) {
    final StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_ReceivedFromChrome;
      }
    };
    final LineReader streamReader = loggableReader.getReader();
    final LineReader wrappedReader = new LineReader() {
      public int read(char[] cbuf, int off, int len) throws IOException {
        int res = streamReader.read(cbuf, off, len);
        if (res > 0) {
          writeToLog(cbuf, off, res, streamId);
          flushLogWriter();
        }
        return res;
      }

      public String readLine() throws IOException {
        String res = streamReader.readLine();
        if (res != null) {
          writeToLog(res + '\n', streamId);
          flushLogWriter();
        }
        return res;
      }
    };
    return new LoggableReader() {
      public LineReader getReader() {
        return wrappedReader;
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
  private synchronized void writeToLog(char[] cbuf, int off, int len, StreamId streamId) {
    try {
      printHead(streamId);
      logWriter.write(cbuf, off, len);
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
