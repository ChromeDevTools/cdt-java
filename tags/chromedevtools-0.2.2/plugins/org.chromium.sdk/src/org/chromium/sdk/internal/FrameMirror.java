// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.List;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.protocol.FrameObject;
import org.chromium.sdk.internal.tools.v8.V8Helper;

/**
 * A representation of a remote JavaScript VM call frame.
 */
public class FrameMirror {

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
   * A script associated with the frame.
   */
  private Script script;

  /**
   * The JSON descriptor of the frame.
   */
  private final FrameObject frameObject;

  public FrameMirror(FrameObject frameObject, int line, long scriptId, String frameFunction) {
    this.frameObject = frameObject;
    this.lineNumber = line;
    this.scriptId = scriptId;
    this.frameFunction = frameFunction;
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

  public int getColumn() {
    Long columnObj = frameObject.column();
    if (columnObj == null) {
      return -1;
    }
    return columnObj.intValue();
  }

  public int getOffset() {
    return frameObject.position().intValue();
  }

  public String getFunctionName() {
    return frameFunction;
  }

  public List<PropertyReference> getLocals() {
    return V8Helper.computeLocals(frameObject);
  }

  public List<ScopeMirror> getScopes() {
    return V8Helper.computeScopes(frameObject);
  }

  public PropertyReference getReceiverRef() {
    return V8Helper.computeReceiverRef(frameObject);
  }

  public synchronized void setScript(Script script) {
    this.script = script;
  }

  public synchronized Script getScript() {
    return script;
  }
}
