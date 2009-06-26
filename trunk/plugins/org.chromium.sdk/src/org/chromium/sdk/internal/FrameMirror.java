// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Script;

/**
 * A representation of a remote JavaScript VM stack frame.
 */
public class FrameMirror {

  /**
   * A name of the script associated with the frame.
   */
  private final String scriptName;

  /**
   * 0-based line number in the entire script resource.
   */
  private final int lineNumber;

  /**
   * Function name associated with the frame.
   */
  private final String frameFunction;

  /**
   * The frame locals.
   */
  private final ValueMirror[] locals;

  /**
   * The associated script id value.
   */
  private final long scriptId;

  /**
   * A script associated with the frame.
   */
  private Script script;

  public FrameMirror(String scriptName, int line, long scriptId,
      String frameFunction, ValueMirror[] locals) {
    this.scriptName = scriptName;
    this.lineNumber = line;
    this.scriptId = scriptId;
    this.locals = locals;
    this.frameFunction = frameFunction;
  }

  public String getScriptName() {
    return scriptName;
  }

  public long getScriptId() {
    return scriptId;
  }

  /**
   * @return the 0-based line number in the resource
   */
  public int getLine() {
    return lineNumber;
  }

  public String getFunctionName() {
    return frameFunction;
  }

  public int getLocalsCount() {
    return locals == null
        ? 0
        : locals.length;
  }

  public void setScript(Script script) {
    this.script = script;
  }

  public Script getScript() {
    return script;
  }

  public ValueMirror getLocal(int idx) {
    return locals[idx];
  }
}
