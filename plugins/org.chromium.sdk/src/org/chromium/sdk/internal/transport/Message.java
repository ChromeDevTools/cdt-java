// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A transport message encapsulating the data sent/received over the wire
 * (protocol headers and content). This class can serialize and deserialize
 * itself into a BufferedWriter according to the ChromeDevTools Protocol
 * specification. Content-Length field is a transparent field: it gets
 * added and processed on read/write phase. It is not reported to user.
 */
public class Message {

  /**
   * This exception is thrown during Message deserialization whenever the input
   * is malformed.
   */
  public static class MalformedMessageException extends Exception {

    private static final long serialVersionUID = 1L;

    public MalformedMessageException() {
      super();
    }

    public MalformedMessageException(String message) {
      super(message);
    }

    public MalformedMessageException(Throwable cause) {
      super(cause);
    }

    public MalformedMessageException(String message, Throwable cause) {
      super(message, cause);
    }

  }

  /**
   * Known ChromeDevTools Protocol headers (ToolHandler implementations
   * can add their own headers.)
   */
  public enum Header {
    TOOL("Tool"),
    DESTINATION("Destination"), ;

    public final String name;

    Header(String value) {
      this.name = value;
    }
  }

  /**
   * The class logger.
   */
  private static final Logger LOGGER = Logger.getLogger(Message.class.getName());

  /**
   * The end of protocol header line.
   */
  private static final byte[] HEADER_TERMINATOR_BYTES = "\r\n".getBytes();

  // While normally key/value separator has additional space for readability, Chrome won't take it.
  private static byte[] FIELD_SEPARATOR_BYTES = ":".getBytes();

  private static final String CONTENT_LENGTH = "Content-Length";

  private final HashMap<String, String> headers;

  private final String content;

  public Message(Map<String, String> headers, String content) {
    this.headers = new HashMap<String, String>(headers);
    this.content = content;
  }

  /**
   * Sends a message through the specified writer.
   *
   * @param writer to send the message through
   * @throws IOException
   */
  public void sendThrough(OutputStream outputStream, Charset charset) throws IOException {
    for (Map.Entry<String, String> entry : this.headers.entrySet()) {
      String headerValue = entry.getValue();
      if (headerValue == null) {
        break;
      }
      writeHeaderField(entry.getKey(), headerValue, outputStream, charset);
    }

    String content = maskNull(this.content);
    byte[] contentBytes = content.getBytes(charset);

    writeHeaderField(CONTENT_LENGTH, String.valueOf(contentBytes.length), outputStream, charset);

    outputStream.write(HEADER_TERMINATOR_BYTES);

    outputStream.write(contentBytes);
  }

  /**
   * Reads a message from the specified reader.
   *
   * @param reader to read message from
   * @return a new message, or {@code null} if input is invalid (end-of-stream
   *         or bad message format)
   * @throws IOException
   * @throws MalformedMessageException if the input does not represent a valid
   *         message
   */
  public static Message fromBufferedReader(LineReader reader, Charset charset)
      throws IOException, MalformedMessageException {
    Map<String, String> headers = new LinkedHashMap<String, String>();

    String contentLengthValue = null;

    while (true) { // read headers
      String line = reader.readLine(charset);
      if (line == null) {
        LOGGER.fine("End of stream");
        return null;
      }
      if (line.length() == 0) {
        break; // end of headers
      }
      int semiColonPos = line.indexOf(':');
      if (semiColonPos == -1) {
        LOGGER.log(Level.SEVERE, "Bad header line: {0}", line);
        return null;
      }
      String name = line.substring(0, semiColonPos);
      String value = line.substring(semiColonPos + 1);
      String trimmedValue = value.trim();
      if (CONTENT_LENGTH.equals(name)) {
        contentLengthValue = trimmedValue;
      } else {
        headers.put(name, trimmedValue);
      }
    }

    // Read payload if applicable
    int contentLength = Integer.valueOf(contentLengthValue.trim());
    byte[] contentBytes = new byte[contentLength];
    int totalRead = 0;
    LOGGER.log(Level.FINER, "Reading payload: {0} bytes", contentLength);
    while (totalRead < contentLength) {
      int readBytes = reader.read(contentBytes, totalRead, contentLength - totalRead);
      if (readBytes == -1) {
        // End-of-stream (browser closed?)
        LOGGER.fine("End of stream while reading content");
        return null;
      }
      totalRead += readBytes;
    }

    // Construct response message
    String contentString = new String(contentBytes, charset);
    return new Message(headers, contentString);
  }

  /**
   * @return the "Tool" header value
   */
  public String getTool() {
    return getHeader(Header.TOOL.name, null);
  }

  /**
   * @return the "Destination" header value
   */
  public String getDestination() {
    return getHeader(Header.DESTINATION.name, null);
  }

  /**
   * @return the message content. Never {@code null} (for no content, returns an
   *         empty String)
   */
  public String getContent() {
    return content;
  }

  /**
   * @param name of the header
   * @param defaultValue to return if the header is not found in the message
   * @return the {@code name} header value or {@code defaultValue} if the header
   *         is not found in the message
   */
  public String getHeader(String name, String defaultValue) {
    return getHeader(this.headers, name, defaultValue);
  }

  private static String getHeader(Map<? extends String, String> headers, String headerName,
      String defaultValue) {
    String value = headers.get(headerName);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  private static String maskNull(String string) {
    return string == null
        ? ""
        : string;
  }

  @Override
  public String toString() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      this.sendThrough(stream, TO_STRING_CHARSET);
    } catch (IOException e) {
      // never occurs
      throw new RuntimeException(e);
    }
    return new String(stream.toByteArray(), TO_STRING_CHARSET);
  }

  private static void writeHeaderField(String name, String value, OutputStream outputStream,
      Charset charset) throws IOException {
    outputStream.write(name.getBytes(charset));
    outputStream.write(FIELD_SEPARATOR_BYTES);
    outputStream.write(value.getBytes(charset));
    outputStream.write(HEADER_TERMINATOR_BYTES);
  }

  private static final Charset TO_STRING_CHARSET = Charset.forName("UTF-8");
}
