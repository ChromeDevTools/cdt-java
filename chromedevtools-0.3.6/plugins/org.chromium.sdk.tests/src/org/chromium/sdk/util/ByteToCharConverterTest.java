// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import org.junit.Test;

import junit.framework.Assert;

public class ByteToCharConverterTest {

  /**
   * Feeds the converter with a sample bytes by small chunks to check how
   * internal buffer works.
   */
  @Test
  public void testConvertByVariousChunks() {
    Random random = new Random(0);
    final int runNumber = 20;
    for (int i = 0; i < runNumber; i++) {
      for (String input : INPUT_STRINGS) {
        convertByRandomSteps(input, random);
      }
    }
  }

  private static final String[] INPUT_STRINGS = {
    "капибара",
    "1а2б3в4г5д6е7",
    "а_б_в_г_д_е",
    "Миссия Google состоит в организации мировой информации, " +
        "обеспечении ее доступности и пользы для всех."
  };

  /**
   * Converts text to bytes, then converts them back by {@link ByteToCharConverter}
   * and checks that it equals.
   */
  private void convertByRandomSteps(String text, Random random) {
    Charset charset = Charset.forName("UTF-8");
    ByteToCharConverter converter = new ByteToCharConverter(charset);

    byte[] bytes = text.getBytes(charset);
    int pos = 0;
    StringBuilder builder = new StringBuilder();
    while (pos < bytes.length) {
      int stepSize = random.nextInt(10);
      stepSize = Math.min(stepSize, bytes.length - pos);

      CharBuffer output = converter.convert(ByteBuffer.wrap(bytes, pos, stepSize));
      while (output.hasRemaining()) {
        builder.append(output.get());
      }

      pos += stepSize;
    }
    Assert.assertEquals(text, builder.toString());
  }
}
