// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.chromium.sdk.ConnectionLogger;
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

  public Writer wrapWriter(final Writer streamWriter) {
    return new Writer() {
      @Override
      public void close() throws IOException {
        streamWriter.close();
        flushLogWriter();
      }
      @Override
      public void flush() throws IOException {
        streamWriter.flush();
        flushLogWriter();
      }
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        streamWriter.write(cbuf, off, len);

        writeToLog(cbuf, off, len, this,
            Messages.ConnectionLoggerImpl_SentToChrome);
      }
    };
  }
  public Reader wrapReader(final Reader streamReader) {
    return new Reader() {
      @Override
      public void close() throws IOException {
        streamReader.close();
        flushLogWriter();
      }

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        int res = streamReader.read(cbuf, off, len);
        if (res > 0) {
          writeToLog(cbuf, off, res, this,
              Messages.ConnectionLoggerImpl_ReceivedFromChrome);
          flushLogWriter();
        }
        return res;
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

  private synchronized void writeToLog(char[] cbuf, int off, int len, Object source,
      String sourceName) {
    try {
      if (lastSource != source) {
        if (lastSource != null) {
          logWriter.append('\n');
        }
        logWriter.append("> ").append(sourceName).append('\n'); //$NON-NLS-1$
        lastSource = source;
      }
      logWriter.write(cbuf, off, len);
    } catch (IOException e) {
      DebugPlugin.log(e);
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
  private Object lastSource = null;
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
}
