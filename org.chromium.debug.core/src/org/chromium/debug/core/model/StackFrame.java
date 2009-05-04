// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.chromium.debug.core.tools.v8.model.mirror.FrameMirror;
import org.chromium.debug.core.tools.v8.model.mirror.Script;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.osgi.util.NLS;

/**
 * Represents Chromium V8 VM stack frame.
 */
public class StackFrame extends DebugElementImpl implements IStackFrame {
  private final int fId;

  private FrameMirror frameM;

  private IVariable[] fVariables;

  private final JsThread thread;

  /**
   * Constructs a stack frame for the given handler using the FrameMirror data
   * from the remote V8 VM.
   *
   * @param thread
   * @param data frame data
   * @param id stack frame id (0 is the stack top)
   */
  public StackFrame(V8DebuggerToolHandler handler, FrameMirror mirror, int id) {
    super(handler);
    fId = id;
    thread = handler.getThread();
    frameM = mirror;
    init();
  }

  /**
   * Initializes this frame based on its data
   *
   * @param data
   */
  private void init() {
    int numVars = frameM.getLocalsCount();
    fVariables = new IVariable[numVars];
    int idx = 0;
    for (int i = 0; i < numVars; i++) {
      fVariables[idx++] = new Variable(this, frameM.getLocal(i));
    }
  }

  public void setFrameMirror(FrameMirror fm) {
    frameM = fm;
    init();
  }

  public IThread getThread() {
    return thread;
  }

  public IVariable[] getVariables() throws DebugException {
    return fVariables;
  }

  public boolean hasVariables() throws DebugException {
    return fVariables.length > 0;
  }

  public int getLineNumber() throws DebugException {
    Script script = frameM.getScript();
    return script != null ? frameM.getLine() - script.getLineOffset() + 1 : -1;
  }

  public int getCharStart() throws DebugException {
    return -1;
  }

  public int getCharEnd() throws DebugException {
    return -1;
  }

  public String getName() throws DebugException {
    String name = frameM.getFunctionName();
    int line = getLineNumber();
    if (line != -1) {
      name = NLS.bind(Messages.StackFrame_NameFormat, name, line);
    }
    return name;
  }

  public IRegisterGroup[] getRegisterGroups() throws DebugException {
    return null;
  }

  public boolean hasRegisterGroups() throws DebugException {
    return false;
  }

  public boolean canStepInto() {
    return getThread().canStepInto();
  }

  public boolean canStepOver() {
    return getThread().canStepOver();
  }

  public boolean canStepReturn() {
    return getThread().canStepReturn();
  }

  public boolean isStepping() {
    return getThread().isStepping();
  }

  public void stepInto() throws DebugException {
    getThread().stepInto();
  }

  public void stepOver() throws DebugException {
    getThread().stepOver();
  }

  public void stepReturn() throws DebugException {
    getThread().stepReturn();
  }

  public boolean canResume() {
    return getThread().canResume();
  }

  public boolean canSuspend() {
    return getThread().canSuspend();
  }

  public boolean isSuspended() {
    return getThread().isSuspended();
  }

  public void resume() throws DebugException {
    getThread().resume();
  }

  public void suspend() throws DebugException {
    getThread().suspend();
  }

  public boolean canTerminate() {
    return getThread().canTerminate();
  }

  public boolean isTerminated() {
    return getThread().isTerminated();
  }

  public void terminate() throws DebugException {
    getThread().terminate();
  }

  /**
   * Returns the name of the source file this stack frame is associated with.
   *
   * @return the name of the source file this stack frame is associated with
   */
  String getSourceName() {
    return frameM.getScriptName();
  }

  // Returns the external file name of script on local machine.
  public String getExternalFileName() {
    if (frameM != null && frameM.getScript() != null) {
      return frameM.getScript().getResourceName();
    }

    return null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof StackFrame) {
      StackFrame sf = (StackFrame) obj;
      try {
        return sf.getSourceName().equals(getSourceName())
            && sf.getLineNumber() == getLineNumber() && sf.fId == fId;
      } catch (DebugException e) {
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getSourceName().hashCode() + fId;
  }

  /**
   * Returns this stack frame's unique identifier within its thread
   *
   * @return this stack frame's unique identifier within its thread
   */
  public int getIdentifier() {
    return fId;
  }
}
