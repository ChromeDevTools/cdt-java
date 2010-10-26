// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.protocol.data.FunctionValueHandle;

/**
 * A holder for the function-specific properties.
 */
public class FunctionAdditionalProperties {
  private final FunctionValueHandle jsonWithProperties;

  public FunctionAdditionalProperties(FunctionValueHandle jsonWithProperties) {
    this.jsonWithProperties = jsonWithProperties;
  }

  /**
   * @return source position or {@link #NO_POSITION} if position is not available
   */
  public int getSourcePosition() {
    return castLongToInt(jsonWithProperties.position(), NO_POSITION);
  }

  public int getLine() {
    return castLongToInt(jsonWithProperties.line(), NO_POSITION);
  }

  public int getColumn() {
    return castLongToInt(jsonWithProperties.column(), NO_POSITION);
  }

  public static final int NO_POSITION = -1;

  /**
   * @return script id or {@link #NO_SCRIPT_ID} if script is not available
   */
  public int getScriptId() {
    return castLongToInt(jsonWithProperties.scriptId(), NO_SCRIPT_ID);
  }

  public static final int NO_SCRIPT_ID = -1;

  private static int castLongToInt(Long l, int defaultValue) {
    if (l == null) {
      l = Long.valueOf(defaultValue);
    }
    return l.intValue();
  }
}
