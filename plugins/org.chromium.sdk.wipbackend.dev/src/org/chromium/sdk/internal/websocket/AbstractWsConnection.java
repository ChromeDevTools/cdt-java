// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.transport.AbstractSocketWrapper;
import org.chromium.sdk.util.SignalRelay;
import org.chromium.sdk.util.SignalRelay.AlreadySignalledException;
import org.chromium.sdk.util.SignalRelay.SignalConverter;

public abstract class AbstractWsConnection<INPUT, OUTPUT> implements WsConnection {
  protected static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
  private static final Logger LOGGER = Logger.getLogger(Hybi00WsConnection.class.getName());
  private final AbstractSocketWrapper<INPUT, OUTPUT> socketWrapper;
  private final ConnectionLogger connectionLogger;
  private volatile boolean isClosingGracefully = false;

  private final BlockingQueue<MessageDispatcher> dispatchQueue =
      new LinkedBlockingQueue<MessageDispatcher>();

  // Access must be synchronized on dispatchQueue.
  private boolean isDispatchQueueClosed = false;

  // Access must be synchronized on this.
  private boolean isOutputClosed = false;

  protected AbstractWsConnection(AbstractSocketWrapper<INPUT, OUTPUT> socketWrapper,
      ConnectionLogger connectionLogger) {
    this.socketWrapper = socketWrapper;
    this.connectionLogger = connectionLogger;
    try {
      linkedCloser.bind(socketWrapper.getShutdownRelay(), null, SOCKET_TO_CONNECTION);
    } catch (AlreadySignalledException e) {
      throw new IllegalStateException(e);
    }
  }

  public enum CloseReason {
    /** Socket has been shut down. */
    CONNECTION_CLOSED,

    /**
     * Some exception has terminated stream read thread.
     * Occasionally {@link #CONNECTION_CLOSED} may be replaced with this reason (we are not
     * accurate enough here).
     */
    INPUT_STREAM_PROBLEM,

    /**
     * Closed as requested by {@link WsConnection#close()}.
     */
    USER_REQUEST,

    /**
     * Connection close has been requested from remote side.
     */
    REMOTE_CLOSE_REQUEST
  }

  @Override
  public RelayOk runInDispatchThread(final Runnable runnable, final SyncCallback syncCallback) {
    MessageDispatcher messageDispatcher = new MessageDispatcher() {
      @Override
      boolean dispatch(Listener userListener) {
        RuntimeException ex = null;
        try {
          runnable.run();
        } catch (RuntimeException e) {
          ex = e;
          throw e;
        } finally {
          syncCallback.callbackDone(ex);
        }
        return false;
      }
    };
    synchronized (dispatchQueue) {
      if (isDispatchQueueClosed) {
        throw new IllegalStateException("Connection is closed");
      }
      dispatchQueue.add(messageDispatcher);
    }
    return DISPATCH_THREAD_PROMISES_TO_RELAY_OK;
  }

