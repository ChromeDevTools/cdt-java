// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.CallFrame;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;

/**
 * A generic implementation of the CallFrame interface.
 */
public class CallFrameImpl implements CallFrame {

  /** The frame ID as reported by the JavaScript VM. */
  private final int frameId;

  /** The debug context this call frame belongs in. */
  private final InternalContext context;

  /** The underlying frame data from the JavaScript VM. */
  private final FrameMirror frameMirror;

  /** The variables known in this call frame. */
  private Collection<JsVariableImpl> variables = null;

  /** The scopes known in this call frame. */
  private List<? extends JsScope> scopes = null;

  /** The receiver variable known in this call frame. May be null. */
  private JsVariable receiverVariable;
  private boolean receiverVariableLoaded = false;

  /**
   * Constructs a call frame for the given handler using the FrameMirror data
   * from the remote JavaScript VM.
   *
   * @param mirror frame in the VM
   * @param index call frame id (0 is the stack top)
   * @param context in which the call frame is created
   */
  public CallFrameImpl(FrameMirror mirror, int index, InternalContext context) {
    this.context = context;
    this.frameId = index;
    this.frameMirror = mirror;
  }

  public InternalContext getInternalContext() {
    return context;
  }

  @Deprecated
  public Collection<JsVariableImpl> getVariables() {
    ensureVariables();
    return variables;
  }

  public List<? extends JsScope> getVariableScopes() {
    ensureScopes();
    return scopes;
  }

  public JsVariable getReceiverVariable() {
    ensureReceiver();
    return this.receiverVariable;
  }

  public JsEvaluateContext getEvaluateContext() {
    return evaluateContextImpl;
  }

  private void ensureVariables() {
    if (variables == null) {
      this.variables = Collections.unmodifiableCollection(createVariables());
    }
  }

  private void ensureScopes() {
    if (scopes == null) {
      this.scopes = Collections.unmodifiableList(createScopes());
    }
  }

  private void ensureReceiver() {
    if (!receiverVariableLoaded) {
      PropertyReference ref = frameMirror.getReceiverRef();
      if (ref == null) {
        this.receiverVariable = null;
      } else {
        ValueLoader valueLoader = context.getValueLoader();
        ValueMirror mirror =
            valueLoader.getOrLoadValueFromRefs(Collections.singletonList(ref)).get(0);
        // This name should be string. We are making it string as a fall-back strategy.
        String varNameStr = ref.getName().toString();
        this.receiverVariable = new JsVariableImpl(this.context, mirror, varNameStr);
      }
      this.receiverVariableLoaded = true;
    }
  }

  public int getLineNumber() {
    Script script = frameMirror.getScript();
    // Recalculate respective to the script start
    // (frameMirror.getLine() returns the line offset in the resource).
    return script != null
        ? frameMirror.getLine() - script.getStartLine()
        : -1;
  }

  public int getCharStart() {
    return -1;
  }

  public int getCharEnd() {
    return -1;
  }

  public String getFunctionName() {
    return frameMirror.getFunctionName();
  }

  public Script getScript() {
    return frameMirror.getScript();
  }

  /**
   * @return this call frame's unique identifier within the V8 VM (0 is the top
   *         frame)
   */
  int getIdentifier() {
    return frameId;
  }

  /**
   * Initializes this frame with variables based on the frameMirror locals.
   */
  private Collection<JsVariableImpl> createVariables() {
    List<PropertyReference> refs = frameMirror.getLocals();
    List<ValueMirror> mirrors = context.getValueLoader().getOrLoadValueFromRefs(refs);
    Collection<JsVariableImpl> result = new ArrayList<JsVariableImpl>(refs.size());
    for (int i = 0; i < refs.size(); i++) {
      // This name should be string. We are making it string as a fall-back strategy.
      String varNameStr = refs.get(i).getName().toString();
      result.add(new JsVariableImpl(this.context, mirrors.get(i), varNameStr));
    }
    return result;
  }

  private List<JsScopeImpl> createScopes() {
    List<ScopeMirror> scopes = frameMirror.getScopes();
    List<JsScopeImpl> result = new ArrayList<JsScopeImpl>(scopes.size());
    for (ScopeMirror mirror : scopes) {
      result.add(new JsScopeImpl(this, mirror));
    }
    return result;
  }

  private final JsEvaluateContextImpl evaluateContextImpl = new JsEvaluateContextImpl() {
    @Override
    protected Integer getFrameIdentifier() {
      return getIdentifier();
    }
    @Override
    public InternalContext getInternalContext() {
      return context;
    }
  };
}
