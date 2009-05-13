// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

public class FrameMirror {
  private String scriptName;

  /**
   * 0-based line number in the entire script resource (not the workspace one)
   */
  private int lineNumber;

  private Script script;

  private String name;

  private String func;

  private ValueMirror[] locals = null;

  /**
   * Whether all frame data have been loaded
   */
  private boolean isFrameReady = false;

  public FrameMirror(String scriptName, int line, String frameName,
      String frameFunc) {
    this.scriptName = scriptName;
    this.lineNumber = line;
    this.name = frameName;
    this.func = frameFunc;
  }

  public FrameMirror(String scriptName, int line, String frameName,
      String frameFunc, ValueMirror[] values) {
    this(scriptName, line, frameName, frameFunc);

    setLocals(values);
  }

  public String getScriptName() {
    return scriptName;
  }

  public int getLine() {
    return lineNumber;
  }

  public String getFrameName() {
    return name;
  }

  public String getFunctionName() {
    return func;
  }

  public int getLocalsCount() {
    return locals == null ? 0 : locals.length;
  }

  public void setLocals(ValueMirror[] values) {
    this.locals = values;
    this.isFrameReady = true;
  }

  public void setScript(Script script) {
    this.script = script;
  }

  public Script getScript() {
    return script;
  }

  public boolean isReady() {
    return isFrameReady;
  }

  public ValueMirror getLocal(int idx) {
    return locals[idx];
  }
}
