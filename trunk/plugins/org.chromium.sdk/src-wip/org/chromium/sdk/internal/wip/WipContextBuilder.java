// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.JsEvaluateContextBase;
import org.chromium.sdk.internal.ScriptImpl;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.WipValueLoader.Getter;
import org.chromium.sdk.internal.wip.protocol.WipProtocol;
import org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue;
import org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ScopeValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemotePropertyValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.EvaluateOnCallFrameParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.ResumeParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepIntoParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepOutParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepOverParams;
import org.chromium.sdk.util.AsyncFutureRef;

/**
 * Builder for {@link DebugContext} that works with Wip protocol.
 */
class WipContextBuilder {
  private static final Logger LOGGER = Logger.getLogger(WipContextBuilder.class.getName());

  private final WipTabImpl tabImpl;
  private WipDebugContextImpl currentContext = null;

  WipContextBuilder(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }


  void createContext(PausedEventData data) {
    if (currentContext != null) {
      LOGGER.severe("Context is already created");
      currentContext = null;
    }

    final WipDebugContextImpl context = new WipDebugContextImpl(data);
    currentContext = context;

    final List<Map<Long, ScriptImpl>> loadedScriptsHolder = new ArrayList<Map<Long,ScriptImpl>>(1);

    tabImpl.getScriptManager().loadScriptSourcesAsync(context.getScriptIds(),
        new WipScriptManager.ScriptSourceLoadCallback() {
          @Override
          public void done(Map<Long, ScriptImpl> loadedScripts) {
            loadedScriptsHolder.add(loadedScripts);
          }
        },
        new SyncCallback() {
          @Override
          public void callbackDone(RuntimeException e) {
            // Invoke next step from sync callback -- even if previous step failed.
            tabImpl.getCommandProcessor().runInDispatchThread(new Runnable() {
              @Override
              public void run() {
                if (!loadedScriptsHolder.isEmpty()) {
                  context.setScripts(loadedScriptsHolder.get(0));
                }
                tabImpl.getDebugListener().getDebugEventListener().suspended(context);
              }
            });
          }
        });
  }

  void onResumeReportedFromRemote(ResumedEventData event) {
    if (currentContext == null) {
      throw new IllegalStateException();
    }
    WipDebugContextImpl context = currentContext;
    currentContext = null;
    this.tabImpl.getDebugListener().getDebugEventListener().resumed();
    context.reportClosed();
  }

  class WipDebugContextImpl implements DebugContext {
    private final WipValueLoader valueLoader = new WipValueLoader(this);
    private final List<CallFrameImpl> frames;
    private final AtomicReference<CloseRequest> closeRequest =
        new AtomicReference<CloseRequest>(null);

    public WipDebugContextImpl(PausedEventData data) {
      List<CallFrameValue> frameDataList = data.details().callFrames();
      frames = new ArrayList<CallFrameImpl>(frameDataList.size());
      for (CallFrameValue frameData : frameDataList) {
        frames.add(new CallFrameImpl(frameData));
      }
    }

    WipValueLoader getValueLoader() {
      return valueLoader;
    }

    void reportClosed() {
      CloseRequest request = this.closeRequest.get();
      if (request != null && request.callback != null) {
        request.callback.success();
      }
    }

    Set<Long> getScriptIds() {
      Set<Long> scriptIds = new HashSet<Long>();
      for (CallFrameImpl frame : frames) {
        Long sourceId = frame.getSourceId();
        if (sourceId != null) {
          scriptIds.add(sourceId);
        }
      }
      return scriptIds;
    }

    void setScripts(Map<Long, ScriptImpl> loadedScripts) {
      for (CallFrameImpl frame : frames) {
        Long sourceId = frame.getSourceId();
        if (sourceId != null) {
          frame.setScript(loadedScripts.get(sourceId));
        }
      }
    }

    @Override
    public State getState() {
      // TODO: implement.
      return State.NORMAL;
    }

    @Override
    public ExceptionData getExceptionData() {
      // TODO: implement.
      return null;
    }

    @Override
    public Collection<? extends Breakpoint> getBreakpointsHit() {
      // TODO: implement.
      return Collections.emptyList();
    }

    @Override
    public List<? extends CallFrame> getCallFrames() {
      return frames;
    }

    @Override
    public void continueVm(StepAction stepAction, int stepCount,
        ContinueCallback callback) {

      {
        boolean updated = closeRequest.compareAndSet(null, new CloseRequest(callback));
        if (!updated) {
          throw new IllegalStateException("Continue already requested");
        }
      }

      WipParams params = sdkStepToProtocolStep(stepAction);
      tabImpl.getCommandProcessor().send(params, null, null);
    }

