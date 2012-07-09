// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.DatatypeConverter;

import org.chromium.sdk.internal.websocket.ManualLoggingSocketWrapper.LoggableInput;
import org.chromium.sdk.util.BasicUtil;

/**
 * WebSocket connection handshake.
 * @see http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17
 */
class Hybi17Handshake {
  static Result performHandshake(ManualLoggingSocketWrapper socket, InetSocketAddress endpoint,
      String resourceName, Random random) throws IOException {
    final ManualLoggingSocketWrapper.LoggableInput input = socket.getLoggableInput();
    ManualLoggingSocketWrapper.LoggableOutput output = socket.getLoggableOutput();

    writeHttpLine(output, "GET " + resourceName + " HTTP/1.1");

    List<String> headerFields = HandshakeUtil.createHttpFields(endpoint);
    headerFields.add("Upgrade: websocket");
    headerFields.add("Host: " + endpoint.getHostName());

    byte[] secKeyBytes = new byte[16];
    random.nextBytes(secKeyBytes);

    String secKeyString = DatatypeConverter.printBase64Binary(secKeyBytes);
    headerFields.add("Sec-WebSocket-Key: " + secKeyString);
    headerFields.add("Sec-WebSocket-Version: 13");

    Collections.shuffle(headerFields, random);

    for (String field : headerFields) {
      writeHttpLine(output, field);
    }

    writeHttpLine(output, "");

    HandshakeUtil.LineReader lineReader = new HandshakeUtil.LineReader() {
      @Override
      byte[] readUpTo0x0D0A() throws IOException {
        ByteBuffer buffer = input.readUpTo0x0D0A();
        byte[] result = new byte[buffer.limit()];
        buffer.get(result);
        return result;
      }
    };

    HandshakeUtil.HttpResponse httpResponse = HandshakeUtil.readHttpResponse(lineReader);

    if (httpResponse.getCode() != 101) {
      return processResult(input, httpResponse);
    }

    Map<String, String> responseFields = httpResponse.getFields();

    if (!"websocket".equalsIgnoreCase(responseFields.get("upgrade"))) {
      throw new IOException("Malformed response");
    }
    if (!"upgrade".equalsIgnoreCase(responseFields.get("connection"))) {
      throw new IOException("Malformed response");
    }
    if (responseFields.get("sec-websocket-extensions") != null) {
      throw new IOException("Malformed response");
    }
    if (responseFields.get("sec-websocket-protocol") != null) {
      throw new IOException("Malformed response");
    }

    String secAcceptString = responseFields.get("sec-websocket-accept");
    if (secAcceptString == null) {
      throw new IOException("Malformed response");
    }

    String expectedConcatenation = secKeyString + GUID;
    byte[] expectedAcceptSha1;
    {
      MessageDigest digest;
      try {
        digest = MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
      expectedAcceptSha1 = digest.digest(expectedConcatenation.getBytes());
    }
    String expectedAcceptString = DatatypeConverter.printBase64Binary(expectedAcceptSha1);

    if (!BasicUtil.eq(expectedAcceptString, secAcceptString)) {
      throw new IOException("Malformed response");
    }
    return CONNECTED_RESULT;
  }

  static abstract class Result {
    abstract <R> R accept(Visitor<R> visitor);

    interface Visitor<R> {
      R visitConnected();
      R visitUnknownError(Exception exception);
      R visitErrorMessage(int code, String errorName, String text);
    }

    static Result createError(final Exception exception) {
      return new Result() {
        @Override
        <R> R accept(Visitor<R> visitor) {
          return visitor.visitUnknownError(exception);
        }
      };
    }
  }

  private static final Result CONNECTED_RESULT = new Result() {
    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitConnected();
    }
  };

  private static Result processResult(LoggableInput input,
      final HandshakeUtil.HttpResponse httpResponse) throws IOException {
    Map<String, String> fields = httpResponse.getFields();
    String contentType = fields.get("content-type");
    String contentLength = fields.get("content-length");

    if ("text/html".equals(contentType) && contentLength != null) {
      int length;
      try {
        length = Integer.parseInt(contentLength);
      } catch (NumberFormatException e) {
        return Result.createError(new Exception("Failed to parse context-length field", e));
      }
      byte[] response = input.readBytes(length);
      final String contentText = new String(response, HandshakeUtil.ASCII_CHARSET);
      return new Result() {
        @Override
        <R> R accept(Visitor<R> visitor) {
          return visitor.visitErrorMessage(httpResponse.getCode(), httpResponse.getReasonPhrase(),
              contentText);
        }
      };
    }

    return Result.createError(new Exception("Error response: " + httpResponse.getCode() + " " +
        httpResponse.getReasonPhrase()));
  }


  private static void writeHttpLine(ManualLoggingSocketWrapper.LoggableOutput output, String line)
      throws IOException {
    output.writeAsciiString(line + "\r\n");
  }

  private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
}
