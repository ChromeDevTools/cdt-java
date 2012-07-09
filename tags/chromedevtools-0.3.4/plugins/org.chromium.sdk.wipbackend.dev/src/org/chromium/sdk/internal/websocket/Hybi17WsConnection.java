// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.internal.websocket.ManualLoggingSocketWrapper.LoggableInput;
import org.chromium.sdk.internal.websocket.ManualLoggingSocketWrapper.LoggableOutput;
import org.chromium.sdk.util.BasicUtil;

/**
 * WebSocket connection. Sends and receives messages. Implements HyBi-17 protocol specification.
 * @see http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17
 */
public class Hybi17WsConnection extends AbstractWsConnection<LoggableInput, LoggableOutput> {
  private static final Logger LOGGER = Logger.getLogger(Hybi17WsConnection.class.getName());
  private static final Random RANDOM = new Random();

  /**
   * Specifies how outgoing frames get masked. While protocol specification requires that every
   * outgoing frame must be masked (to disable provocative content that socket client may send),
   * this doesn't really make sense when the client is trusted. On the other hand, transparent mask
   * makes debug sniffering easier.
   */
  public enum MaskStrategy {
    /**
     * Directs to use no mask at all. This is explicitly against protocol specification, peer
     * is expected to terminate connection in response.
     */
    NO_MASK() {
      @Override public byte[] generate() {
        return null;
      }

      @Override
      ManualLoggingSocketWrapper.FactoryBase getLogWrapperFactory() {
        return ManualLoggingSocketWrapper.PLAIN_ASCII;
      }
    },
    /**
     * Directs to always use transparent mask (i.e. all zeroes). This makes all frames clear-text.
     * Not suitable when untrusted client uses the WebSocket.
     */
    TRANSPARENT_MASK() {
      private final byte[] bytes = new byte[4];

      @Override public byte[] generate() {
        return bytes;
      }

      @Override
      ManualLoggingSocketWrapper.FactoryBase getLogWrapperFactory() {
        return ManualLoggingSocketWrapper.PLAIN_ASCII;
      }
    },
    /**
     * Directs to use randomly generated masks as specified by specification. As a by-product makes
     * traffic hard to sniff.
     */
    NORMAL_MASK() {
      @Override
      byte[] generate() {
        byte[] result = new byte[4];
        RANDOM.nextBytes(result);
        return result;
      }

      @Override
      ManualLoggingSocketWrapper.FactoryBase getLogWrapperFactory() {
        return ManualLoggingSocketWrapper.ANNOTATED;
      }
    };

    /** @return 4-byte array or null */
    abstract byte[] generate();

    abstract ManualLoggingSocketWrapper.FactoryBase getLogWrapperFactory();
  }

  public static Hybi17WsConnection connect(InetSocketAddress endpoint, int timeout,
      String resourceId, MaskStrategy maskStrategy, ConnectionLogger connectionLogger)
      throws IOException {
    ManualLoggingSocketWrapper socketWrapper = new ManualLoggingSocketWrapper(endpoint, timeout,
        connectionLogger, maskStrategy.getLogWrapperFactory());

    boolean handshakeDone = false;
    Exception handshakeException = null;
    try {
      performHandshakeOrFail(socketWrapper, endpoint, resourceId);
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

    return new Hybi17WsConnection(socketWrapper, maskStrategy, connectionLogger);
  }

  private final MaskStrategy maskStrategy;

  private Hybi17WsConnection(ManualLoggingSocketWrapper socketWrapper, MaskStrategy maskStrategy,
      ConnectionLogger connectionLogger) {
    super(socketWrapper, connectionLogger);
    this.maskStrategy = maskStrategy;
  }

  @Override
  public void sendTextualMessage(final String message) throws IOException {
    final byte[] bytes = message.getBytes(UTF_8_CHARSET);

    LoggablePayload payload = new LoggablePayload() {
      @Override void send(LoggableOutput output, byte[] maskBytes) throws IOException {
        output.writeToLog(message, "utf-8 demasked");
        if (maskBytes != null) {
          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ maskBytes[i % 4]);
          }
        }
        output.writeBytesNoLogging(bytes);
      }
      @Override int getLength() {
        return bytes.length;
      }
    };

