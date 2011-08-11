// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.transport.SocketWrapper;
import org.chromium.sdk.util.SignalRelay;
import org.chromium.sdk.util.SignalRelay.AlreadySignalledException;
import org.chromium.sdk.util.SignalRelay.SignalConverter;

/**
 * WebSocket connection. Sends and receives messages.
 */
public class WsConnection {
  private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
  private static final Logger LOGGER = Logger.getLogger(WsConnection.class.getName());
  private static final Random HANDSHAKE_RANDOM = new Random();

  public static WsConnection connect(InetSocketAddress endpoint, int timeout, String resourceId,
      String origin, ConnectionLogger connectionLogger) throws IOException {
    SocketWrapper socketWrapper =
        new SocketWrapper(endpoint, timeout, connectionLogger, LOGGER_CHARSET);

    boolean handshakeDone = false;
    Exception handshakeException = null;
    try {
      Handshake.performHandshake(socketWrapper, endpoint, resourceId, origin, HANDSHAKE_RANDOM);
      handshakeDone = true;
    } catch (RuntimeException e) {
      handshakeException = e;
      throw e;
    } catch (IOException e) {
      handshakeException = e;
      throw e;
    } finally {
      if (!handshakeDone) {
        socketWrapper.getShutdownRelay().sendSignal(null, handshakeException);
      }
    }

    return new WsConnection(socketWrapper, connectionLogger);
  }

  private final SocketWrapper socketWrapper;
  private final ConnectionLogger connectionLogger;
  private volatile boolean isClosingGracefully = false;

  private WsConnection(SocketWrapper socketWrapper, ConnectionLogger connectionLogger) {
    this.socketWrapper = socketWrapper;
    this.connectionLogger = connectionLogger;
    try {
      linkedCloser.bind(socketWrapper.getShutdownRelay(), null, SOCKET_TO_CONNECTION);
    } catch (AlreadySignalledException e) {
      throw new IllegalStateException(e);
    }
  }

  public interface Listener {
    void textMessageRecieved(String text);
    /**
     * Some non-fatal error happened.
     */
    void errorMessage(Exception ex);

    /**
     * Connection has been closed. Message is called from Dispatch thread.
     */
    void eofMessage();
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

  public void startListening(final Listener listener) {
    SignalRelay<CloseReason> listenerCloser =
        SignalRelay.create(new SignalRelay.Callback<CloseReason>() {
      @Override
      public void onSignal(CloseReason reason, Exception cause) {
      }
    });

    final SocketWrapper.LoggableInputStream loggableReader = socketWrapper.getLoggableInput();
    final BufferedInputStream input = new BufferedInputStream(loggableReader.getInputStream());
    Runnable listenRunnable = new Runnable() {
      @Override
      public void run() {
        Exception closeCause = null;
        CloseReason closeReason = null;
        try {
          closeReason = runImpl();
        } catch (IOException e) {
          closeCause = e;
          LOGGER.log(Level.SEVERE, "Connection read failure", e);
        } catch (InterruptedException e) {
          closeCause = e;
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

      private CloseReason runImpl() throws IOException, InterruptedException {
        while (true) {
          loggableReader.markSeparatorForLog();
          int firstByte;
          try {
            firstByte = input.read();
          } catch (IOException e) {
            if (isClosingGracefully) {
              return CloseReason.USER_REQUEST;
            } else {
              throw e;
            }
          }
          if (firstByte == -1) {
            if (isClosingGracefully) {
              return CloseReason.USER_REQUEST;
            } else {
              throw new IOException("Unexpected end of stream");
            }
          }
          if ((firstByte & 0x80) == 0) {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            while (true) {
              int i = input.read();
              if (i == -1) {
                throw new IOException("End of stream");
              }
              byte b = (byte)i;
              if (b == (byte)0xFF) {
                break;
              }
              byteBuffer.write(b);
            }
            byte[] messageBytes = byteBuffer.toByteArray();
            final String text = new String(messageBytes, UTF_8_CHARSET);
            dispatchQueue.put(new MessageDispatcher() {
              @Override
              public boolean dispatch(Listener userListener) {
                userListener.textMessageRecieved(text);
                return false;
              }
            });
          } else {
            long len = 0;
            while (true) {
              int lengthByte = input.read();
              if (lengthByte == -1) {
                throw new IOException("End of stream");
              }
              len = len * 10 + (lengthByte & 0x7F);
              if (len > Integer.MAX_VALUE) {
                throw new IOException("Message too long");
              }
              if ((lengthByte & 0x80) == 0) {
                break;
              }
            }
            long needSkip = len;
            while (needSkip > 0) {
              long skipped = input.skip(needSkip);
              needSkip -= skipped;
            }
            if (firstByte == (byte) 0xFF && len == 0) {
              return CloseReason.REMOTE_CLOSE_REQUEST;
            } else {
              final long finalLen = len;
              dispatchQueue.put(new MessageDispatcher() {
                @Override
                public boolean dispatch(Listener userListener) {
                  userListener.errorMessage(
                      new Exception("Unexpected binary message of length " + finalLen));
                  return false;
                }
              });
            }
          }
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

  public void sendTextualMessage(String message) throws IOException {
    byte[] bytes = message.getBytes(UTF_8_CHARSET);
    SocketWrapper.LoggableOutputStream loggableWriter = socketWrapper.getLoggableOutput();
    OutputStream output = loggableWriter.getOutputStream();
    output.write((byte) 0);
    output.write(bytes);
    output.write((byte) 255);
    output.flush();
    loggableWriter.markSeparatorForLog();
  }

  public void close() {
    linkedCloser.sendSignal(CloseReason.USER_REQUEST, null);
  }

  public SignalRelay<?> getCloser() {
    return linkedCloser;
  }

  private final SignalRelay<CloseReason> linkedCloser =
      SignalRelay.create(new SignalRelay.Callback<CloseReason>() {
    @Override public void onSignal(CloseReason param, Exception cause) {
      isClosingGracefully = true;
    }
  });

  private final BlockingQueue<MessageDispatcher> dispatchQueue =
      new LinkedBlockingQueue<MessageDispatcher>();

  // Access must be synchronized on dispatchQueue
  private boolean isDispatchQueueClosed = false;

  /**
   * A debug charset that simply encodes all non-ascii symbols as %DDD.
   * We need it for log console because web-socket connection is essentially a random
   * sequence of bytes.
   */
  private static final Charset LOGGER_CHARSET =
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

  private static abstract class MessageDispatcher {
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

  private static final SignalConverter<SocketWrapper.ShutdownSignal, CloseReason>
      SOCKET_TO_CONNECTION = new SignalConverter<SocketWrapper.ShutdownSignal, CloseReason>() {
    @Override public CloseReason convert(SocketWrapper.ShutdownSignal source) {
      return CloseReason.CONNECTION_CLOSED;
    }
  };

  private static final RelayOk DISPATCH_THREAD_PROMISES_TO_RELAY_OK = new RelayOk() {};
}
