// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Script;
import org.json.simple.JSONObject;

/**
 * A representation of a remote JavaScript VM call frame.
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
   * The associated script id value.
   */
  private final long scriptId;

  /**
   * The debug context in which this mirror was created.
   */
  private final InternalContext context;

  /**
   * A script associated with the frame.
   */
  private Script script;

  /**
   * The frame locals.
   */
  private ValueMirror[] locals;

  /**
   * The JSON descriptor of the frame.
   * Should be reset when the locals are resolved.
   */
  private JSONObject frameObject;

  public FrameMirror(InternalContext context, JSONObject frameObject,
      String scriptName, int line, long scriptId, String frameFunction) {
    this.context = context;
    this.frameObject = frameObject;
    this.scriptName = scriptName;
    this.lineNumber = line;
    this.scriptId = scriptId;
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
    ensureLocals();
    return locals == null
        ? 0
        : locals.length;
  }

  private void ensureLocals() {
    if (locals == null) {
      locals = context.computeLocals(frameObject);
      frameObject = null;
    }
  }

  public synchronized void setScript(Script script) {
    this.script = script;
  }

  public synchronized Script getScript() {
    return script;
  }

  public ValueMirror getLocal(int idx) {
    ensureLocals();
    return locals[idx];
  }

  JSONObject getFrameObject() {
    return frameObject;
  }
}