    @Override
    public JsEvaluateContext getGlobalEvaluateContext() {
      return WipBrowserImpl.throwUnsupported();
    }

    public WipTabImpl getTab() {
      return tabImpl;
    }

    public WipCommandProcessor getCommandProcessor() {
      return tabImpl.getCommandProcessor();
    }

    private class CloseRequest {
      final ContinueCallback callback;

      CloseRequest(ContinueCallback callback) {
        this.callback = callback;
      }
    }

    private class CallFrameImpl implements CallFrame {
      private final String functionName;
      private final String id;
      private final List<ScopeImpl> scopes;
      private final JsVariable thisObject;
      private final TextStreamPosition streamPosition;
      private final Long sourceId;
      private ScriptImpl scriptImpl;

      public CallFrameImpl(CallFrameValue frameData) {
        functionName = frameData.functionName();
        id = frameData.id();
        Object sourceIDObject = frameData.location().sourceID();
        sourceId = Long.parseLong(sourceIDObject.toString());
        List<ScopeValue> scopeDataList = frameData.scopeChain();
        scopes = new ArrayList<ScopeImpl>(scopeDataList.size());

        // TODO: 'this' variable it sorted out by a brute force. Make it accurate.
        RemoteObjectValue thisObjectData = null;

        for (int i = 0; i < scopeDataList.size(); i++) {
          ScopeValue scopeData = scopeDataList.get(i);
          // TODO(peter.rybin): provide actual scope type.
          JsScope.Type type = (i == 0) ? JsScope.Type.LOCAL : JsScope.Type.GLOBAL;
          scopes.add(new ScopeImpl(scopeData, type));
          if (thisObjectData == null) {
            thisObjectData = scopeData.getThis();
          }
        }
        if (thisObjectData == null) {
          thisObject = null;
        } else {
          thisObject = createSimpleNameVariable("this", thisObjectData);
        }

        // 0-based.
        final int line = (int) frameData.location().lineNumber();

        // 0-based.
        // TODO: check documentation, whether it's 0-based
        Long columnObject = frameData.location().columnNumber();
        final int column;
        if (columnObject == null) {
          column = 0;
        } else {
          column = columnObject.intValue();
        }
        streamPosition = new TextStreamPosition() {
          @Override public int getOffset() {
            return WipBrowserImpl.throwUnsupported();
          }
          @Override public int getLine() {
            return line;
          }

          @Override public int getColumn() {
            return column;
          }
        };
      }

      Long getSourceId() {
        return sourceId;
      }

      void setScript(ScriptImpl scriptImpl) {
        this.scriptImpl = scriptImpl;
      }

      @Override
      public Collection<? extends JsVariable> getVariables() {
        return WipBrowserImpl.throwUnsupported();
      }

      @Override
      public List<? extends JsScope> getVariableScopes() {
        return scopes;
      }

      @Override
      public JsVariable getReceiverVariable() {
        return thisObject;
      }

      @Override
      public Script getScript() {
        return scriptImpl;
      }

      @Override
      public TextStreamPosition getStatementStartPosition() {
        return streamPosition;
      }

      @Override
      public String getFunctionName() {
        return functionName;
      }

      @Override
      public JsEvaluateContext getEvaluateContext() {
        return evaluateContext;
      }

      private JsVariable createSimpleNameVariable(final String name,
          RemoteObjectValue thisObjectData) {
        ValueNameBuilder valueNameBuidler = WipExpressionBuilder.createRootName(name, false);
        return valueLoader.getValueBuilder().createVariable(thisObjectData, valueNameBuidler);
      }

      private final WipEvaluateContextImpl evaluateContext = new WipEvaluateContextImpl() {
        @Override
        protected String getCallFrameId() {
          return id;
        }
      };
    }

    @Override
    public JavascriptVm getJavascriptVm() {
      return tabImpl;
    }

    private class ScopeImpl implements JsScope {
      private final AsyncFutureRef<Getter<ScopeVariables>> propertiesRef =
          new AsyncFutureRef<Getter<ScopeVariables>>();
      private final String objectId;
      private final Type type;

      public ScopeImpl(ScopeValue scopeData, Type type) {
        this.type = type;
        if (!WipProtocol.parseHasChildren(scopeData.object().hasChildren())) {
          objectId = null;
          propertiesRef.initializeTrivial(EMPTY_SCOPE_VARIABLES_OPTIONAL);
        } else {
          objectId = scopeData.object().objectId();
        }
      }

      @Override
      public Type getType() {
        return type;
      }

