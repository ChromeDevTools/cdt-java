// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * A stateful converter from bytes to characters according to a specified {@link Charset}.
 * The byte input may end with an incomplete character code (e.g. in UTF-8 one character
 * is coded up to 4 bytes). In this case the partial character code is saved in an internal
 * buffer, thus the statefulness.
 */
public class ByteToCharConverter {
  private static final int REMAINDER_MAX_SIZE = 10;
  private final CharsetDecoder decoder;
  private final ByteBuffer unprocessedBuffer;

  public ByteToCharConverter(Charset charset) {
    decoder = charset.newDecoder();
    unprocessedBuffer = ByteBuffer.wrap(new byte[REMAINDER_MAX_SIZE]);
    unprocessedBuffer.flip();
  }

  public CharBuffer convert(ByteBuffer input) {
    CharBuffer result = convertImpl(input);
    result.flip();
    return result;
  }

  private CharBuffer convertImpl(ByteBuffer input) {
    CharBuffer out = CharBuffer.allocate(input.remaining() + unprocessedBuffer.remaining() + 1);
    // Process what has left from previous call.
    if (unprocessedBuffer.hasRemaining()) {
      while (true) {
        if (!input.hasRemaining()) {
          return out;
        }
        unprocessedBuffer.compact();
        unprocessedBuffer.put(input.get());
        unprocessedBuffer.flip();
        CoderResult res = decoder.decode(unprocessedBuffer, out, false);
        if (!res.isUnderflow()) {
          throw new RuntimeException("Unexpected error: " + res);
        }
        if (unprocessedBuffer.position() > 0) {
          assert !unprocessedBuffer.hasRemaining();
          break;
        }
      }
    }
    if (!input.hasRemaining()) {
      return out;
    }
    // Process main bulk.
    CoderResult res = decoder.decode(input, out, false);
    if (!res.isUnderflow()) {
      throw new RuntimeException("Unexpected error: " + res);
    }
    // Save what remained for future processing.
    unprocessedBuffer.clear();
    while (input.hasRemaining()) {
      unprocessedBuffer.put(input.get());
    }
    unprocessedBuffer.flip();
    return out;
  }
}