// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;

/**
 * A transport message encapsulating the data sent/received over the wire
 * (protocol headers and content). This class can serialize and deserialize
 * itself into a BufferedWriter according to the Chrome Developer Tools Protocol
 * specification.
 */
public class Message {

  /**
   * Known Chrome Developer Tools Protocol headers (ToolHandler implementations
   * can add their own headers.)
   */
  public enum Header {
    CONTENT_LENGTH("Content-Length"), //$NON-NLS-1$
    TOOL("Tool"), //$NON-NLS-1$
    DESTINATION("Destination"), //$NON-NLS-1$
    ;

    public String value;

    Header(String value) {
      this.value = value;
    }
  }

  private static final String TOSTRING_PREFIX = "[Message: "; //$NON-NLS-1$

  /**
   * The end of protocol header line.
   */
  private static final String HEADER_TERMINATOR = "\r\n"; //$NON-NLS-1$

  private final HashMap<String, String> headers;

  private final String content;

  /**
   * @param headers
   * @param contentString
   */
  public Message(Map<String, String> headers, String content) {
    this.headers = new HashMap<String, String>(headers);
    this.content = content;
  }

  public void sendThrough(BufferedWriter writer) throws IOException {
    String content = maskNull(this.content);
    writeNonEmptyHeader(writer, Header.CONTENT_LENGTH.value, String.valueOf(content.length()));
    for (Map.Entry<String, String> entry : this.headers.entrySet()) {
      writeNonEmptyHeader(writer, entry.getKey(), entry.getValue());
    }
    writer.write(HEADER_TERMINATOR);
    if (content.length() > 0) {
      writer.write(content);
    }
    writer.flush();
  }

  /**
   * @param reader
   * @return a new message, or null if EOS has been reached
   * @throws IOException
   */
  public static Message fromBufferedReader(BufferedReader reader)
      throws IOException {
    Map<String, String> headers = new HashMap<String, String>();
    synchronized (reader) {
      while (true) { // read headers
        String line = reader.readLine();
        if (line == null) {
          return null;
        }
        if (line.length() == 0) {
          break; // end of headers
        }
        String[] nameValue = line.split(":", 2); //$NON-NLS-1$
        if (nameValue.length != 2) {
          ChromiumDebugPlugin.logError(Messages.TransportMessage_InvalidHeaderFromRemote, line);
          return null;
        } else {
          headers.put(nameValue[0], nameValue[1]);
        }
      }

      // Read payload if applicable
      int contentLength =
          Integer.valueOf(
              getHeader(headers, Header.CONTENT_LENGTH.value, "0")); //$NON-NLS-1$
      char[] content = new char[contentLength];
      int totalRead = 0;
      while (totalRead < contentLength) {
        int readBytes =
            reader.read(content, totalRead, contentLength - totalRead);
        if (readBytes == -1) {
          // Handle End Of Stream (browser closed?).
          ChromiumDebugPlugin.logWarning(Messages.TransportMessage_EosOnInputSocket);
          return null;
        }
        totalRead += readBytes;
      }

      // Construct response message
      String contentString = new String(content);
      return new Message(headers, contentString);
    }
  }

  public String getTool() {
    return getHeader(headers, Header.TOOL.value, null);
  }

  public String getDestination() {
    return getHeader(headers, Header.DESTINATION.value, null);
  }

  /**
   * @return never null (for no content, return an empty String)
   */
  public String getContent() {
    return content;
  }

  public String getHeader(String name, String defaultValue) {
    return getHeader(this.headers, name, defaultValue);
  }

  private static String getHeader(Map<? extends String, String> headers,
      String headerName, String defaultValue) {
    String value = headers.get(headerName);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  private String maskNull(String string) {
    return string == null ? "" : string; //$NON-NLS-1$
  }

  private void writeNonEmptyHeader(BufferedWriter writer, String headerName,
      String headerValue) throws IOException {
    if (headerValue != null) {
      writer.write(buildHeader(headerName, headerValue));
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(TOSTRING_PREFIX);
    sb.append(Header.CONTENT_LENGTH.value).append('=')
        .append(content.length());
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      sb.append(',').append(entry.getKey()).append('=').append(entry.getValue());
    }
    sb.append(" content=<").append(getContent()).append(">]"); //$NON-NLS-1$ //$NON-NLS-2$
    return sb.toString();
  }

  private static String buildHeader(String name, String value) {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append(':').append(value).append(HEADER_TERMINATOR);
    return sb.toString();
  }
}
