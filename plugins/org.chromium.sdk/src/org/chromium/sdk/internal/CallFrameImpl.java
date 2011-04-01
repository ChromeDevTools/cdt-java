// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.protocol.FrameObject;
import org.chromium.sdk.internal.protocol.ScopeRef;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
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

  /** The variables known in this call frame. */
  private Collection<JsVariableImpl> variables = null;

  /** The scopes known in this call frame. */
  private List<? extends JsScope> scopes = null;

  /** The receiver variable known in this call frame. May be null. */
  private JsVariable receiverVariable;
  private boolean receiverVariableLoaded = false;

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
    String srcLine = frameObject.sourceLineText();
    if (srcLine.trim().startsWith(DEBUGGER_RESERVED)) {
      currentLine++;
    }
    Long scriptRef = V8ProtocolUtil.getObjectRef(frameObject.script());

    Long scriptId = -1L;
    if (scriptRef != null) {
      SomeHandle handle = context.getHandleManager().getHandle(scriptRef);
      if (handle != null) {
        ScriptHandle scriptHandle;
        try {
          scriptHandle = handle.asScriptHandle();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        scriptId = scriptHandle.id();
      }
    }

    this.scriptId = scriptId;
    this.lineNumber = currentLine;
    this.frameFunction = V8ProtocolUtil.getFunctionName(func);
    this.frameId = index;
  }

  public InternalContext getInternalContext() {
    return context;
  }

  @Override
  @Deprecated
  public Collection<JsVariableImpl> getVariables() {
    ensureVariables();
    return variables;
  }

  @Override
  public List<? extends JsScope> getVariableScopes() {
    ensureScopes();
    return scopes;
  }

  @Override
  public JsVariable getReceiverVariable() {
    ensureReceiver();
    return this.receiverVariable;
  }

  @Override
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
      PropertyReference ref = V8Helper.computeReceiverRef(frameObject);
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
  int getIdentifier() {
    return frameId;
  }

  void hookUpScript(ScriptManager scriptManager) {
    Script script = scriptManager.findById(scriptId);
    if (script != null) {
      this.script = script;
    }
  }

  /**
   * Initializes this frame with variables based on the frameMirror locals.
   */
  private Collection<JsVariableImpl> createVariables() {
    List<PropertyReference> refs = V8Helper.computeLocals(frameObject);
    List<ValueMirror> mirrors = context.getValueLoader().getOrLoadValueFromRefs(refs);
    Collection<JsVariableImpl> result = new ArrayList<JsVariableImpl>(refs.size());
    for (int i = 0; i < refs.size(); i++) {
      // This name should be string. We are making it string as a fall-back strategy.
      String varNameStr = refs.get(i).getName().toString();
      result.add(new JsVariableImpl(this.context, mirrors.get(i), varNameStr));
    }
    return result;
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