  @Override
  public void startListening(final Listener listener) {
    final INPUT loggableReader = socketWrapper.getLoggableInput();
    Runnable listenRunnable = new Runnable() {
      @Override
      public void run() {
        Exception closeCause = null;
        CloseReason closeReason = null;
        try {
          closeReason = runListenLoop(loggableReader);
        } catch (IOException e) {
          closeCause = e;
          LOGGER.log(Level.SEVERE, "Connection read failure", e);
        } catch (InterruptedException e) {
          closeCause = e;
          closeReason = CloseReason.USER_REQUEST;
          LOGGER.log(Level.SEVERE, "Thread interruption", e);
        } finally {
          synchronized (dispatchQueue) {
            dispatchQueue.add(EOS_MESSAGE_DISPATCHER);
            isDispatchQueueClosed = true;
          }

          if (connectionLogger != null) {
            connectionLogger.handleEos();
          }
          if (closeReason == null) {
            closeReason = CloseReason.INPUT_STREAM_PROBLEM;
          }
          linkedCloser.sendSignal(closeReason, closeCause);
        }
      }
    };
    Thread readThread = new Thread(listenRunnable, "WebSocket listen thread");
    readThread.setDaemon(true);
    readThread.start();
    if (connectionLogger != null) {
      connectionLogger.start();
    }

    Runnable dispatchRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          runImpl();
        } catch (InterruptedException e) {
          LOGGER.log(Level.SEVERE, "Thread interruption", e);
        }
      }
      private void runImpl() throws InterruptedException {
        while (true) {
          MessageDispatcher next = dispatchQueue.take();
          try {
            boolean isLast = next.dispatch(listener);
            if (isLast) {
              return;
            }
          } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Exception in dispatch thread", e);
          }
        }
      }
    };
    Thread dispatchThread = new Thread(dispatchRunnable, "WebSocket dispatch thread");
    dispatchThread.setDaemon(true);
    dispatchThread.start();
  }

  @Override
  public abstract void sendTextualMessage(String message) throws IOException;

  protected abstract CloseReason runListenLoop(INPUT loggableReader)
      throws IOException, InterruptedException;

  public SignalRelay<?> getCloser() {
    return linkedCloser;
  }

  protected AbstractSocketWrapper<INPUT, OUTPUT> getSocketWrapper() {
    return socketWrapper;
  }

  protected boolean isClosingGracefully() {
    return isClosingGracefully;
  }

  /**
   * Caller must be synchronized on this.
   */
  protected boolean isOutputClosed() {
    return isOutputClosed;
  }

  /**
   * Caller must be synchronized on this.
   */
  protected void setOutputClosed(boolean isOutputClosed) {
    this.isOutputClosed = isOutputClosed;
  }

  protected BlockingQueue<MessageDispatcher> getDispatchQueue() {
    return dispatchQueue;
  }

  private final SignalRelay<CloseReason> linkedCloser =
      SignalRelay.create(new SignalRelay.Callback<CloseReason>() {
    @Override public void onSignal(CloseReason param, Exception cause) {
      isClosingGracefully = true;
    }
  });

  /**
   * A debug charset that simply encodes all non-ascii symbols as %DDD.
   * We need it for log console because web-socket connection is essentially a random
   * sequence of bytes.
   */
  protected static final Charset LOGGER_CHARSET =
      new Charset("Chromium_Logger_Charset", new String[0]) {
    @Override
    public boolean contains(Charset cs) {
      return this == cs;
    }

    @Override
    public CharsetDecoder newDecoder() {
      return new CharsetDecoder(this, 4 / 2, 4) {
        @Override
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
          while (in.hasRemaining()) {
            byte b = in.get();
            if (b < 20 && b != (byte) '\n') {
              if (out.remaining() < 4) {
                return CoderResult.OVERFLOW;
              }
              out.put('%');
              int code = b;
              int d1 = code / 100 % 10;
              int d2 = code / 10 % 10;
              int d3 = code % 10;
              out.put((char) ('0' + d1));
              out.put((char) ('0' + d2));
              out.put((char) ('0' + d3));
            } else {
              char ch = (char) b;
              if (ch == '%') {
                if (out.remaining() < 2) {
                  return CoderResult.OVERFLOW;
                }
                out.put('%');
                out.put('%');
              } else {
                if (!out.hasRemaining()) {
                  return CoderResult.OVERFLOW;
                }
                out.put(ch);
              }
            }
          }
          return CoderResult.UNDERFLOW;
        }
      };
    }

    @Override
    public CharsetEncoder newEncoder() {
      throw new UnsupportedOperationException();
    }
  };

  static abstract class MessageDispatcher {
    /**
     * Dispatches message to user.
     * @return true if it was a last message in queue
     */
    abstract boolean dispatch(Listener userListener);
  }

  private static final MessageDispatcher EOS_MESSAGE_DISPATCHER = new MessageDispatcher() {
    @Override
    boolean dispatch(Listener userListener) {
      userListener.eofMessage();
      return true;
    }
  };

  private static final SignalConverter<AbstractSocketWrapper.ShutdownSignal, CloseReason>
      SOCKET_TO_CONNECTION =
      new SignalConverter<AbstractSocketWrapper.ShutdownSignal, CloseReason>() {
        @Override public CloseReason convert(AbstractSocketWrapper.ShutdownSignal source) {
          return CloseReason.CONNECTION_CLOSED;
        }
      };

  private static final RelayOk DISPATCH_THREAD_PROMISES_TO_RELAY_OK = new RelayOk() {};
}
