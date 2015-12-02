// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.chromium.sdk.internal.transport.Message.MalformedMessageException;
import org.junit.Test;

public class LineReaderTest {
  /**
   * Creates {@link LineReader} on a special InputStream that returns bytes in a random small
   * chunks (to test buffer work inside {@link LineReader}). Checks that {@link LineReader}
   * works alright by reading {@link Message}'s from it.
   */
  @Test
  public void testOnRandomChunkStream() throws IOException, MalformedMessageException {
    Random random = new Random(0);
    Charset charset = Charset.forName("UTF-8");

    final int runNumber = 50;

    for (int j = 0; j < runNumber; j++) {

      List<Message> messages = new ArrayList<Message>(Arrays.asList(SAMPLES_MESSAGES));
      Collections.shuffle(messages, random);

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      for (Message message : messages) {
        message.sendThrough(byteArrayOutputStream, charset);
      }

      byte[] bytes = byteArrayOutputStream.toByteArray();

      MultiChunkInputStream inputStream = new MultiChunkInputStream(bytes, random);
      LineReader lineReader = new LineReader(inputStream);
      List<Message> reReadMessages = new ArrayList<Message>();
      while (true) {
        Message nextMessage = Message.fromBufferedReader(lineReader, charset);
        if (nextMessage == null) {
          break;
        }
        reReadMessages.add(nextMessage);
      }
      Assert.assertEquals(messages.size(), reReadMessages.size());
      for (int i = 0; i < messages.size(); i++) {
        Assert.assertEquals(messages.get(i).toString(), reReadMessages.get(i).toString());
      }
    }
  }

  private static final Message[] SAMPLES_MESSAGES = {
    new Message(createHeader(), "Test"),
    new Message(createHeader("To", "peter", "From", "google"), "Привет!"),
    new Message(createHeader("Tool", "DevToolsService", "Destination", "2"),
        "{variable={\"name\": \"result\", \"value\": \"результат\"}}"),
    new Message(createHeader("To", "abcde@ttt.com", "From", "a"), "This is just a test"),
    new Message(createHeader("To", "peter", "From", "google"), "Привет!"),
    new Message(createHeader("To", "Паша@gmail.com", "From", "vikings@gmail.com"),
        "Спам! Spam! Спам! Spam!"),
  };

  private static Map<String, String> createHeader(String ... keyAndValuePairs) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    for (int i = 0; i < keyAndValuePairs.length; i += 2) {
      result.put(keyAndValuePairs[i], keyAndValuePairs[i + 1]);
    }
    return result;
  }

  private static class MultiChunkInputStream extends InputStream {
    private static final int CHUNK_MAX_SIZE = 17;
    private final byte[] bytes;
    private final Random random;
    private int pos = 0;
    private int nextChunkEnd;

    MultiChunkInputStream(byte[] bytes, Random random) {
      this.bytes = bytes;
      this.random = random;
      this.pos = 0;
      planNextChunk();
    }

    @Override
    public int read() {
      if (pos >= bytes.length) {
        return -1;
      }
      return bytes[pos++];
    }

    @Override
    public int read(byte[] b, int off, int len) {
      if (pos >= bytes.length) {
        return -1;
      }
      if (pos >= nextChunkEnd) {
        planNextChunk();
      }
      len = Math.min(len, Math.min(nextChunkEnd, bytes.length) - pos);
      System.arraycopy(bytes, pos, b, off, len);
      pos += len;
      return len;
    }

    private void planNextChunk() {
      nextChunkEnd = pos + 1 + random.nextInt(CHUNK_MAX_SIZE);
    }
  }
}
