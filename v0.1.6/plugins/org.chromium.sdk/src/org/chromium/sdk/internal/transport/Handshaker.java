// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.chromium.sdk.LineReader;
import org.chromium.sdk.internal.JavascriptVmImpl;
import org.chromium.sdk.internal.transport.Message.MalformedMessageException;

/**
 * Handshaker handles "handshake" part of communication. It may write and read whatever it needs
 * before regular message-based communication has started.
 */
public interface Handshaker {
  /**
   * Performs handshake. This method is blocking. After it has successfully finished, input/output
   * should be ready for normal message-based communication. In case method fails with IOException,
   * input and output are returned in undefined state.
   * @throws IOException if handshake process failed physically (input or output has unexpectedly
   * closed) or logically (if unexpected message came from remote).
   */
  void perform(LineReader input, Writer output) throws IOException;

  /**
   * Implementation of handshake from Google Chrome Developer Tools Protocol. Used when we
   * connect to browser.
   */
  Handshaker CHROMIUM = new Handshaker() {
    public void perform(LineReader input, Writer output) throws IOException {
      output.write(OUTGOING_MESSAGE);
      output.flush();

      // TODO(prybin): expose this as a parameter or get rid of this option if we don't need it.
      final boolean ignoreUnexpectedResponses = false;

      while (true) {
        if (Thread.interrupted()) {
          throw new IOException("Interrupted");
        }
        String line = input.readLine();
        if (line == null) {
          throw new IOException("Connection closed");
        }
        if (INCOMING_TEXT.equals(line)) {
          break;
        }
        if (!ignoreUnexpectedResponses) {
          throw new IOException("Unexpected handshake: " + line);
        }
      }
    }

    /**
     * A handshake string to be sent by a browser on the connection start,
     * specified by the protocol design doc (without trailing cr/lf).
     */
    private static final String INCOMING_TEXT = "ChromeDevToolsHandshake";

    /**
     * A handshake string that we send to a browser on the connection start,
     * specified by the protocol design doc (including trailing cr/lf).
     */
    private static final String OUTGOING_MESSAGE = "ChromeDevToolsHandshake\r\n";
  };

  /**
   * Stateful handshaker implementation for Standalone V8 protocol.
   */
  class StandaloneV8 implements Handshaker {
    public interface RemoteInfo {
      String getProtocolVersion();
      String getV8VmVersion();
      String getEmbeddingHostName();
    }

    public Future<RemoteInfo> getRemoteInfo() {
      return runnableFuture;
    }

    private final FutureTask<RemoteInfo> runnableFuture =
        new FutureTask<RemoteInfo>(new HandshakeTaks());

    private LineReader input = null;

    public void perform(LineReader input, Writer output) throws IOException {
      this.input = input;
      runnableFuture.run();

      // Check for possible exceptions
      try {
        runnableFuture.get();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw JavascriptVmImpl.newIOException("Failed to perform handshake", e);
      }

    }

    class HandshakeTaks implements Callable<RemoteInfo> {
      public RemoteInfo call() throws IOException {
        final Message message;
        try {
          message = Message.fromBufferedReader(input);
        } catch (MalformedMessageException e) {
          throw JavascriptVmImpl.newIOException("Unrecognized handshake message from remote", e);
        }
        if (message == null) {
          throw new IOException("End of stream");
        }
        final String protocolVersion = message.getHeader("Protocol-Version", null);
        if (protocolVersion == null) {
          throw new IOException("Absent protocol version");
        }
        final String vmVersion = message.getHeader("V8-Version", null);
        if (vmVersion == null) {
          throw new IOException("Absent V8 VM version");
        }
        RemoteInfo remoteInfo = new RemoteInfo() {
          public String getProtocolVersion() {
            return protocolVersion;
          }
          public String getV8VmVersion() {
            return vmVersion;
          }
          public String getEmbeddingHostName() {
            return message.getHeader("Embedding-Host", null);
          }
        };
        return remoteInfo;
      }
    }
  }
}
