// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import org.chromium.debug.core.model.VmResourceId;

/**
 * Defines a position within a whole source base. All numbers are 0-based.
 */
public class SourcePosition {
  private final VmResourceId id;

  /**
   * 0-based number.
   */
  private final int line;

  /**
   * 0-based number.
   */
  private final int column;

  public SourcePosition(VmResourceId id, int line, int column) {
    this.id = id;
    this.line = line;
    this.column = column;
  }

  public VmResourceId getId() {
    return id;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }
}