// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.io.IOException;
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

  /**
   * We mix 2 streams into a single console. This type helps to annotate them textually.
   */
  private static abstract class StreamId {
    abstract String getStreamName();
  }

  @Override
  public StreamListener getIncomingStreamListener() {
    StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_ReceivedFromChrome;
      }
    };
    return new StreamListenerImpl(streamId);
  }

  @Override
  public StreamListener getOutgoingStreamListener() {
    StreamId streamId = new StreamId() {
      public String getStreamName() {
        return Messages.ConnectionLoggerImpl_SentToChrome;
      }
    };
    return new StreamListenerImpl(streamId);
  }

  private class StreamListenerImpl implements StreamListener {
    private final StreamId streamId;

    private StreamListenerImpl(StreamId streamId) {
      this.streamId = streamId;
    }

    @Override
    public void addContent(CharSequence text) {
      writeToLog(text, streamId);
      flushLogWriter();
    }

    @Override
    public void addSeparator() {
      writeToLog(MESSAGE_SEPARATOR, streamId);
      flushLogWriter();
    }
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

  private synchronized void writeToLog(CharSequence str, StreamId streamId) {
    try {
      printHead(streamId);
      logWriter.append(str);
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
