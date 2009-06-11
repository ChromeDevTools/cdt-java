// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public WriterThread() {
      super("WriterThread");
    }

    @Override
    public void run() {
      while (!isTerminated && isAttached) {
        try {
          handleOutboundMessage(outboundQueue.take());
        } catch (InterruptedException e) {
          // interrupt called on this thread, exit on isTerminated
        }
      }
    }

    private void handleOutboundMessage(Message message) {
      try {
        synchronized (socket) {
          if (!socket.isClosed()) {
            log(Level.FINER, "-->" + message, null);
            message.sendThrough(writer);
          }
        }
      } catch (IOException e) {
        SocketConnection.this.shutdown(e, false);
      }
    }
  }

  /**
   * A thread reading data from the debugger socket.
   */
  private class ReaderThread extends InterruptibleThread {

    /**
     * A handshake string to be sent by a browser on the connection start,
     * specified by the protocol design doc.
     */
    private static final String CHROME_DEV_TOOLS_HANDSHAKE = "ChromeDevToolsHandshake";

    private boolean handshakeDone = false;

    public ReaderThread() {
      super("ReaderThread");
    }

    @Override
    public void run() {
      Message message;
      try {
        while (!isTerminated && isAttached) {
          if (handshakeDone) {
            try {
              message = Message.fromBufferedReader(reader);
            } catch (MalformedMessageException e) {
              log(Level.SEVERE, "Malformed protocol message", e);
              continue;
            }
            inboundQueue.add(message);
          } else {
            String line = reader.readLine();
            if (CHROME_DEV_TOOLS_HANDSHAKE.equals(line)) {
              handshakeDone = true;
            }
          }
        }
      } catch (IOException e) {
        if (!isInterrupted()) {
          SocketConnection.this.shutdown(e, false);
        }
      }
    }
  }

  /**
   * A thread dispatching V8 responses (to avoid locking the ReaderThread.)
   */
  private class ResponseDispatcherThread extends InterruptibleThread {

    public ResponseDispatcherThread() {
      super("ResponseDispatcherThread");
    }

    @Override
    public void run() {
      Message message;
      try {
        while (!isTerminated && isAttached) {
          message = inboundQueue.take();
          try {
            handleInboundMessage(message);
          } catch (Exception e) {
            log(Level.SEVERE, "Exception in message listener", e);
          }
        }
      } catch (InterruptedException e) {
        // terminate thread
      }
    }

    private void handleInboundMessage(Message message) {
      log(Level.FINER, "<--" + message, null);
      listener.messageReceived(message);
    }
  }

  /** Lameduck shutdown delay in ms. */
  private static final int LAMEDUCK_DELAY_MS = 1000;

  private static final NetListener NULL_LISTENER = new NetListener() {
    public void connectionClosed() {
    }

    public void messageReceived(Message message) {
    }
  };

  /** Whether the agent is currently attached to a remote browser. */
  protected volatile boolean isAttached = false;

  /** The communication socket. */
  protected Socket socket;

  /** The socket reader. */
  protected BufferedReader reader;

  /** The socket writer. */
  protected BufferedWriter writer;

  /** The listener to report network events to. */
  protected volatile NetListener listener;

  /** The inbound message queue. */
  protected final BlockingQueue<Message> inboundQueue = new LinkedBlockingQueue<Message>();

  /** The outbound message queue. */
  protected final BlockingQueue<Message> outboundQueue = new LinkedBlockingQueue<Message>();

  /** The socket endpoint. */
  private final SocketAddress socketEndpoint;

  /** The thread that processes the outbound queue. */
  private WriterThread writerThread;

  /** The thread that processes the inbound queue. */
  private ReaderThread readerThread;

  /** The thread that dispatches the inbound messages (to avoid queue growth.) */
  private ResponseDispatcherThread dispatcherThread;

  /** Connection attempt timeout in ms. */
  private final int connectionTimeoutMs;

  /** Browser server socket host. */
  private final String host;

  /** Browser server socket host. */
  private final int port;

  public SocketConnection(String host, int port, int connectionTimeoutMs) {
    this.host = host;
    this.port = port;
    this.socketEndpoint = new InetSocketAddress(host, port);
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  synchronized void attach() throws IOException {
    this.socket = new Socket();
    this.readerThread = new ReaderThread();
    this.writerThread = new WriterThread();
    this.dispatcherThread = new ResponseDispatcherThread();

    socket.connect(socketEndpoint, connectionTimeoutMs);
    this.writer =
        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), SOCKET_CHARSET));
    this.reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream(), SOCKET_CHARSET));

    isAttached = true;

    writerThread.setDaemon(true);
    writerThread.start();
    readerThread.setDaemon(true);
    readerThread.start();
    dispatcherThread.setDaemon(true);
    dispatcherThread.start();

    outboundQueue.add(new HandshakeMessage());
  }

  void detach(boolean lameduckMode) {
    shutdown(null, lameduckMode);
  }

  void sendMessage(Message message) {
    outboundQueue.add(message);
  }

  private boolean isAttached() {
    return isAttached;
  }

  private synchronized void shutdown(Exception cause, boolean lameduckMode) {
    if (!isAttached) {
      return;
    }
    isAttached = false;
    log(Level.INFO, "Shutdown requested", cause);

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

    // Do not synchronize on anything as all pending transfers should be terminated.
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
    interruptThread(dispatcherThread);
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

  private static void log(Level level, String message, Exception exception) {
    Logger.getLogger(SocketConnection.class.getName()).log(level, message, exception);
  }

  @Override
  public void close() {
    if (isAttached()) {
      detach(true);
    }
  }

  @Override
  public boolean isConnected() {
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



  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SocketConnection)) {
      return false;
    }
    SocketConnection that = (SocketConnection) obj;
    return (this.host.equals(that.host) && this.port == that.port);
  }

  @Override
  public int hashCode() {
    return host.hashCode() + port;
  }

  private void checkAttached() {
    if (!isAttached()) {
      throw new IllegalStateException("Connection not attached");
    }
  }
}
