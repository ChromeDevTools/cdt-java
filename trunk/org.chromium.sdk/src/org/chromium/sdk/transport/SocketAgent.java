// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.LoggingUtil;

/**
 * The low-level network agent handling the reading and writing of
 * Messages using the debugger socket.
 *
 * This class is thread-safe.
 */
class SocketAgent {

  /**
   * Character encoding used in the socket data interchange.
   */
  private static final String SOCKET_CHARSET = "UTF-8"; //$NON-NLS-1$

  /**
   * This interface implementor is passed into SocketAgent on construction,
   * and it will be notified of the network events that occur with the agent.
   */
  public interface SocketAgentListener {

    /**
     * Gets invoked when a message is received from a Chromium instance.
     */
    void messageReceived(Message message);

    /**
     * Gets invoked when a socket associated with the agent is closed (e.g.
     * due to a Chromium instance shutdown.)
     */
    void socketClosed();
  }

  /**
   * A thread writing client-supplied messages into the debugger socket.
   */
  private class WriterThread extends Thread {

    private volatile boolean isTerminated;

    public WriterThread() {
      super("WriterThread"); //$NON-NLS-1$
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
        LoggingUtil.logTransport("--> " + message.toString()); //$NON-NLS-1$
        synchronized (socket) {
          if (!socket.isClosed()) {
            message.sendThrough(writer);
          }
        }
      } catch (IOException e) {
        SocketAgent.this.shutdown(e, false);
      }
    }
  }

  /**
   * A thread reading data from the debugger socket.
   */
  private class ReaderThread extends Thread {

    /**
     * A handshake string to be sent by Chromium first, specified by the
     * protocol design doc.
     */
    private static final String CHROME_DEV_TOOLS_HANDSHAKE =
        "ChromeDevToolsHandshake"; //$NON-NLS-1$

    private volatile boolean isTerminated = false;

    private boolean handshakeDone = false;

    public ReaderThread() {
      super("ReaderThread"); //$NON-NLS-1$
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

    @Override
    public void run() {
      Message message;
      try {
        while (!isTerminated && isAttached) {
          if (handshakeDone) {
            message = Message.fromBufferedReader(reader);
            if (message != null) {
              inboundQueue.add(message);
            }
          } else {
            String line = reader.readLine();
            if (CHROME_DEV_TOOLS_HANDSHAKE.equals(line)) {
              handshakeDone = true;
            }
          }
        }
      } catch (IOException e) {
        if (!isInterrupted()) {
          SocketAgent.this.shutdown(e, false);
        }
      }
    }
  }

  /**
   * A thread dispatching V8 responses (to avoid locking the ReaderThread.)
   */
  private class ResponseDispatcherThread extends Thread {
    private volatile boolean isTerminated = false;

    public ResponseDispatcherThread() {
      super("ResponseDispatcherThread"); //$NON-NLS-1$
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

    @Override
    public void run() {
      Message message;
      try {
        while (!isTerminated && isAttached) {
          message = inboundQueue.take();
          try {
            handleInboundMessage(message);
          } catch (Exception e) {
            ChromiumDebugPlugin.log(e);
          }
        }
      } catch (InterruptedException e) {
        // terminate thread
      }
    }

    private void handleInboundMessage(Message message) {
      LoggingUtil.logTransport("<-- " + message.toString()); //$NON-NLS-1$
      listener.messageReceived(message);
    }
  }

  /** Lameduck shutdown delay in ms. */
  private static final int LAMEDUCK_DELAY_MS = 1000;

  private final BlockingQueue<Message> inboundQueue =
      new LinkedBlockingQueue<Message>();

  private final BlockingQueue<Message> outboundQueue =
      new LinkedBlockingQueue<Message>();

  private WriterThread writerThread;

  private ReaderThread readerThread;

  private ResponseDispatcherThread dispatcherThread;

  private final SocketAddress endpoint;

  private Socket socket;

  private BufferedReader reader;

  private BufferedWriter writer;

  private final SocketAgentListener listener;

  private Exception terminatedException;

  private volatile boolean isAttached = false;

  SocketAgent(SocketAddress endpoint, SocketAgentListener listener) {
    this.endpoint = endpoint;
    this.listener = listener;
  }

  void attach() throws IOException {
    this.terminatedException = null;
    this.socket = new Socket();
    this.readerThread = new ReaderThread();
    this.writerThread = new WriterThread();
    this.dispatcherThread = new ResponseDispatcherThread();

    // TODO(apavlov): introduce timeout property into the launch tab
    socket.connect(endpoint, 1000); // 1 second to connect
    this.writer =
        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
            SOCKET_CHARSET));
    this.reader =
        new BufferedReader(new InputStreamReader(socket.getInputStream(),
            SOCKET_CHARSET));

    isAttached = true;

    writerThread.start();
    readerThread.start();
    dispatcherThread.start();

    outboundQueue.add(new HandshakeMessage());
  }

  void detach(boolean lameduckMode) {
    shutdown(null, lameduckMode);
  }

  void sendMessage(Message message) {
    outboundQueue.add(message);
  }

  Exception getTerminatedException() {
    return terminatedException;
  }

  boolean isAttached() {
    return isAttached;
  }

  private synchronized void shutdown(Exception cause, boolean lameduckMode) {
    if (!isAttached) {
      return;
    }
    isAttached = false;
    LoggingUtil.logTransport("Shutdown requested", cause); //$NON-NLS-1$

    if (lameduckMode) {
      final Semaphore sem = new Semaphore(0);
      TimerTask terminationTask = new TimerTask() {
        @Override
        public void run() {
          interruptServiceThreads();
          sem.release();
        }
      };
      Timer timer = new Timer("ServiceThreadTerminator"); //$NON-NLS-1$
      timer.schedule(terminationTask, LAMEDUCK_DELAY_MS);
      try {
        sem.acquire();
      } catch (InterruptedException e) {
        // fall through
      }
    } else {
      interruptServiceThreads();
    }

    // Do not synchronize on anything
    // as all pending transfers should be terminated
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
    this.terminatedException = cause;
    listener.socketClosed();
  }

  private void interruptServiceThreads() {
    interruptThread(writerThread);
    interruptThread(readerThread);
    interruptThread(dispatcherThread);
  }

  private void interruptThread(Thread thread) {
    try {
      thread.interrupt();
    } catch (SecurityException e) {
      // ignore
    }
  }
}
