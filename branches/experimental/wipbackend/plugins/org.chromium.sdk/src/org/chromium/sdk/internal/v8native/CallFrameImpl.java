// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.v8native.protocol.V8ProtocolUtil;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;
import org.chromium.sdk.internal.v8native.protocol.input.ScopeRef;
import org.chromium.sdk.internal.v8native.value.JsScopeImpl;
import org.chromium.sdk.internal.v8native.value.JsVariableImpl;
import org.chromium.sdk.internal.v8native.value.PropertyReference;
import org.chromium.sdk.internal.v8native.value.ValueLoader;
import org.chromium.sdk.internal.v8native.value.ValueMirror;
import org.json.simple.JSONObject;

/**
 * A generic implementation of the CallFrame interface.
 */
public class CallFrameImpl implements CallFrame {

  /** The frame ID as reported by the JavaScript VM. */
  private final int frameId;

  /** The debug context this call frame belongs in. */
  private final InternalContext context;

  /** The underlying frame data from the JavaScript VM. */
  private final FrameObject frameObject;

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

  /** The scopes known in this call frame. */
  private final AtomicReference<List<? extends JsScope>> scopesRef =
      new AtomicReference<List<? extends JsScope>>(null);

  /** The receiver variable known in this call frame. May be null. Null is not cached. */
  private final AtomicReference<JsVariable> receiverVariableRef =
      new AtomicReference<JsVariable>(null);

  /**
   * A script associated with the frame.
   */
  private Script script;

  /**
   * Constructs a call frame for the given handler using the FrameMirror data
   * from the remote JavaScript VM.
   *
   * @param mirror frame in the VM
   * @param index call frame id (0 is the stack top)
   * @param context in which the call frame is created
   */
  public CallFrameImpl(FrameObject frameObject, InternalContext context) {
    this.frameObject = frameObject;
    this.context = context;

    int index = (int) frameObject.index();
    JSONObject func = frameObject.func();

    int currentLine = (int) frameObject.line();

    // If we stopped because of the debuggerword then we're on the next
    // line.
    // TODO(apavlov): Terry says: we need to use the [e.g. Rhino] AST to
    // decide if line is debuggerword. If so, find the next sequential line.
    // The below works for simple scripts but doesn't take into account
    // comments, etc.
    // TODO(peter.rybin): do we really need this thing? (advancing to the next line?)
    //     stopping on "debugger;" seems to be a quite natural thing.
    String srcLine = frameObject.sourceLineText();
    if (srcLine.trim().startsWith(DEBUGGER_RESERVED)) {
      currentLine++;
    }
    Long scriptRef = V8ProtocolUtil.getObjectRef(frameObject.script());
    long scriptId =
        ScriptImpl.getScriptId(context.getValueLoader().getSpecialHandleManager(), scriptRef);

    this.scriptId = scriptId;
    this.lineNumber = currentLine;
    this.frameFunction = V8ProtocolUtil.getFunctionName(func);
    this.frameId = index;
  }

  public InternalContext getInternalContext() {
    return context;
  }

  @Override
  public List<? extends JsScope> getVariableScopes() {
    ensureScopes();
    return scopesRef.get();
  }

  @Override
  public JsVariable getReceiverVariable() {
    ensureReceiver();
    return receiverVariableRef.get();
  }

  @Override
  public JsEvaluateContext getEvaluateContext() {
    return evaluateContextImpl;
  }

  private void ensureScopes() {
    if (scopesRef.get() != null) {
      return;
    }
    List<? extends JsScope> result = Collections.unmodifiableList(createScopes());
    scopesRef.compareAndSet(null, result);
  }

  private void ensureReceiver() {
    if (receiverVariableRef.get() != null) {
      return;
    }
    JsVariable result;

    PropertyReference ref = V8Helper.computeReceiverRef(frameObject);
    if (ref == null) {
      result = null;
    } else {
      ValueLoader valueLoader = context.getValueLoader();
      ValueMirror mirror =
          valueLoader.getOrLoadValueFromRefs(Collections.singletonList(ref)).get(0);
      // This name should be string. We are making it string as a fall-back strategy.
      String varNameStr = ref.getName().toString();
      result = new JsVariableImpl(valueLoader, mirror, varNameStr);
    }
    if (result != null) {
      receiverVariableRef.compareAndSet(null, result);
    }
  }

  @Override
  public TextStreamPosition getStatementStartPosition() {
    return textStreamPosition;
  }

  @Override
  public String getFunctionName() {
    return frameFunction;
  }

  @Override
  public Script getScript() {
    return script;
  }

  /**
   * @return this call frame's unique identifier within the V8 VM (0 is the top
   *         frame)
   */
  public int getIdentifier() {
    return frameId;
  }

  void hookUpScript(ScriptManager scriptManager) {
    Script script = scriptManager.findById(scriptId);
    if (script != null) {
      this.script = script;
    }
  }

  private List<JsScopeImpl<?>> createScopes() {
    List<ScopeRef> scopes = frameObject.scopes();
    List<JsScopeImpl<?>> result = new ArrayList<JsScopeImpl<?>>(scopes.size());
    for (ScopeRef scopeRef : scopes) {
      result.add(JsScopeImpl.create(this, scopeRef));
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
    @Override
    public DebugContext getDebugContext() {
      return context.getUserContext();
    }
  };

  private final TextStreamPosition textStreamPosition = new TextStreamPosition() {
    @Override public int getOffset() {
      return frameObject.position().intValue();
    }
    @Override public int getLine() {
      return lineNumber;
    }
    @Override public int getColumn() {
      Long columnObj = frameObject.column();
      if (columnObj == null) {
        return -1;
      }
      return columnObj.intValue();
    }
  };

  private static final String DEBUGGER_RESERVED = "debugger";
}
