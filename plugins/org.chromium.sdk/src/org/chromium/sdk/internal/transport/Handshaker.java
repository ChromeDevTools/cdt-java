// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

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
  void perform(BufferedReader input, Writer output) throws IOException;

  /**
   * Implementation of handshake from Google Chrome Developer Tools Protocol. Used when we
   * connect to browser.
   */
  Handshaker CHROMIUM = new Handshaker() {
    public void perform(BufferedReader input, Writer output) throws IOException {
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
}
