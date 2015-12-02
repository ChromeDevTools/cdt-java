// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.List;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "scripts" V8 request message.
 */
public class ScriptsMessage extends ContextlessDebuggerMessage {

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

  /**
   * @param ids of scripts to retrieve
   * @param includeSource whether to include script source in the response,
   *        default is false
   */
  public ScriptsMessage(List<Long> ids, Boolean includeSource) {
    super(DebuggerCommand.SCRIPTS.value);
    putArgument("ids", ids);
    putArgument("includeSource", includeSource);
  }
}
