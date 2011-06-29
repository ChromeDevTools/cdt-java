// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol;

import java.util.HashMap;
import java.util.Map;

/**
 * A V8 debugger attachment/detachment operation result.
 */
public enum Result {

  /** The operation went fine. */
  OK(0),

  /** The tab attachment status is illegal for the specified operation. */
  ILLEGAL_TAB_STATE(1),

  /** The tab specified is not known. */
  UNKNOWN_TAB(2),

  /** A generic debugger error occurred. */
  DEBUGGER_ERROR(3),

  /** An unknown command was specified. */
  UNKNOWN_COMMAND(4), ;

  public final int code;

  private static final Map<Integer, Result> codeToResult = new HashMap<Integer, Result>();

  static {
    for (Result result : values()) {
      codeToResult.put(result.code, result);
    }
  }

  private Result(int code) {
    this.code = code;
  }

  /**
   * Gets a Result value for the given code.
   *
   * @param code to look up the Result for
   * @return a Result value for {@code code} or {@code null} if code is unknown
   */
  public static Result forCode(int code) {
    return codeToResult.get(code);
  }
}
