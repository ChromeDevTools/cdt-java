// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

/**
 * A holder for the function-specific properties.
 */
public class FunctionAdditionalProperties {
  private final int sourcePosition;
  private final int scriptId;

  public FunctionAdditionalProperties(int sourcePosition, int scriptId) {
    this.sourcePosition = sourcePosition;
    this.scriptId = scriptId;
  }

  /**
   * @return source position or {@link #NO_POSITION} if position is not available
   */
  public int getSourcePosition() {
    return sourcePosition;
  }

  public static final int NO_POSITION = -1;

  /**
   * @return script id or {@link #NO_SCRIPT_ID} if script is not available
   */
  public int getScriptId() {
    return scriptId;
  }

  public static final int NO_SCRIPT_ID = -1;
}