      @Override
      public WithScope asWithScope() {
        // TODO: implement.
        return null;
      }

      @Override
      public List<? extends JsVariable> getVariables() {
        if (!propertiesRef.isInitialized()) {
          WipValueLoader.LoadPostprocessor<Getter<ScopeVariables>> processor =
              new WipValueLoader.LoadPostprocessor<Getter<ScopeVariables>>() {
            @Override
            public Getter<ScopeVariables> process(
                List<? extends RemotePropertyValue> propertyList) {
              final List<JsVariable> properties = new ArrayList<JsVariable>(propertyList.size());

              WipValueBuilder valueBuilder = valueLoader.getValueBuilder();
              for (RemotePropertyValue property : propertyList) {
                final String name = property.name();

                ValueNameBuilder valueNameBuilder =
                    WipExpressionBuilder.createRootName(name, false);
                JsVariable variable =
                    valueBuilder.createVariable(property.value(), valueNameBuilder);
                properties.add(variable);
              }
              final ScopeVariables scopeVariables = new ScopeVariables(properties);
              return new Getter<ScopeVariables>() {
                @Override
                ScopeVariables get() {
                  return scopeVariables;
                }
              };
            }

            @Override
            public Getter<ScopeVariables> getEmptyResult() {
              return EMPTY_SCOPE_VARIABLES_OPTIONAL;
            }

            @Override
            public Getter<ScopeVariables> forException(Exception exception) {
              return WipValueLoader.Getter.newFailure(exception);
            }
          };
          // This is blocking.
          valueLoader.loadPropertiesAsync(objectId, processor, propertiesRef);
        }

        // This is blocking.
        return propertiesRef.getSync().get().variables;
      }
    }

    private abstract class WipEvaluateContextImpl extends JsEvaluateContextBase {
      @Override
      public DebugContext getDebugContext() {
        return WipDebugContextImpl.this;
      }

      @Override
      public void evaluateAsync(final String expression,
          Map<String, String> additionalContext, final EvaluateCallback callback,
          SyncCallback syncCallback) {
        if (additionalContext != null) {
          WipBrowserImpl.throwUnsupported();
          return;
        }
        // TODO: set a proper value of injectedScriptIdInt.
        int injectedScriptIdInt = 0;
        EvaluateOnCallFrameParams params = new EvaluateOnCallFrameParams(getCallFrameId(),
            expression, "watch-group", false);

        JavascriptVm.GenericCallback<EvaluateOnCallFrameData> commandCallback;
        if (callback == null) {
          commandCallback = null;
        } else {
          commandCallback = new JavascriptVm.GenericCallback<EvaluateOnCallFrameData>() {
            @Override
            public void success(EvaluateOnCallFrameData data) {
              RemoteObjectValue valueData = data.result();

              ValueNameBuilder valueNameBuidler =
                  WipExpressionBuilder.createRootName(expression, true);

              WipValueBuilder valueBuilder = getValueLoader().getValueBuilder();
              JsVariable variable = valueBuilder.createVariable(valueData, valueNameBuidler);

              // TODO: support isException
              callback.success(variable);
            }
            @Override
            public void failure(Exception exception) {
              callback.failure(exception.getMessage());
            }
          };
        }
        tabImpl.getCommandProcessor().send(params, commandCallback, syncCallback);
      }

      protected abstract String getCallFrameId();
    }
  }

  private static class ScopeVariables {
    final List<JsVariable> variables;

    ScopeVariables(List<JsVariable> variables) {
      this.variables = variables;
    }
  }

  private final Getter<ScopeVariables> EMPTY_SCOPE_VARIABLES_OPTIONAL =
      new Getter<ScopeVariables>() {
        private final ScopeVariables value =
          new ScopeVariables(Collections.<JsVariable>emptyList());

        @Override ScopeVariables get() {
          return value;
        }
      };


  private WipParams sdkStepToProtocolStep(StepAction stepAction) {
    switch (stepAction) {
    case CONTINUE:
      return RESUME_PARAMS;
    case IN:
      return STEP_INTO_PARAMS;
    case OUT:
      return STEP_OUT_PARAMS;
    case OVER:
      return STEP_OVER_PARAMS;
    default:
      throw new RuntimeException();
    }
  }

  private static final ResumeParams RESUME_PARAMS = new ResumeParams();
  private static final StepIntoParams STEP_INTO_PARAMS = new StepIntoParams();
  private static final StepOutParams STEP_OUT_PARAMS = new StepOutParams();
  private static final StepOverParams STEP_OVER_PARAMS = new StepOverParams();
}