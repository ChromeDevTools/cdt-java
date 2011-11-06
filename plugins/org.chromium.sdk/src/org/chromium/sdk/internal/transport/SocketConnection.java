// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.transport.Message.MalformedMessageException;
import org.chromium.sdk.util.SignalRelay;
import org.chromium.sdk.util.SignalRelay.AlreadySignalledException;

/**
 * The low-level network agent handling the reading and writing of Messages
 * using the debugger socket.
 *
 * This class is thread-safe.
 */
public class SocketConnection implements Connection {

  /**
   * A thread that can be gracefully interrupted by a third party.
   * <p>
   * Unfortunately there is no standard way of interrupting I/O in Java. See Bug #4514257
   * on Java Bug Database (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4514257).
   */
  private static abstract class InterruptibleThread extends Thread {

    protected volatile boolean isTerminated = false;

    InterruptibleThread(String name) {
      super(name);
    }

    @Override
    public synchronized void start() {
      this.isTerminated = false;
      super.start();
    }

    @Override
    public synchronized void interrupt() {
      this.isTerminated = true;
      super.interrupt();
    }
  }

  /**
   * Character encoding used in the socket data interchange.
   */
  private static final Charset SOCKET_CHARSET = Charset.forName("UTF-8");

  /**
   * A thread writing client-supplied messages into the debugger socket.
   */
  private class WriterThread extends InterruptibleThread {

    private final AutoLoggingSocketWrapper.LoggableOutputStream writer;

    public WriterThread(AutoLoggingSocketWrapper.LoggableOutputStream writer) {
      super("WriterThread");
      // Wrap writer into a buffered writer.
      this.writer = writer;
    }

    @Override
    public void run() {
      while (!isTerminated && isAttached.get()) {
        try {
          handleOutboundMessage(outboundQueue.take());
        } catch (InterruptedException e) {
          // interrupt called on this thread, exit on isTerminated
        }
      }
    }

    private void handleOutboundMessage(Message message) {
      try {
        LOGGER.log(Level.FINER, "-->{0}", message);
        message.sendThrough(writer.getOutputStream(), SOCKET_CHARSET);
        writer.getOutputStream().flush();
        writer.markSeparatorForLog();
      } catch (IOException e) {
        shutdownRelay.sendSignal(false, e);
      }
    }
  }

  private static abstract class MessageItem {
    abstract void report(NetListener listener);
    abstract boolean isEos();
  }
  private static final MessageItem EOS = new MessageItem() {
    @Override
    void report(NetListener listener) {
      LOGGER.log(Level.FINER, "<--EOS");
      listener.eosReceived();
    }
    @Override
    boolean isEos() {
      return true;
    }
  };
  private static class RegularMessageItem extends MessageItem {
    private final Message message;
    RegularMessageItem(Message message) {
      this.message = message;
    }
    @Override
    void report(NetListener listener) {
      LOGGER.log(Level.FINER, "<--{0}", message);
      listener.messageReceived(message);
    }
    @Override
    boolean isEos() {
      return false;
    }
  }

  /**
   * A thread reading data from the debugger socket.
   */
  private class ReaderThread extends InterruptibleThread {

    private final AutoLoggingSocketWrapper.LoggableInputStream reader;
    private final AutoLoggingSocketWrapper.LoggableOutputStream handshakeWriter;

    public ReaderThread(AutoLoggingSocketWrapper.LoggableInputStream reader,
        AutoLoggingSocketWrapper.LoggableOutputStream handshakeWriter) {
      super("ReaderThread");
      this.reader = reader;
      this.handshakeWriter = handshakeWriter;
    }

    @Override
    public void run() {
      Exception breakException;
      try {
        /** The thread that dispatches the inbound messages (to avoid queue growth.) */
        startResponseDispatcherThread();

        if (connectionLogger != null) {
          connectionLogger.start();
        }

        LineReader lineReader = new LineReader(reader.getInputStream());

        handshaker.perform(lineReader, handshakeWriter.getOutputStream());

        reader.markSeparatorForLog();
        handshakeWriter.markSeparatorForLog();

        startWriterThread();

        while (!isTerminated && isAttached.get()) {
          Message message;
          try {
            message = Message.fromBufferedReader(lineReader, SOCKET_CHARSET);
          } catch (MalformedMessageException e) {
            LOGGER.log(Level.SEVERE, "Malformed protocol message", e);
            continue;
          }
          if (message == null) {
            LOGGER.fine("End of stream");
            break;
          }
          inboundQueue.add(new RegularMessageItem(message));
          reader.markSeparatorForLog();
        }
        breakException = null;
      } catch (IOException e) {
        breakException = e;
      } finally {
        synchronized (inboundQueue) {
          inboundQueue.add(EOS);
          isInboundQueueClosed = true;
        }
      }
      if (!isInterrupted()) {
        shutdownRelay.sendSignal(false, breakException);
      }
    }
  }

  /**
   * A thread dispatching V8 responses (to avoid locking the ReaderThread.)
   */
  private class ResponseDispatcherThread extends Thread {

    public ResponseDispatcherThread() {
      super("ResponseDispatcherThread");
    }

    @Override
    public void run() {
      MessageItem messageItem;
      try {
        while (true) {
          messageItem = inboundQueue.take();
          try {
            messageItem.report(listener);
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception in message listener", e);
          }
          if (messageItem.isEos()) {
            if (connectionLogger != null) {
              connectionLogger.handleEos();
            }
            break;
          }
        }
      } catch (InterruptedException e) {
        // terminate thread
      }
    }
  }

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(SocketConnection.class.getName());

  /** Lameduck shutdown delay in ms. */
  private static final int LAMEDUCK_DELAY_MS = 1000;

