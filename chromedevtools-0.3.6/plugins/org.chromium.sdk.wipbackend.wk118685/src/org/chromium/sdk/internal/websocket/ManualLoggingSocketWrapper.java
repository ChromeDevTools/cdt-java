// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.StreamListener;
import org.chromium.sdk.internal.transport.AbstractSocketWrapper;
import org.chromium.sdk.internal.websocket.ManualLoggingSocketWrapper.LoggableInput;
import org.chromium.sdk.internal.websocket.ManualLoggingSocketWrapper.LoggableOutput;

/**
 * A wrapper around platform socket that handles logging and closing. It allows user to manually
 * control what goes to socket and what is logged. This makes sense when protocol
 * is not clear-text.
 */
public class ManualLoggingSocketWrapper extends
    AbstractSocketWrapper<LoggableInput, LoggableOutput> {

  public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

  public ManualLoggingSocketWrapper(SocketAddress endpoint, int connectionTimeoutMs,
      ConnectionLogger connectionLogger,
      WrapperFactory<LoggableInput, LoggableOutput> wrapperFactory) throws IOException {
    super(endpoint, connectionTimeoutMs, connectionLogger, wrapperFactory);
  }

  /**
   * Provides access to incoming bytes and possibly logs traffic.
   */
  public static abstract class LoggableInput {
    public abstract int readByteOrEos() throws IOException;
    public abstract byte[] readBytes(int length) throws IOException;
    public abstract ByteBuffer readUpTo0x0D0A() throws IOException;

    public abstract void markSeparatorForLog();
  }

  /**
   * Receives outgoing bytes and possibly logs traffic. Its methods allow to manually
   * control what goes into socket and what goes into log.
   */
  public static abstract class LoggableOutput {
    public abstract void writeAsciiString(String string) throws IOException;

    public abstract void writeByte(byte b) throws IOException;
    public abstract void writeByteNoLogging(byte b) throws IOException;
    public abstract void writeByteToLog(byte b) throws IOException;

    public abstract void writeBytes(byte[] bytes) throws IOException;
    public abstract void writeBytesToLog(byte[] bytes);
    public abstract void writeBytesNoLogging(byte[] bytes) throws IOException;

    /**
     * Write a string to log with a small string that may somehow annotate that this
     * string is not a clear-text out-take.
     */
    public abstract void writeToLog(String string, String annotation) throws IOException;

    public abstract void markSeparatorForLog();
  }

  public static abstract class FactoryBase
      implements WrapperFactory<LoggableInput, LoggableOutput> {
    protected static final Charset CHARSET = AbstractWsConnection.LOGGER_CHARSET;

    @Override
    public LoggableInput wrapInputStream(InputStream inputStream) {
      final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

      return new LoggableInput() {
        @Override
        public ByteBuffer readUpTo0x0D0A() throws IOException {
          ByteBuffer buffer = ByteBuffer.allocate(20);
          while (true) {
            byte b = expectByte();
            if (b == (byte) 0x0D) {
              break;
            }
            if (!buffer.hasRemaining()) {
              buffer.flip();
              ByteBuffer biggerBuffer = ByteBuffer.allocate(buffer.remaining() * 2);
              biggerBuffer.put(buffer);
              buffer = biggerBuffer;
            }
            buffer.put(b);
          }
          byte b2 = expectByte();
          if (b2 != (byte) 0x0A) {
            throw new IOException("0x0A byte expected");
          }
          buffer.flip();
          return buffer;
        }

        @Override
        public int readByteOrEos() throws IOException {
          return bufferedInputStream.read();
        }

        private byte expectByte() throws IOException {
          int next = bufferedInputStream.read();
          if (next == -1) {
            throw new IOException("Unexpected EOS");
          }
          return (byte) next;
        }

        @Override
        public byte[] readBytes(int length) throws IOException {
          byte[] result = new byte[length];
          int offset = 0;
          while (length > 0) {
            int r = bufferedInputStream.read(result, offset, length);
            if (r == -1) {
              throw new IOException("Unexpected EOS");
            }
            length -= r;
            offset += r;
          }
          return result;
        }

        @Override
        public void markSeparatorForLog() {
        }
      };
    }

    @Override
    public LoggableOutput wrapOutputStream(final OutputStream outputStream) {
      return new LoggableOutput() {
        @Override public void writeAsciiString(String string) throws IOException {
          outputStream.write(string.getBytes(UTF_8_CHARSET));
        }
        @Override public void writeByte(byte b) throws IOException {
          outputStream.write(b);
        }
        @Override public void writeBytes(byte[] bytes) throws IOException {
          outputStream.write(bytes);
        }
        @Override public void writeBytesToLog(byte[] bytes) {
        }
        @Override public void writeBytesNoLogging(byte[] bytes) throws IOException {
          outputStream.write(bytes);
        }
        @Override public void writeToLog(String string, String annotation) throws IOException {
        }
        @Override public void writeByteNoLogging(byte b) throws IOException {
          outputStream.write(b);
        }
        @Override public void writeByteToLog(byte b) throws IOException {
        }
        @Override public void markSeparatorForLog() {
        }
      };
    }

    @Override
    public LoggableInput wrapInputStream(final LoggableInput originalInputWrapper,
        final StreamListener streamListener) {
      return new LoggableInput() {
        @Override
        public ByteBuffer readUpTo0x0D0A() throws IOException {
          ByteBuffer bytes = originalInputWrapper.readUpTo0x0D0A();
          String logString =
              new String(bytes.array(), bytes.arrayOffset(), bytes.limit(), CHARSET) + "\r\n";
          streamListener.addContent(logString);
          return bytes;
        }

        @Override
        public byte[] readBytes(int length) throws IOException {
          byte[] bytes = originalInputWrapper.readBytes(length);
          String logString = new String(bytes, CHARSET);
          streamListener.addContent(logString);
          return bytes;
        }

        @Override
        public int readByteOrEos() throws IOException {
          int res = originalInputWrapper.readByteOrEos();
          if (res != -1) {
            StringBuilder builder = new StringBuilder(4);
            dumpByte((byte) res, builder);
            streamListener.addContent(builder);
          }
          return res;
        }

        @Override
        public void markSeparatorForLog() {
          streamListener.addSeparator();
        }
      };
    }

    protected static abstract class OutputWrapperBase extends LoggableOutput {
      private final LoggableOutput originalOutputWrapper;
      private final StreamListener streamListener;

      public OutputWrapperBase(LoggableOutput originalOutputWrapper,
          StreamListener streamListener) {
        this.originalOutputWrapper = originalOutputWrapper;
        this.streamListener = streamListener;
      }

      @Override
      public void writeAsciiString(String string) throws IOException {
        originalOutputWrapper.writeAsciiString(string);
        streamListener.addContent(string);
      }

      @Override
      public void writeByte(byte b) throws IOException {
        originalOutputWrapper.writeByte(b);
        dumpByte(b, getStreamListener());
      }

      @Override
      public void writeBytes(byte[] bytes) throws IOException {
        originalOutputWrapper.writeBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 4);
        for (byte b : bytes) {
          dumpByte(b, builder);
        }
        streamListener.addContent(builder);
      }

      @Override
      public void markSeparatorForLog() {
        streamListener.addSeparator();
      }

      protected LoggableOutput getOriginalOutputWrapper() {
        return originalOutputWrapper;
      }

      protected StreamListener getStreamListener() {
        return streamListener;
      }
    }
  }

  /**
   * Creates loggable input/output that logs all traffic as a non-masked ASCII text or bytes.
   * Does not employ annotations.
   */
  public static final FactoryBase PLAIN_ASCII = new FactoryBase() {
    @Override
    public LoggableOutput wrapOutputStream(
        LoggableOutput originalOutputWrapper,
        StreamListener streamListener) {
      return new OutputWrapperBase(originalOutputWrapper, streamListener) {
        @Override
        public void writeByteToLog(byte b) throws IOException {
        }

        @Override
        public void writeToLog(String string, String annotation)
            throws IOException {
        }

        @Override
        public void writeByteNoLogging(byte b) throws IOException {
          getOriginalOutputWrapper().writeByteNoLogging(b);
          dumpByte(b, getStreamListener());
        }

        @Override
        public void writeBytesToLog(byte[] bytes) {
        }

        @Override
        public void writeBytesNoLogging(byte[] bytes) throws IOException {
          getOriginalOutputWrapper().writeBytesNoLogging(bytes);
          String str = new String(bytes, CHARSET);
          getStreamListener().addContent(str);
        }
      };
    }
  };

  /**
   * Creates loggable input/output that logs all traffic as an ASCII text or bytes, or
   * demasked annotated text.
   */
  public static final FactoryBase ANNOTATED = new FactoryBase() {
    @Override
    public LoggableOutput wrapOutputStream(
        LoggableOutput originalOutputWrapper,
        StreamListener streamListener) {
      return new OutputWrapperBase(originalOutputWrapper, streamListener) {
        @Override
        public void writeByteToLog(byte b) throws IOException {
          dumpByte(b, getStreamListener());
        }

        @Override
        public void writeToLog(String string, String annotation)
            throws IOException {
          getStreamListener().addContent(annotation + "<" + string + ">");
        }

        @Override
        public void writeBytesToLog(byte[] bytes) {
          StringBuilder builder = new StringBuilder(bytes.length * 4);
          for (byte b : bytes) {
            dumpByte(b, builder);
          }
          getStreamListener().addContent(builder);
        }

        @Override
        public void writeByteNoLogging(byte b) throws IOException {
          getOriginalOutputWrapper().writeByteNoLogging(b);
        }

        @Override
        public void writeBytesNoLogging(byte[] bytes) throws IOException {
          getOriginalOutputWrapper().writeBytesNoLogging(bytes);
        }
      };
    }
  };

  private static void dumpByte(byte b, StringBuilder output) {
    AbstractWsConnection.dumpByte(b, output);
  }

  private static void dumpByte(byte b, StreamListener streamListener) {
    StringBuilder builder = new StringBuilder(4);
    dumpByte(b, builder);
    streamListener.addContent(builder);
  }
}
