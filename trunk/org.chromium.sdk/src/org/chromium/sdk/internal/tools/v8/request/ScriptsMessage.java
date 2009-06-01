// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "scripts" V8 request message.
 */
public class ScriptsMessage extends DebuggerMessage {

  /**
   * Native scripts constant.
   */
  public static final int SCRIPTS_NATIVE = 1 << 0;

  /**
   * Extension scripts constant.
   */
  public static final int SCRIPTS_EXTENSION = 1 << 1;

  /**
   * Normal scripts constant.
   */
  public static final int SCRIPTS_NORMAL = 1 << 2;

  /**
   * @param types a bitwise OR of script types to retrieve
   * @param includeSource whether to include script source in the response,
   *        default is false
   */
  public ScriptsMessage(Integer types, Boolean includeSource) {
    super(DebuggerCommand.SCRIPTS.value);
    putArgument("types", types);
    putArgument("includeSource", includeSource);
  }
}