  private static final NetListener NULL_LISTENER = new NetListener() {
    @Override public void connectionClosed() {
    }

    @Override public void eosReceived() {
    }

    @Override public void messageReceived(Message message) {
    }
  };

  /** Whether the agent is currently attached to a remote browser. */
  private AtomicBoolean isAttached = new AtomicBoolean(false);

  /** The communication socket. */
  private AutoLoggingSocketWrapper socket;

  private final ConnectionLogger connectionLogger;

  /** Handshaker used to establish connection. */
  private final Handshaker handshaker;

  /** The listener to report network events to. */
  private volatile NetListener listener;

  /** The inbound message queue. */
  private final BlockingQueue<MessageItem> inboundQueue = new LinkedBlockingQueue<MessageItem>();

  /** Field must be accessed synchronized on inboundQueue */
  private boolean isInboundQueueClosed = false;

  /** The outbound message queue. */
  private final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<Message>();

  /** The socket endpoint. */
  private final SocketAddress socketEndpoint;

  /** The thread that processes the outbound queue. */
  private WriterThread writerThread;

  /** The thread that processes the inbound queue. */
  private ReaderThread readerThread;

  /** Connection attempt timeout in ms. */
  private final int connectionTimeoutMs;

  public SocketConnection(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger, Handshaker handshaker) {
    this.socketEndpoint = endpoint;
    this.connectionTimeoutMs = connectionTimeoutMs;
    this.connectionLogger = connectionLogger;
    this.handshaker = handshaker;
  }

  void attach() throws IOException {
    this.socket = new AutoLoggingSocketWrapper(socketEndpoint, connectionTimeoutMs,
        connectionLogger, SOCKET_CHARSET);

    try {
      shutdownRelay.bind(this.socket.getShutdownRelay(), null, null);
    } catch (AlreadySignalledException e) {
      throw new IOException("Unexpected: socket is already closed", e);
    }

    isAttached.set(true);

    this.readerThread = new ReaderThread(socket.getLoggableInput(), socket.getLoggableOutput());
    // We do not start WriterThread until handshake is done (see ReaderThread)
    this.writerThread = null;
    readerThread.setDaemon(true);
    readerThread.start();
  }

  void sendMessage(Message message) {
    try {
      outboundQueue.put(message);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void runInDispatchThread(final Runnable callback) {
    MessageItem messageItem = new MessageItem() {
      @Override
      void report(NetListener listener) {
        callback.run();
      }
      @Override
      boolean isEos() {
        return false;
      }
    };
    try {
      synchronized (inboundQueue) {
        if (isInboundQueueClosed) {
          throw new IllegalStateException("Connection is closed");
        }
        inboundQueue.put(messageItem);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isAttached() {
    return isAttached.get();
  }

  private final SignalRelay<Boolean> shutdownRelay =
      SignalRelay.create(new SignalRelay.Callback<Boolean>() {
    @Override public void onSignal(Boolean lameduckMode, Exception cause) {
      shutdown(lameduckMode == Boolean.TRUE, cause);
    }

    private void shutdown(boolean lameduckMode, Exception cause) {
      if (!isAttached.compareAndSet(true, false)) {
        // already shut down
        return;
      }
      LOGGER.log(Level.INFO, "Shutdown requested", cause);

      if (lameduckMode) {
        Thread terminationThread = new Thread("ServiceThreadTerminator") {
          @Override
          public void run() {
            interruptServiceThreads();
          }
        };
        terminationThread.setDaemon(true);
        terminationThread.start();
        try {
          terminationThread.join(LAMEDUCK_DELAY_MS);
        } catch (InterruptedException e) {
          // fall through
        }
      } else {
        interruptServiceThreads();
      }
    }
    private void interruptServiceThreads() {
      interruptThread(writerThread);
      interruptThread(readerThread);
    }
    private void interruptThread(Thread thread) {
      try {
        if (thread != null) {
          thread.interrupt();
        }
      } catch (SecurityException e) {
        // ignore
      }
    }
  });

  private void startWriterThread() {
    if (writerThread != null) {
      throw new IllegalStateException();
    }
    writerThread = new WriterThread(socket.getLoggableOutput());
    writerThread.setDaemon(true);
    writerThread.start();
  }

  private ResponseDispatcherThread startResponseDispatcherThread() {
    ResponseDispatcherThread dispatcherThread;
    dispatcherThread = new ResponseDispatcherThread();
    dispatcherThread.setDaemon(true);
    dispatcherThread.start();
    return dispatcherThread;
  }

  @Override public void close() {
    shutdownRelay.sendSignal(true, null);
  }

  @Override public boolean isConnected() {
    return isAttached();
  }

  @Override
  public void send(Message message) {
    checkAttached();
    sendMessage(message);
  }

  @Override
  public void setNetListener(NetListener netListener) {
    if (this.listener != null && netListener != this.listener) {
      throw new IllegalStateException("Cannot change NetListener");
    }
    this.listener = netListener != null
        ? netListener
        : NULL_LISTENER;
    SignalRelay<?> listenerCloser = SignalRelay.create(new SignalRelay.Callback<Void>() {
      @Override public void onSignal(Void param, Exception cause) {
        listener.connectionClosed();
      }
    });
    try {
      shutdownRelay.bind(listenerCloser, null, null);
    } catch (AlreadySignalledException e) {
      // ListenerCloser cannot be closing and we should not be closing at this moment of time.
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void start() throws IOException {
    try {
      if (!isAttached()) {
        attach();
      }
    } catch (IOException e) {
      listener.connectionClosed();
      throw e;
    }
  }

  private void checkAttached() {
    if (!isAttached()) {
      throw new IllegalStateException("Connection not attached");
    }
  }
}
