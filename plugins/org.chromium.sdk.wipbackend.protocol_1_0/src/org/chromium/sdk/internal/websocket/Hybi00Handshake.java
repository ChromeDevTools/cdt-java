// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.chromium.sdk.internal.transport.SocketWrapper;

/**
 * A more or less straightforward implementation of WebSocket client-side handshake
 * as defined in Internet-Draft of May 23, 2010.
 * See http://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-00
 * <p>Note that the standard was reworked completely. This implementation is obsolete. However
 * it is still compatible with the current Chrome implementation.
 */
class Hybi00Handshake {
  static void performHandshake(SocketWrapper socket, InetSocketAddress endpoint,
      String resourceName, String origin, Random random) throws IOException {
    HandshakeUtil.checkOriginString(origin);

    OutputStream output = socket.getLoggableOutput().getOutputStream();
    Writer outputWriter = new OutputStreamWriter(output, HandshakeUtil.UTF_8_CHARSET);

    outputWriter.write("GET " + resourceName + " HTTP/1.1\r\n");

    List<String> fields = HandshakeUtil.createHttpFields(endpoint);
    fields.add("Upgrade: WebSocket");
    fields.add("Origin: " + origin);
    int port = endpoint.getPort();
    String portSuffix = port == 80 ? "" : ":" + port;
    fields.add("Host: " + endpoint.getHostName() + portSuffix);
    WsKey key1 = new WsKey(random);
    WsKey key2 = new WsKey(random);
    fields.add("Sec-WebSocket-Key1: " + key1.getKeySocketField());
    fields.add("Sec-WebSocket-Key2: " + key2.getKeySocketField());

    Collections.shuffle(fields, random);
    for (String field : fields) {
      outputWriter.write(field);
      outputWriter.write("\r\n");
    }
    outputWriter.write("\r\n");
    byte[] key3 = new byte[8];
    random.nextBytes(key3);

    outputWriter.flush();

    output.write(key3);
    output.flush();


    byte[] expectedMd5Bytes;
    {
      // Challenge.
      ByteArrayOutputStream challengeBytes = new ByteArrayOutputStream(16);
      writeIntBigEndian(key1.getNumber(), challengeBytes);
      writeIntBigEndian(key2.getNumber(), challengeBytes);
      challengeBytes.write(key3);
      MessageDigest digest;
      try {
        digest = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
      expectedMd5Bytes = digest.digest(challengeBytes.toByteArray());
    }

    final InputStream input = socket.getLoggableInput().getInputStream();

    HandshakeUtil.LineReader lineReader = HandshakeUtil.createLineReader(input);

    HandshakeUtil.HttpResponse httpResponse = HandshakeUtil.readHttpResponse(lineReader);

    if (httpResponse.getCode() != 101) {
      throw new IOException("Unexpected response code " + httpResponse.getCode());
    }

    Map<String, String> responseFields = httpResponse.getFields();

    if (responseFields.size() != EXPECTED_FIELDS.size()) {
      throw new IOException("Malformed response");
    }
    if (!responseFields.keySet().containsAll(EXPECTED_FIELDS)) {
      throw new IOException("Malformed response");
    }
    if (!"WebSocket".equals(responseFields.get("upgrade"))) {
      throw new IOException("Malformed response");
    }
    if (!"upgrade".equalsIgnoreCase(responseFields.get("connection"))) {
      throw new IOException("Malformed response");
    }
    if (!origin.equals(responseFields.get("sec-websocket-origin"))) {
      throw new IOException("Malformed response");
    }
    String expectedUrl = createUrl(endpoint, resourceName, false);
    if (!expectedUrl.equals(responseFields.get("sec-websocket-location"))) {
      throw new IOException("Malformed response: unexpected sec-websocket-location");
    }

    {
      // Challenge response.
      byte[] actualMd5Bytes = new byte[16];
      {
        int readPos = 0;
        while (readPos < actualMd5Bytes.length) {
          int readRes = input.read(actualMd5Bytes, readPos, actualMd5Bytes.length - readPos);
          if (readRes == -1) {
            throw new IOException("End of stream");
          }
          readPos += readRes;
        }
      }
      if (!Arrays.equals(expectedMd5Bytes, actualMd5Bytes)) {
        throw new IOException("Wrong challenge response: expected=" +
            Arrays.toString(expectedMd5Bytes) + " recieved=" + Arrays.toString(actualMd5Bytes));
      }
    }
  }

  private static String createUrl(InetSocketAddress endpoint, String resourceName,
      boolean secure) {
    boolean needPort;
    if (secure) {
      needPort = endpoint.getPort() != 443;
    } else {
      needPort = endpoint.getPort() != 80;
    }
    return (secure ? "wss://" : "ws://") +
        endpoint.getHostName() +
        (needPort ? ":" + endpoint.getPort() : "") +
        resourceName;
  }

  private static void writeIntBigEndian(long value, OutputStream output) throws IOException {
    output.write((byte) ((value & 0xFF000000L) >> (3 * 8)));
    output.write((byte) ((value & 0xFF0000L) >> (2 * 8)));
    output.write((byte) ((value & 0xFF00L) >> (1 * 8)));
    output.write((byte) ((value & 0xFFL)));
  }

  private static final Set<String> EXPECTED_FIELDS = new HashSet<String>(Arrays.asList(
      "upgrade",
      "connection",
      "sec-websocket-origin",
      "sec-websocket-location"
      ));

  private static class WsKey {
    private static final long SPEC_MAX = 4294967295l;

    private final long resNumber;
    private final String keyString;

    WsKey(Random random) {
      int spaces = random.nextInt(12) + 1;
      long max = SPEC_MAX / spaces;
      long number = Math.abs(random.nextLong()) % (max + 1);
      resNumber = number;
      long product = number * spaces;
      assert(product <= SPEC_MAX);

      String productStr = Long.toString(product);
      List<Byte> keyBytes = new ArrayList<Byte>(40);
      keyBytes.addAll(Collections.nCopies(productStr.length(), (byte) '1'));
      int stuffByteNumber = random.nextInt(12) + 1;
      for (int i = 0; i < stuffByteNumber; i++) {
        keyBytes.add(StuffBytes.getByte(random));
      }
      Collections.shuffle(keyBytes, random);
      keyBytes.subList(0, keyBytes.size() - 1).addAll(Collections.nCopies(spaces, (byte) ' '));
      Collections.shuffle(keyBytes.subList(1, keyBytes.size() - 1), random);
      byte[] resultBytes = new byte[keyBytes.size()];
      int strPos = 0;
      for (int i = 0; i < resultBytes.length; i++) {
        byte b = keyBytes.get(i);
        if (b == (byte) '1') {
          b = (byte) productStr.charAt(strPos);
          strPos++;
        }
        resultBytes[i] = b;
      }
      assert(strPos == productStr.length());
      keyString = new String(resultBytes, HandshakeUtil.ASCII_CHARSET);
    }

    String getKeySocketField() {
      return keyString;
    }

    long getNumber() {
      return resNumber;
    }

    private static class StuffBytes {
      private static byte RANGE_1_BEGIN = 0x21;
      private static byte RANGE_1_END = 0x2F + 1;
      private static byte RANGE_2_BEGIN = 0x3A;
      private static byte RANGE_2_END = 0x7E + 1;

      private static int RANDOM_RANGE_1 = RANGE_1_END - RANGE_1_BEGIN;
      private static int RANDOM_RANGE = RANDOM_RANGE_1 + RANGE_2_END - RANGE_2_BEGIN;

      private static byte getByte(Random random) {
        int i = random.nextInt(RANDOM_RANGE);
        if (i < RANDOM_RANGE_1) {
          return (byte) (i + RANGE_1_BEGIN);
        } else {
          return (byte) (i + - RANDOM_RANGE_1 + RANGE_2_BEGIN);
        }
      }
    }
  }
}
