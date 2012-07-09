// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods, classes and constants used in all WebSocket handshake procedures.
 */
public class HandshakeUtil {
  public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
  public static final Charset ASCII_CHARSET = Charset.forName("ASCII");

  static ArrayList<String> createHttpFields(InetSocketAddress endpoint) {
    ArrayList<String> fields = new ArrayList<String>();
    fields.add("Connection: Upgrade");
    return fields;
  }

  static void checkOriginString(String origin) {
    for (int i = 0; i < origin.length(); i++) {
      char ch = origin.charAt(i);
      if (ch >= 'A' && ch <= 'Z') {
        throw new IllegalArgumentException();
      }
    }
  }

  public interface HttpResponse {
    int getCode();
    Map<String, String> getFields();
    String getReasonPhrase();
  }

  public static HttpResponse readHttpResponse(LineReader input) throws IOException {
    final int code;
    final String reasonPhrase;
    // First line.
    {
      byte[] firstLine = input.readUpTo0x0D0A();
      if (firstLine.length < 7 - 2) {
        throw new IOException("Malformed response");
      }
      int space1Pos = byteIndexOf((byte)' ', firstLine, 0);
      if (space1Pos == -1) {
        throw new IOException("Malformed response");
      }
      int space2Pos = byteIndexOf((byte)' ', firstLine, space1Pos + 1);
      if (space2Pos == -1) {
        throw new IOException("Malformed response");
      }
      if (space2Pos - space1Pos != 4) {
        throw new IOException("Malformed response");
      }
      int codeTemp = 0;
      for (int i = space1Pos + 1; i < space2Pos; i++) {
        codeTemp = codeTemp * 10 + firstLine[i] - (byte)'0';
      }
      code = codeTemp;
      int reasonPhraseStart = space2Pos + 1;
      reasonPhrase = new String(firstLine, reasonPhraseStart, firstLine.length - space2Pos - 1,
          ASCII_CHARSET);
    }

    // Fields.
    final Map<String, String> responseFields;
    {
      responseFields = new HashMap<String, String>();
      while (true) {
        byte[] line = input.readUpTo0x0D0A();
        if (line.length == 0) {
          break;
        }
        String lineStr = new String(line, UTF_8_CHARSET);
        int colonPos = lineStr.indexOf(':');
        if (colonPos == -1) {
          throw new IOException("Malformed response field");
        }
        if (colonPos == 0) {
          throw new IOException("Malformed response field: empty key");
        }
        String key = lineStr.substring(0, colonPos).toLowerCase();
        if (lineStr.length() > colonPos + 1 && lineStr.charAt(colonPos + 1) == ' ') {
          colonPos++;
        }
        String value = lineStr.substring(colonPos + 1);
        Object conflict = responseFields.put(key, value);
        if (conflict != null) {
          throw new IOException("Malformed response field: duplicated field: " + key);
        }
      }
    }
    return new HttpResponse() {
      @Override public int getCode() {
        return code;
      }
      @Override public String getReasonPhrase() {
        return reasonPhrase;
      }
      @Override public Map<String, String> getFields() {
        return responseFields;
      }
    };
  }

  public static abstract class LineReader {
    abstract byte[] readUpTo0x0D0A() throws IOException;
  }

  public static LineReader createLineReader(final InputStream input) {
    return new LineReader() {
      @Override
      byte[] readUpTo0x0D0A() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (true) {
          // TODO(peter.rybin): this is slow (for connection logger implementation).
          int i = input.read();
          if (i == -1) {
            throw new IOException("End of stream");
          }
          byte b = (byte) i;
          if (b == 0x0D) {
            break;
          }
          if (b == 0x0A) {
            throw new IOException("Malformed end of line");
          }
          outputStream.write(b);
        }
        {
          int i = input.read();
          if (i == -1) {
            throw new IOException("End of stream");
          }
          byte b = (byte) i;
          if (b != 0x0A) {
            throw new IOException("Malformed end of line");
          }
        }
        return outputStream.toByteArray();
      }
    };
  }

  private static int byteIndexOf(byte b, byte[] array, int start) {
    int i = start;
    while (i < array.length) {
      if (array[i] == b) {
        return i;
      }
      i++;
    }
    return -1;
  }
}
