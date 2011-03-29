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
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.WipValueLoader.Getter;
import org.chromium.sdk.internal.wip.protocol.input.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.GetPropertiesData;
import org.chromium.sdk.internal.wip.protocol.input.PausedScriptData;
import org.chromium.sdk.internal.wip.protocol.input.WipCallFrame;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.WipScope;
import org.chromium.sdk.internal.wip.protocol.input.ValueData;
import org.chromium.sdk.internal.wip.protocol.input.GetPropertiesData.Property;
import org.chromium.sdk.internal.wip.protocol.output.ContinueRequest;
import org.chromium.sdk.internal.wip.protocol.output.EvaluateOnCallFrame;
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


  void createContext(PausedScriptData data) {
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

  void onResumeReportedFromRemote() {
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

    public WipDebugContextImpl(PausedScriptData data) {
      List<WipCallFrame> frameDataList = data.details().callFrames();
      frames = new ArrayList<CallFrameImpl>(frameDataList.size());
      for (WipCallFrame frameData : frameDataList) {
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

      ContinueRequest request = new ContinueRequest(sdkStepToProtocolStep(stepAction));
      tabImpl.getCommandProcessor().send(request, null, null);
    }

    private ContinueRequest.StepCommand sdkStepToProtocolStep(StepAction stepAction) {
      switch (stepAction) {
      case CONTINUE:
        return ContinueRequest.StepCommand.RESUME;
      case IN:
        return ContinueRequest.StepCommand.STEP_INTO;
      case OUT:
        return ContinueRequest.StepCommand.STEP_OUT;
      case OVER:
        return ContinueRequest.StepCommand.STEP_OVER;
      default:
        throw new RuntimeException();
      }
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
      private final int ordinal;
      private final int frameInjectedFrameId;
      private final List<ScopeImpl> scopes;
      private final JsVariable thisObject;
      private final TextStreamPosition streamPosition;
      private final Long sourceId;
      private ScriptImpl scriptImpl;

      public CallFrameImpl(WipCallFrame frameData) {
        functionName = frameData.functionName();
        ordinal = (int) frameData.id().ordinal();
        frameInjectedFrameId = (int) frameData.id().injectedScriptId();
        sourceId = frameData.sourceID();
        List<WipScope> scopeDataList = frameData.scopeChain();
        scopes = new ArrayList<ScopeImpl>(scopeDataList.size());

        // TODO: 'this' variable it sorted out by a brute force. Make it accurate.
        ValueData thisObjectData = null;

        for (int i = 0; i < scopeDataList.size(); i++) {
          WipScope scopeData = scopeDataList.get(i);
          // TODO(peter.rybin): provide actual scope type.
          JsScope.Type type = (i == 0) ? JsScope.Type.LOCAL : JsScope.Type.GLOBAL;
          scopes.add(new ScopeImpl(scopeData, type));
          if (thisObjectData == null) {
            thisObjectData = scopeData.thisObject();
          }
        }
        if (thisObjectData == null) {
          thisObject = null;
        } else {
          thisObject = createSimpleNameVariable("this", thisObjectData);
        }

        // 0-based.
        final int line = (int) frameData.line();

        // 0-based.
        final int column = (int) frameData.column();
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

      private JsVariable createSimpleNameVariable(final String name, ValueData valueData) {
        ValueNameBuilder valueNameBuidler = WipExpressionBuilder.createRootName(name, false);
        return valueLoader.getValueBuilder().createVariable(valueData, valueNameBuidler);
      }

      private final WipEvaluateContextImpl evaluateContext = new WipEvaluateContextImpl() {
        @Override
        protected int getCallFrameOrdinal() {
          return ordinal;
        }
        @Override
        protected int getCallFrameInjectedScriptId() {
          return frameInjectedFrameId;
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
      private final ValueData.Id objectId;
      private final Type type;

      public ScopeImpl(WipScope scopeData, Type type) {
        this.type = type;
        if (!scopeData.hasChildren()) {
          objectId = null;
          propertiesRef.initializeTrivial(EMPTY_SCOPE_VARIABLES_OPTIONAL);
        } else {
          objectId = scopeData.objectId();
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
            public Getter<ScopeVariables> process(List<? extends Property> propertyList) {
              final List<JsVariable> properties = new ArrayList<JsVariable>(propertyList.size());

              WipValueBuilder valueBuilder = valueLoader.getValueBuilder();
              for (GetPropertiesData.Property property : propertyList) {
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
        EvaluateOnCallFrame request = new EvaluateOnCallFrame(expression, getCallFrameOrdinal(),
            getCallFrameInjectedScriptId(), "watch-group", false);

        WipCommandCallback commandCallback;
        if (callback == null) {
          commandCallback = null;
        } else {
          commandCallback = new WipCommandCallback.Default() {
            @Override
            protected void onSuccess(WipCommandResponse.Success response) {
              EvaluateData data;
              try {
                data = response.data().asEvaluateData();
              } catch (JsonProtocolParseException e) {
                throw new RuntimeException(e);
              }
              ValueData valueData = data.result();

              ValueNameBuilder valueNameBuidler =
                  WipExpressionBuilder.createRootName(expression, true);

              WipValueBuilder valueBuilder = getValueLoader().getValueBuilder();
              JsVariable variable = valueBuilder.createVariable(valueData, valueNameBuidler);

              if (data.isException() == Boolean.TRUE) {
                // TODO: implement more accurate, without using string value as a user message.
                callback.failure(variable.getValue().getValueString());
              } else {
                callback.success(variable);
              }

            }
            @Override
            protected void onError(String message) {
              callback.failure(message);
            }
          };
        }
        tabImpl.getCommandProcessor().send(request, commandCallback, syncCallback);
      }

      protected abstract int getCallFrameOrdinal();
      protected abstract int getCallFrameInjectedScriptId();
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

}