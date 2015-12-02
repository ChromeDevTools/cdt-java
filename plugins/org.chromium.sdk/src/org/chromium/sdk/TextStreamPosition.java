// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

/**
 * A structure that defines position within text stream (file).
 * There are 2 possible notations: offset from the beginning of the stream (in characters)
 * and line/column pair. This structure contains numbers in both notations.
 */
public interface TextStreamPosition {
  /**
   * Returns 0-based offset from the beginning of the stream/file measured in Unicode characters.
   * @return offset from the beginning of the stream/file or {@link #NO_POSITION} if value
   *     is not available
   */
  int getOffset();

  /**
   * Returns 0-based line number within the stream/file.
   * @return 0-based line number or {@link #NO_POSITION} if value is not available
   */
  int getLine();

  /**
   * Returns 0-based column number within the line.
   * @return 0-based column number or {@link #NO_POSITION} if value is not available
   */
  int getColumn();

  int NO_POSITION = -1;
}
