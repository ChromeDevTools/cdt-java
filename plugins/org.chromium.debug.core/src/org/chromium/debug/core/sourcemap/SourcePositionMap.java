// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import org.chromium.debug.core.model.VmResourceId;

/**
 * A map between "user" and "vm" source positions. "Vm" position applies
 * to sources that are actually used inside virtual machine. "User" position
 * applies to sources that user works with.
 * All line/column numbers are 0-based.
 */
public interface SourcePositionMap {
  SourcePosition calculateVmPosition(VmResourceId id, int line, int column);

  SourcePosition calculateUserPosition(VmResourceId id, int line, int column);

  /**
   * @return current instance of token
   */
  Token getCurrentToken();

  /**
   * A token that can be kept and used later to check, whether source map has been updated.
   * This helps correctly caching mapped positions.
   */
  interface Token {
    boolean isUpdated();
  }
}
