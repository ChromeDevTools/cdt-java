// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.transport.Message.MalformedMessageException;

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
  private static final String SOCKET_CHARSET = "UTF-8";

  /**
   * A thread writing client-supplied messages into the debugger socket.
   */
  private class WriterThread extends InterruptibleThread {

    private final BufferedWriter writer;

    public WriterThread(BufferedWriter writer) {
      super("WriterThread");
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
        message.sendThrough(writer);
      } catch (IOException e) {
        SocketConnection.this.shutdown(e, false);
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

    private final BufferedReader reader;
    private final Writer handshakeWriter;

    public ReaderThread(BufferedReader reader, Writer handshakeWriter) {
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

        handshaker.perform(reader, handshakeWriter);

        startWriterThread();

        while (!isTerminated && isAttached.get()) {
          Message message;
          try {
            message = Message.fromBufferedReader(reader);
          } catch (MalformedMessageException e) {
            LOGGER.log(Level.SEVERE, "Malformed protocol message", e);
            continue;
          }
          if (message == null) {
            LOGGER.fine("End of stream");
            break;
          }
          inboundQueue.add(new RegularMessageItem(message));
        }
        breakException = null;
      } catch (IOException e) {
        breakException = e;
      } finally {
        inboundQueue.add(EOS);
      }
      if (!isInterrupted()) {
        SocketConnection.this.shutdown(breakException, false);
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

  /** The input stream buffer size. */
  private static final int INPUT_BUFFER_SIZE_BYTES = 65536;

  private static final NetListener NULL_LISTENER = new NetListener() {
    public void connectionClosed() {
    }

    public void eosReceived() {
    }

    public void messageReceived(Message message) {
    }
  };

  /** Whether the agent is currently attached to a remote browser. */
  private AtomicBoolean isAttached = new AtomicBoolean(false);

  /** The communication socket. */
  protected Socket socket;

  /** The socket reader. */
  protected BufferedReader reader;

  /** The socket writer. */
  protected BufferedWriter writer;

  private final ConnectionLogger connectionLogger;

  /** Handshaker used to establish connection. */
  private final Handshaker handshaker;

  /** The listener to report network events to. */
  protected volatile NetListener listener;

  /** The inbound message queue. */
  protected final BlockingQueue<MessageItem> inboundQueue = new LinkedBlockingQueue<MessageItem>();

  /** The outbound message queue. */
  protected final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<Message>();

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
    this.socket = new Socket();
    this.socket.connect(socketEndpoint, connectionTimeoutMs);
    Writer streamWriter = new OutputStreamWriter(socket.getOutputStream(), SOCKET_CHARSET);
    Reader streamReader = new InputStreamReader(socket.getInputStream(), SOCKET_CHARSET);

    if (connectionLogger != null) {
      streamWriter = connectionLogger.wrapWriter(streamWriter);
      streamReader = connectionLogger.wrapReader(streamReader);
    }

    this.writer = new BufferedWriter(streamWriter);
    this.reader = new BufferedReader(streamReader, INPUT_BUFFER_SIZE_BYTES);
    isAttached.set(true);

    this.readerThread = new ReaderThread(reader, writer);
    // We do not start WriterThread until handshake is done (see ReaderThread)
    this.writerThread = null;
    readerThread.setDaemon(true);
    readerThread.start();
  }

  void detach(boolean lameduckMode) {
    shutdown(null, lameduckMode);
  }

  void sendMessage(Message message) {
    outboundQueue.add(message);
  }

  private boolean isAttached() {
    return isAttached.get();
  }

  /**
   * The method is synchronized so that it does not get called
   * from the {Reader,Writer}Thread when the underlying socket is
   * closed in another invocation of this method.
   */
  private void shutdown(Exception cause, boolean lameduckMode) {
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
      // ignore
    }
    listener.connectionClosed();
  }

  private void interruptServiceThreads() {
    interruptThread(writerThread);
    interruptThread(readerThread);
  }

  private void startWriterThread() {
    if (writerThread != null) {
      throw new IllegalStateException();
    }
    writerThread = new WriterThread(writer);
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

  private void interruptThread(Thread thread) {
    try {
      if (thread != null) {
        thread.interrupt();
      }
    } catch (SecurityException e) {
      // ignore
    }
  }

  public void close() {
    if (isAttached()) {
      detach(true);
    }
  }

  public boolean isConnected() {
    return isAttached();
  }

  public void send(Message message) {
    checkAttached();
    sendMessage(message);
  }

  public void setNetListener(NetListener netListener) {
    if (this.listener != null && netListener != this.listener) {
      throw new IllegalStateException("Cannot change NetListener");
    }
    this.listener = netListener != null
        ? netListener
        : NULL_LISTENER;
  }

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
