// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.transport.SocketWrapper;
import org.chromium.sdk.internal.transport.SocketWrapper.LoggableInputStream;
import org.chromium.sdk.internal.transport.SocketWrapper.LoggableOutputStream;

/**
 * WebSocket connection. Sends and receives messages. Implements HyBi-00 protocol specification.
 */
public class Hybi00WsConnection
    extends AbstractWsConnection<LoggableInputStream, LoggableOutputStream> {

  public static Hybi00WsConnection connect(InetSocketAddress endpoint, int timeout,
      String resourceId, String origin, ConnectionLogger connectionLogger) throws IOException {
    SocketWrapper socketWrapper =
        new SocketWrapper(endpoint, timeout, connectionLogger, LOGGER_CHARSET);

    boolean handshakeDone = false;
    Exception handshakeException = null;
    try {
      Hybi00Handshake.performHandshake(socketWrapper, endpoint, resourceId, origin,
          HANDSHAKE_RANDOM);
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

    return new Hybi00WsConnection(socketWrapper, connectionLogger);
  }

  private Hybi00WsConnection(SocketWrapper socketWrapper,
      ConnectionLogger connectionLogger) {
    super(socketWrapper, connectionLogger);
  }

  @Override
  public void sendTextualMessage(String message) throws IOException {
    byte[] bytes = message.getBytes(UTF_8_CHARSET);
    LoggableOutputStream loggableWriter = getSocketWrapper().getLoggableOutput();
    OutputStream output = loggableWriter.getOutputStream();
    synchronized (this) {
      output.write((byte) 0);
      output.write(bytes);
      output.write((byte) 255);
      output.flush();
    }
    loggableWriter.markSeparatorForLog();
  }

  @Override
  protected CloseReason runListenLoop(LoggableInputStream loggableReader)
      throws IOException, InterruptedException {
    BufferedInputStream input = new BufferedInputStream(loggableReader.getInputStream());
    while (true) {
      loggableReader.markSeparatorForLog();
      int firstByte;
      try {
        firstByte = input.read();
      } catch (IOException e) {
        if (isClosingGracefully()) {
          return CloseReason.USER_REQUEST;
        } else {
          throw e;
        }
      }
      if (firstByte == -1) {
        if (isClosingGracefully()) {
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
          byte b = (byte) i;
          if (b == (byte) 0xFF) {
            break;
          }
          byteBuffer.write(b);
        }
        byte[] messageBytes = byteBuffer.toByteArray();
        final String text = new String(messageBytes, UTF_8_CHARSET);
        getDispatchQueue().put(new MessageDispatcher() {
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
          getDispatchQueue().put(new MessageDispatcher() {
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

  private static final Random HANDSHAKE_RANDOM = new Random();
}