    sendMessage(OpCode.TEXT, payload, false);
  }

  @Override
  protected CloseReason runListenLoop(LoggableInput loggableReader)
      throws IOException, InterruptedException {
    try {
      return runListenLoopImpl(loggableReader);
    } catch (IOException e) {
      String stackTrace = BasicUtil.getStacktraceString(e);
      sendClosingMessage(StatusCode.PROTOCOL_ERROR, stackTrace);
      throw new IOException(e);
    } catch (IncomingProtocolException e) {
      String stackTrace = BasicUtil.getStacktraceString(e);
      sendClosingMessage(e.getStatusCode(), stackTrace);
      throw new IOException(e);
    }
  }

  private CloseReason runListenLoopImpl(LoggableInput loggableReader)
      throws IOException, InterruptedException, IncomingProtocolException {
    while (true) {
      loggableReader.markSeparatorForLog();
      int firstByte;
      try {
        firstByte = loggableReader.readByteOrEos();
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

      if ((firstByte & FrameBits.FIN_BIT) == 0) {
        throw new IncomingProtocolException("Fragments unsupported",
            StatusCode.CANNOT_ACCEPT, null);
      }
      if ((firstByte & FrameBits.RESERVED_MASK) != 0) {
        throw new IncomingProtocolException("Unexpected reserved bits",
            StatusCode.PROTOCOL_ERROR, null);
      }

      int opcode = firstByte & FrameBits.OPCODE_MASK;

      IncomingFrameHandler frameHandler;

      switch (opcode) {
      case OpCode.CONTINUATION:
        throw new IncomingProtocolException("Continuation is not supported",
            StatusCode.CANNOT_ACCEPT, null);
      case OpCode.TEXT:
        frameHandler = IncomingFrameHandler.TEXT_MESSAGE;
        break;
      case OpCode.BINARY:
        throw new IncomingProtocolException("Binary is not supported",
            StatusCode.CANNOT_ACCEPT, null);
      case OpCode.CLOSE:
        sendClosingMessage(StatusCode.NORMAL, null);
        return CloseReason.REMOTE_CLOSE_REQUEST;
      case OpCode.PING:
        frameHandler = IncomingFrameHandler.PING;
        break;
      case OpCode.PONG:
        frameHandler = IncomingFrameHandler.PONG;
        break;
      default:
        throw new IncomingProtocolException("Unsupported opcode " + opcode,
            StatusCode.CANNOT_ACCEPT, null);
      }

      int secondByte = readByteOfFail(loggableReader);

      boolean hasMask = (secondByte & FrameBits.MASK_BIT) != 0;

      if (hasMask) {
        throw new IncomingProtocolException("Masked server-to-client message is not supported",
            StatusCode.PROTOCOL_ERROR, null);
      }

      int payloadLenByte = secondByte & FrameBits.LENGTH_MASK;
      int payloadLen;
      if (payloadLenByte == FrameBits.LENGTH_2_BYTE_CODE) {
        int lengthTemp = readByteOfFail(loggableReader);
        lengthTemp <<= 8;
        lengthTemp += readByteOfFail(loggableReader);
        payloadLen = lengthTemp;
      } else if (payloadLenByte == FrameBits.LENGTH_8_BYTE_CODE) {
        for (int i = 0; i < 4; i++) {
          int b = readByteOfFail(loggableReader);
          if (b != 0) {
            throw new IncomingProtocolException("Payload length is too large",
                StatusCode.CANNOT_ACCEPT, null);
          }
        }
        int lengthTemp = readByteOfFail(loggableReader);
        if ((lengthTemp & FrameBits.HIGH_BIT) != 0) {
          throw new IncomingProtocolException("Payload length is too large",
              StatusCode.CANNOT_ACCEPT, null);
        }
        for (int i = 0; i < 3; i++) {
          lengthTemp <<= 8;
          lengthTemp += readByteOfFail(loggableReader);
        }
        payloadLen = lengthTemp;
      } else {
        payloadLen = payloadLenByte;
      }

      byte [] bytes = loggableReader.readBytes(payloadLen);
      frameHandler.process(bytes, this);
    }
  }

  private static class IncomingProtocolException extends Exception {
    private final int statusCode;

    private IncomingProtocolException(String message, int statusCode, Throwable cause) {
      super(message, cause);
      this.statusCode = statusCode;
    }

    int getStatusCode() {
      return statusCode;
    }
  }

  private static abstract class IncomingFrameHandler {
    abstract void process(byte[] bytes, Hybi17WsConnection hybiWsConnection);

    static final IncomingFrameHandler TEXT_MESSAGE = new IncomingFrameHandler() {
      @Override
      void process(byte[] bytes, Hybi17WsConnection hybiWsConnection) {
        final String text = new String(bytes, UTF_8_CHARSET);
        hybiWsConnection.getDispatchQueue().add(new MessageDispatcher() {
          @Override
          boolean dispatch(Listener userListener) {
            userListener.textMessageRecieved(text);
            return false;
          }
        });
      }
    };

    static final IncomingFrameHandler PING = new IncomingFrameHandler() {
      @Override
      void process(final byte[] bytes, Hybi17WsConnection hybiWsConnection) {
        LoggablePayload payload = new LoggablePayload() {
          @Override
          void send(LoggableOutput output, byte[] maskBytes) throws IOException {
            output.writeBytesToLog(bytes);
            if (maskBytes != null) {
              for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ maskBytes[i % 4]);
              }
            }
            output.writeBytes(bytes);
            output.markSeparatorForLog();
          }
          @Override int getLength() {
            return bytes.length;
          }
        };
        try {
          // Should we do in this thread or relay it to Dispatch thread?
          hybiWsConnection.sendMessage(OpCode.PONG, payload, false);
        } catch (IOException e) {
          LOGGER.log(Level.WARNING, "Failed to send pong", e);
        }
      }
    };

    static final IncomingFrameHandler PONG = new IncomingFrameHandler() {
      @Override
      void process(byte[] bytes, Hybi17WsConnection hybiWsConnection) {
        // Ignore
      }
    };
  }

  /**
   * Payload that can send and properly log itself. Good logging requires that the body
   * is not masked.
   */
  private static abstract class LoggablePayload {
    abstract void send(LoggableOutput output, byte[] maskBytes) throws IOException;
    abstract int getLength();
  }

  private void sendClosingMessage(final int statusCode, final String message) throws IOException {
    final byte[] bytes;
    if (message == null) {
      bytes = new byte[0];
    } else {
      bytes = message.getBytes(UTF_8_CHARSET);
    }

    LoggablePayload payload = new LoggablePayload() {
      @Override
      void send(LoggableOutput output, byte[] maskBytes) throws IOException {
        byte codeByte1 = (byte) ((statusCode >> 8) & 0xFF);
        byte codeByte2 = (byte) (statusCode & 0xFF);

        byte codeByteMasked1 = codeByte1;
        byte codeByteMasked2 = codeByte2;
        if (maskBytes != null) {
          codeByteMasked1 ^= maskBytes[0];
          codeByteMasked2 ^= maskBytes[1];

          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ maskBytes[(i + STATUS_CODE_LENTGH) % 4]);
          }
        }
        output.writeByteNoLogging(codeByteMasked1);
        output.writeByteNoLogging(codeByteMasked2);

        output.writeByteToLog(codeByte1);
        output.writeByteToLog(codeByte2);

        output.writeBytesNoLogging(bytes);
        output.writeToLog(message, "utf-8 demasked");
      }

      @Override int getLength() {
        return STATUS_CODE_LENTGH + bytes.length;
      }
    };

    sendMessage(OpCode.CLOSE, payload, true);
  }

  private void sendMessage(int opCode, LoggablePayload loggablePayload, boolean isClosingMessage)
      throws IOException {
    int length = loggablePayload.getLength();
    LoggableOutput output = getSocketWrapper().getLoggableOutput();

    byte[] maskBytes = maskStrategy.generate();

    synchronized (this) {
      if (isOutputClosed()) {
        throw new IOException("WebSocket is already closed for output");
      }

      byte firstByte = (byte) (FrameBits.FIN_BIT | OpCode.TEXT);

      output.writeByte(firstByte);

      int maskFlag = maskBytes == null ? 0 : FrameBits.MASK_BIT;

      if (length <= 125) {
        output.writeByte((byte) (length | maskFlag));
      } else if (length <= FrameBits.MAX_TWO_BYTE_INT) {
        output.writeByte((byte) (FrameBits.LENGTH_2_BYTE_CODE | maskFlag));
        output.writeByte((byte) ((length >> 8) & 0xFF));
        output.writeByte((byte) (length & 0xFF));
      } else {
        output.writeByte((byte) (FrameBits.LENGTH_8_BYTE_CODE | maskFlag));
        output.writeByte((byte) 0);
        output.writeByte((byte) 0);
        output.writeByte((byte) 0);
        output.writeByte((byte) 0);
        output.writeByte((byte) (length >>> 24));
        output.writeByte((byte) ((length >> 16) & 0xFF));
        output.writeByte((byte) ((length >> 8) & 0xFF));
        output.writeByte((byte) (length & 0xFF));
      }

      if (maskBytes != null) {
        output.writeBytes(maskBytes);
      }
      loggablePayload.send(output, maskBytes);

      if (isClosingMessage) {
        setOutputClosed(true);
      }
    }

    output.markSeparatorForLog();
  }

  private static void performHandshakeOrFail(ManualLoggingSocketWrapper socket,
      InetSocketAddress endpoint, String resourceId) throws IOException {
    Hybi17Handshake.Result result =
        Hybi17Handshake.performHandshake(socket, endpoint, resourceId, RANDOM);
    result.accept(HANDSHAKE_RESULT_VISITOR).get();
  }

  private static final Hybi17Handshake.Result.Visitor<DataOrException<Void>>
      HANDSHAKE_RESULT_VISITOR =
      new Hybi17Handshake.Result.Visitor<DataOrException<Void>>() {
        @Override
        public DataOrException<Void> visitConnected() {
          return new DataOrException<Void>() {
            @Override Void get() throws IOException {
              return null;
            }
          };
        }

        @Override
        public DataOrException<Void> visitUnknownError(final Exception exception) {
          return new DataOrException<Void>() {
            @Override Void get() throws IOException {
              throw new IOException("Failed to establish WebSocket connection", exception);
            }
          };
        }

        @Override
        public DataOrException<Void> visitErrorMessage(final int code,
            final String errorName, final String text) {
          return new DataOrException<Void>() {
            @Override Void get() throws IOException {
              throw new IOException("Failed to establish WebSocket connection: " + code + " " +
                  errorName + " | " + text);
            }
          };
        }
      };

  /**
   * This class is used solely to put IOException through Visitor.
   */
  private static abstract class DataOrException<T> {
    abstract T get() throws IOException;
  }

  private static int readByteOfFail(LoggableInput loggableReader) throws IOException {
    int b = loggableReader.readByteOrEos();
    if (b == -1) {
      throw new IOException("Unexpected EOS");
    }
    return b;
  }

  private interface FrameBits {
    // First byte bits.
    int FIN_BIT = 1 << 7;
    int MASK_BIT = 1 << 7;

    // Second byte bits.
    int OPCODE_LENGTH = 4;
    int OPCODE_MASK = (1 << OPCODE_LENGTH) - 1;
    int RESERVED_MASK = ((1 << 3) - 1) << OPCODE_LENGTH ;

    int LENGTH_MASK = (1 << 7) - 1;
    int LENGTH_2_BYTE_CODE = 126;
    int LENGTH_8_BYTE_CODE = 127;

    // Length bytes.
    int HIGH_BIT = 1 << 7;
    int MAX_TWO_BYTE_INT = 1 << 16 - 1;
  }

  private interface OpCode {
    int CONTINUATION = 0x0;
    int TEXT = 0x1;
    int BINARY = 0x2;
    int CLOSE = 0x8;
    int PING = 0x9;
    int PONG = 0xA;
  }

  private interface StatusCode {
    int NORMAL = 1000;
    int PROTOCOL_ERROR = 1002;
    int CANNOT_ACCEPT = 1003;
  }

  private static final int STATUS_CODE_LENTGH = 2;
}
