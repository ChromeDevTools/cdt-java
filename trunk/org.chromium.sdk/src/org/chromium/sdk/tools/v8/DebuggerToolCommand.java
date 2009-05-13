// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

/**
 * Available V8Debugger tool commands.
 */
public enum V8DebuggerToolCommand {
  ATTACH("attach"), //$NON-NLS-1$
  DETACH("detach"), //$NON-NLS-1$
  DEBUGGER_COMMAND("debugger_command"), //$NON-NLS-1$
  EVALUATE_JAVASCRIPT("evaluate_javascript"), //$NON-NLS-1$
  ;

  public String commandName;

  private V8DebuggerToolCommand(String value) {
    this.commandName = value;
  }
}
