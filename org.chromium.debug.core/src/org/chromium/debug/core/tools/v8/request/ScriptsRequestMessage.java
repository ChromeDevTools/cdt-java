// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "scripts" V8 request message.
 */
public class ScriptsRequestMessage extends V8DebugRequestMessage {

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
   * @param types
   *          a bitwise OR of script types to retrieve
   * @param includeSource
   *          whether to include script source in the response, default is false
   */
  public ScriptsRequestMessage(Integer types, Boolean includeSource) {
    super(V8Command.SCRIPTS.value);
    putArgument("types", types); //$NON-NLS-1$
    putArgument("includeSource", includeSource); //$NON-NLS-1$
  }
}
