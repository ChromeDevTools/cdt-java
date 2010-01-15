// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Lets read lines similar to BufferedReader, but does not buffer.
 */
public interface LineReader {
  /**
   * Method has similar semantics to {@link BufferedReader#read(char[], int, int)} method.
   */
  int read(char[] cbuf, int off, int len) throws IOException;

  /**
   * Method has similar semantics to {@link BufferedReader#readLine()} method.
   */
  String readLine() throws IOException;
}