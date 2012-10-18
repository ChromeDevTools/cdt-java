// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugContext.StepAction;
import org.chromium.sdk.ExceptionData;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.RestartFrameExtension;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.WipValueLoader.Getter;
import org.chromium.sdk.internal.wip.protocol.WipParserAccess;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue;
import org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.RestartFrameData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ScopeValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.InternalPropertyDescriptorValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.PropertyDescriptorValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue;
import org.chromium.sdk.internal.wip.protocol.output.WipParams;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.debugger.EvaluateOnCallFrameParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.RestartFrameParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.ResumeParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepIntoParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepOutParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.StepOverParams;
import org.chromium.sdk.internal.wip.protocol.output.runtime.EvaluateParams;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.LazyConstructable;
import org.chromium.sdk.util.MethodIsBlockingException;
import org.chromium.sdk.util.RelaySyncCallback;
import org.json.simple.JSONObject;

/**
 * Builder for {@link DebugContext} that works with Wip protocol.
 */
class WipContextBuilder {
  private static final Logger LOGGER = Logger.getLogger(WipContextBuilder.class.getName());

  private final WipTabImpl tabImpl;
  private final EvaluateHack evaluateHack;
  private WipDebugContextImpl currentContext = null;

  WipContextBuilder(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
    this.evaluateHack = new EvaluateHack(tabImpl);
  }

  // Called from Dispatch Thread.
  RelayOk updateStackTrace(List<CallFrameValue> callFrames,
      GenericCallback<Void> callback, final SyncCallback syncCallback) {
    if (currentContext == null) {
      if (callback != null) {
        callback.success(null);
      }
      return RelaySyncCallback.finish(syncCallback);
    } else {
      return currentContext.setFrames(callFrames, callback, syncCallback);
    }
  }

  void createContext(PausedEventData data) {
    if (currentContext != null) {
      LOGGER.severe("Context is already created");
      currentContext = null;
    }

    final WipDebugContextImpl context = new WipDebugContextImpl(data);
    currentContext = context;

    GenericCallback<Void> callback = new GenericCallback<Void>() {
      @Override
      public void success(Void value) {
        tabImpl.getDebugListener().getDebugEventListener().suspended(context);
      }

      @Override
      public void failure(Exception exception) {
        throw new RuntimeException(exception);
      }
    };

    context.setFrames(data.callFrames(), callback, null);
  }

  EvaluateHack getEvaluateHack() {
    return evaluateHack;
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
    private volatile List<CallFrameImpl> frames = null;
    private final ExceptionData exceptionData;
    private final AtomicReference<CloseRequest> closeRequest =
        new AtomicReference<CloseRequest>(null);
    private final JsEvaluateContext globalContext;

    public WipDebugContextImpl(PausedEventData data) {
      PausedEventData.Data additionalData = data.data();
      if (data.reason() == PausedEventData.Reason.EXCEPTION && additionalData != null) {
        RemoteObjectValue exceptionRemoteObject;
        try {
          JSONObject additionalDataObject = additionalData.getUnderlyingObject();
          exceptionRemoteObject =
              WipParserAccess.get().parseRemoteObjectValue(additionalDataObject);
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException("Failed to parse exception data", e);
        }
        JsValue exceptionValue =
            valueLoader.getValueBuilder().wrap(exceptionRemoteObject, null);
        exceptionData = new ExceptionDataImpl(exceptionValue);
      } else {
        exceptionData = null;
      }
      globalContext = new GlobalEvaluateContext(getValueLoader());
    }

    RelayOk setFrames(List<CallFrameValue> frameDataList,
        final GenericCallback<Void> callback, final SyncCallback syncCallback) {
      frames = new ArrayList<CallFrameImpl>(frameDataList.size());
      for (CallFrameValue frameData : frameDataList) {
        frames.add(new CallFrameImpl(frameData));
      }

      return tabImpl.getScriptManager().loadScriptSourcesAsync(getScriptIds(),
          new WipScriptManager.ScriptSourceLoadCallback() {
            @Override
            public void done(Map<String, WipScriptImpl> loadedScripts) {
              setScripts(loadedScripts);

              if (callback != null) {
                callback.success(null);
              }
            }
          },
          syncCallback);
    }

    void resetFrames(List<CallFrameValue> frameDataList) {
      List<CallFrameImpl> newFrames = new ArrayList<CallFrameImpl>(frameDataList.size());
      for (int i = 0; i < frameDataList.size(); i++) {
        CallFrameImpl callFrameImpl = new CallFrameImpl(frameDataList.get(i));
        callFrameImpl.setScript(frames.get(i).getScript());
        newFrames.add(callFrameImpl);
      }
      frames = newFrames;
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

    Set<String> getScriptIds() {
      Set<String> scriptIds = new HashSet<String>();
      for (CallFrameImpl frame : frames) {
        String sourceId = frame.getSourceId();
        if (sourceId != null) {
          scriptIds.add(sourceId);
        }
      }
      return scriptIds;
    }

    void setScripts(Map<String, WipScriptImpl> loadedScripts) {
      for (CallFrameImpl frame : frames) {
        String sourceId = frame.getSourceId();
        if (sourceId != null) {
          WipScriptImpl script = getSafe(loadedScripts, sourceId);
          // Script can be null.
          frame.setScript(script);
        }
      }
    }

    @Override
    public State getState() {
      if (exceptionData == null) {
        return State.NORMAL;
      } else {
        return State.EXCEPTION;
      }
    }

    @Override
    public ExceptionData getExceptionData() {
      return exceptionData;
    }

    @Override
    public Collection<? extends Breakpoint> getBreakpointsHit() {
      if (frames.isEmpty()) {
        return Collections.emptyList();
      }
      CallFrameImpl topFrame = frames.get(0);
      return tabImpl.getBreakpointManager().findRelatedBreakpoints(topFrame);
    }

    @Override
    public List<? extends CallFrame> getCallFrames() {
      return frames;
    }

    @Override
    public void continueVm(StepAction stepAction, int stepCount,
        ContinueCallback callback) {
      continueVm(stepAction, stepCount, callback, null);
    }

    @Override
    public RelayOk continueVm(StepAction stepAction, int stepCount,
        final ContinueCallback callback, SyncCallback syncCallback) {
      {
        boolean updated = closeRequest.compareAndSet(null, new CloseRequest(callback));
        if (!updated) {
          throw new IllegalStateException("Continue already requested");
        }
      }

      WipParams params = sdkStepToProtocolStep(stepAction);

      WipCommandCallback commandCallback;
      if (callback == null) {
        commandCallback = null;
      } else {
        commandCallback = new WipCommandCallback() {
          @Override public void messageReceived(WipCommandResponse response) {
            callback.success();
          }
          @Override public void failure(String message) {
            callback.failure(message);
          }
        };
      }
      return tabImpl.getCommandProcessor().send(params, commandCallback, syncCallback);
    }

    @Override
    public JsEvaluateContext getGlobalEvaluateContext() {
      return globalContext;
    }

    @Override
    public RemoteValueMapping getDefaultRemoteValueMapping() {
      return valueLoader;
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

    class CallFrameImpl implements CallFrame {
      private final String functionName;
      private final String id;
      private final LazyConstructable<List<JsScope>> scopeData;
      private final JsVariable thisObject;
      private final TextStreamPosition streamPosition;
      private final String sourceId;
      private WipScriptImpl scriptImpl;

      public CallFrameImpl(CallFrameValue frameData) {
        functionName = frameData.functionName();
        id = frameData.callFrameId();
        sourceId = frameData.location().scriptId();
        final List<ScopeValue> scopeDataList = frameData.scopeChain();

        scopeData = LazyConstructable.create(new LazyConstructable.Factory<List<JsScope>>() {
          @Override
          public List<JsScope> construct() {
            final List<JsScope> scopes = new ArrayList<JsScope>(scopeDataList.size());

            for (int i = 0; i < scopeDataList.size(); i++) {
              ScopeValue scopeData = scopeDataList.get(i);
              scopes.add(createScope(scopeData, valueLoader));
            }
            return scopes;
          }
        });

        RemoteObjectValue thisObjectData = frameData.getThis();
        if (thisObjectData == null) {
          LOGGER.log(Level.SEVERE, "Missing local scope", new Exception());
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

      String getSourceId() {
        return sourceId;
      }

      void setScript(WipScriptImpl scriptImpl) {
        this.scriptImpl = scriptImpl;
      }

      @Override
      public List<? extends JsScope> getVariableScopes() {
        return scopeData.get();
      }

      @Override
      public JsVariable getReceiverVariable() {
        return thisObject;
      }

      @Override
      public WipScriptImpl getScript() {
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

      private final WipEvaluateContextBase<?> evaluateContext =
          new WipEvaluateContextBase<EvaluateOnCallFrameData>(getValueLoader()) {
        @Override
        protected WipParamsWithResponse<EvaluateOnCallFrameData> createRequestParams(
            String expression, WipValueLoader destinationValueLoader) {
          return new EvaluateOnCallFrameParams(id, expression,
              destinationValueLoader.getObjectGroupId(), false, null, false);
        }

        @Override protected RemoteObjectValue getRemoteObjectValue(EvaluateOnCallFrameData data) {
          return data.result();
        }

        @Override protected Boolean getWasThrown(EvaluateOnCallFrameData data) {
          return data.wasThrown();
        }
      };

      RelayOk restart(final GenericCallback<Boolean> callback,
          SyncCallback syncCallback) {

        RelaySyncCallback relaySyncCallback = new RelaySyncCallback(syncCallback);

        final RelaySyncCallback.Guard guard = relaySyncCallback.newGuard();

        RestartFrameParams params = new RestartFrameParams(id);
        WipCommandProcessor commandProcessor = valueLoader.getTabImpl().getCommandProcessor();
        GenericCallback<RestartFrameData> commandCallback =
            new GenericCallback<RestartFrameData>() {
          @Override
          public void success(RestartFrameData value) {
            RelayOk relayOk = handleRestartFrameData(value, callback, guard.getRelay());
            guard.discharge(relayOk);
          }

          @Override
          public void failure(Exception exception) {
            if (callback != null) {
              callback.failure(exception);
            }
          }
        };
        return commandProcessor.send(params, commandCallback, guard.asSyncCallback());
      }

      private RelayOk handleRestartFrameData(RestartFrameData data,
          final GenericCallback<Boolean> callback, RelaySyncCallback relay) {
        // We are in Dispatch thread.
        if (currentContext != WipDebugContextImpl.this) {
          return finishSuccessfulRestart(false, callback, relay);
        }
        if (data.result().getUnderlyingObject().get("stack_update_needs_step_in") ==
            Boolean.TRUE) {
          final RelaySyncCallback.Guard guard = relay.newGuard();
          final ContinueCallback continueCallback = new ContinueCallback() {
            @Override
            public void success() {
              RelayOk relayOk = finishSuccessfulRestart(true, callback, guard.getRelay());
              guard.discharge(relayOk);
            }

            @Override
            public void failure(String errorMessage) {
              if (callback != null) {
                callback.failure(new Exception(errorMessage));
              }
            }
          };
          return currentContext.continueVm(StepAction.IN, 1,
              continueCallback, guard.asSyncCallback());
        } else {
          resetFrames(data.callFrames());
          return finishSuccessfulRestart(false, callback, relay);
        }
      }

      private RelayOk finishSuccessfulRestart(boolean vmResumed,
          GenericCallback<Boolean> callback, RelaySyncCallback relay) {
        if (callback != null) {
          callback.success(vmResumed);
        }
        return relay.finish();
      }
    }

    private class ExceptionDataImpl implements ExceptionData {
      private final JsValue exceptionValue;

      ExceptionDataImpl(JsValue exceptionValue) {
        this.exceptionValue = exceptionValue;
      }

      @Override
      public JsValue getExceptionValue() {
        return exceptionValue;
      }

      @Override
      public boolean isUncaught() {
        // TODO: implement.
        return false;
      }

      @Override
      public String getSourceText() {
        // Not supported.
        return null;
      }

      @Override
      public String getExceptionMessage() {
        // TODO: implement.
        return exceptionValue.getValueString();
      }
    }

    @Override
    public JavascriptVm getJavascriptVm() {
      return tabImpl;
    }

    private final WipValueLoader valueLoader = new WipValueLoader(tabImpl) {
      @Override
      String getObjectGroupId() {
        return null;
      }
    };
  }

  /**
   * Implements restart frame operation as chain of VM calls. After the main 'restart' command
   * it either calls 'step in' request or reloads backtrace. {@link RelaySyncCallback} is used
   * to guarantee final sync callback invocation.
   */
  public static final RestartFrameExtension RESTART_FRAME_EXTENSION = new RestartFrameExtension() {
    @Override
    public RelayOk restartFrame(CallFrame callFrame,
        GenericCallback<Boolean> callback, SyncCallback syncCallback) {
      WipDebugContextImpl.CallFrameImpl callFrameImpl =
          (WipDebugContextImpl.CallFrameImpl) callFrame;
      return callFrameImpl.restart(callback, syncCallback);
    }

    @Override
    public boolean canRestartFrame(CallFrame callFrame) {
      return callFrame.getScript() != null;
    }
  };


  static JsScope createScope(ScopeValue scopeData, WipValueLoader valueLoader) {
    JsScope.Type type = WIP_TO_SDK_SCOPE_TYPE.get(scopeData.type());
    if (type == null) {
      type = JsScope.Type.UNKNOWN;
    }
    if (type == JsScope.Type.WITH) {
      return new WithScopeImpl(scopeData, valueLoader);
    } else {
      return new ScopeImpl(scopeData, type, valueLoader);
    }
  }

  private static class ScopeImpl implements JsScope {
    private final AsyncFutureRef<Getter<ScopeVariables>> propertiesRef =
        new AsyncFutureRef<Getter<ScopeVariables>>();
    private final String objectId;
    private final Type type;
    private final WipValueLoader valueLoader;

    public ScopeImpl(ScopeValue scopeData, Type type, WipValueLoader valueLoader) {
      this.type = type;
      this.objectId = scopeData.object().objectId();
      this.valueLoader = valueLoader;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public WithScope asWithScope() {
      return null;
    }

    @Override
    public List<? extends JsVariable> getVariables() throws MethodIsBlockingException {
      int currentCacheState = valueLoader.getCacheState();
      if (propertiesRef.isInitialized()) {
        ScopeVariables result = propertiesRef.getSync().get();
        if (result.cacheState == currentCacheState) {
          return result.variables;
        }
        startLoadOperation(true, currentCacheState);
      } else {
        startLoadOperation(false, currentCacheState);
      }

      // This is blocking.
      return propertiesRef.getSync().get().variables;
    }

    /**
     * Starts load operation that works synchronously, i.e. it may block the calling method.
     * This is done because some thread must take post-processing anyway and it shouldn't
     * be the Dispatch thread.
     * The method may not be blocking, if another thread is already doing the same operation.
     */
    private void startLoadOperation(boolean reload, int currentCacheState)
        throws MethodIsBlockingException {
      WipValueLoader.LoadPostprocessor<Getter<ScopeVariables>> processor =
          new WipValueLoader.LoadPostprocessor<Getter<ScopeVariables>>() {
        @Override
        public Getter<ScopeVariables> process(List<? extends PropertyDescriptorValue> propertyList,
            List<? extends InternalPropertyDescriptorValue> internalPropertyList,
            int currentCacheState) {
          final List<JsVariable> properties = new ArrayList<JsVariable>(propertyList.size());

          WipValueBuilder valueBuilder = valueLoader.getValueBuilder();
          for (PropertyDescriptorValue property : propertyList) {
            final String name = property.name();

            ValueNameBuilder valueNameBuilder =
                WipExpressionBuilder.createRootName(name, false);

            JsVariable variable;
            if (objectId == null) {
              variable = valueBuilder.createVariable(property.value(), valueNameBuilder);
            } else {
              variable = valueBuilder.createObjectProperty(property, objectId, valueNameBuilder);
            }
            properties.add(variable);
          }
          final ScopeVariables scopeVariables = new ScopeVariables(properties, currentCacheState);
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
      valueLoader.loadPropertiesInFuture(objectId, processor, reload, currentCacheState,
          propertiesRef);
    }
  }

  private static class WithScopeImpl implements JsScope.WithScope {
    private final JsValue jsValue;

    WithScopeImpl(ScopeValue scopeData, WipValueLoader valueLoader) {
      jsValue = valueLoader.getValueBuilder().wrap(scopeData.object(), null);
    }

    @Override
    public Type getType() {
      return Type.WITH;
    }

    @Override
    public WithScope asWithScope() {
      return this;
    }

    @Override
    public List<? extends JsVariable> getVariables() throws MethodIsBlockingException {
      JsObject asObject = jsValue.asObject();
      if (asObject == null) {
        return Collections.emptyList();
      }
      return new ArrayList<JsVariable>(asObject.getProperties());
    }

    @Override
    public JsValue getWithArgument() {
      return jsValue;
    }
  }

  static JsVariable wrapExceptionValue(RemoteObjectValue valueData,
      WipValueBuilder valueBuilder) {
    JsValue exceptionValue = valueBuilder.wrap(valueData, null);

    final JsVariable property =
        WipValueBuilder.createVariable(exceptionValue, EVALUATE_EXCEPTION_INNER_NAME);

    JsObject wrapperValue = new JsObject() {
      @Override
      public RelayOk reloadHeavyValue(ReloadBiggerCallback callback,
          SyncCallback syncCallback) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isTruncated() {
        return false;
      }

      @Override
      public String getValueString() {
        return "<abnormal return>";
      }

      @Override
      public Type getType() {
        return Type.TYPE_OBJECT;
      }

      @Override
      public JsObject asObject() {
        return this;
      }

      @Override
      public String getRefId() {
        return null;
      }

      @Override
      public RemoteValueMapping getRemoteValueMapping() {
        return null;
      }

      @Override
      public JsVariable getProperty(String name) {
        if (name.equals(property.getName())) {
          return property;
        }
        return null;
      }

      @Override
      public Collection<? extends JsVariable> getProperties()
          throws MethodIsBlockingException {
        return Collections.singletonList(property);
      }

      @Override
      public Collection<? extends JsVariable> getInternalProperties()
          throws MethodIsBlockingException {
        return Collections.emptyList();
      }

      @Override
      public String getClassName() {
        return null;
      }

      @Override
      public JsFunction asFunction() {
        return null;
      }

      @Override
      public JsArray asArray() {
        return null;
      }
    };

    return WipValueBuilder.createVariable(wrapperValue, EVALUATE_EXCEPTION_NAME);
  }

  static final class GlobalEvaluateContext extends WipEvaluateContextBase<EvaluateData> {

    GlobalEvaluateContext(WipValueLoader valueLoader) {
      super(valueLoader);
    }

    @Override protected WipParamsWithResponse<EvaluateData> createRequestParams(String expression,
        WipValueLoader destinationValueLoader) {
      boolean doNotPauseOnExceptions = true;
      return new EvaluateParams(expression, destinationValueLoader.getObjectGroupId(),
          false, doNotPauseOnExceptions, null, false);
    }

    @Override protected RemoteObjectValue getRemoteObjectValue(EvaluateData data) {
      return data.result();
    }

    @Override protected Boolean getWasThrown(EvaluateData data) {
      return data.wasThrown();
    }
  }

  private static final Map<ScopeValue.Type, JsScope.Type> WIP_TO_SDK_SCOPE_TYPE;
  static {
    WIP_TO_SDK_SCOPE_TYPE = new HashMap<ScopeValue.Type, JsScope.Type>();

    WIP_TO_SDK_SCOPE_TYPE.put(ScopeValue.Type.GLOBAL, JsScope.Type.GLOBAL);
    WIP_TO_SDK_SCOPE_TYPE.put(ScopeValue.Type.LOCAL, JsScope.Type.LOCAL);
    WIP_TO_SDK_SCOPE_TYPE.put(ScopeValue.Type.WITH, JsScope.Type.WITH);
    WIP_TO_SDK_SCOPE_TYPE.put(ScopeValue.Type.CLOSURE, JsScope.Type.CLOSURE);
    WIP_TO_SDK_SCOPE_TYPE.put(ScopeValue.Type.CATCH, JsScope.Type.CATCH);

    assert WIP_TO_SDK_SCOPE_TYPE.size() == ScopeValue.Type.values().length;
  }

  private static final ValueNameBuilder EXCEPTION_NAME =
      WipExpressionBuilder.createRootNameNoDerived("exception");

  private static final ValueNameBuilder WITH_OBJECT_NAME =
      WipExpressionBuilder.createRootNameNoDerived("<with object>");

  private static final ValueNameBuilder EVALUATE_EXCEPTION_INNER_NAME =
      WipExpressionBuilder.createRootNameNoDerived("<exception>");

  private static final ValueNameBuilder EVALUATE_EXCEPTION_NAME =
      WipExpressionBuilder.createRootNameNoDerived("<thrown exception>");

  private static class ScopeVariables {
    final List<JsVariable> variables;
    final int cacheState;

    ScopeVariables(List<JsVariable> variables, int cacheState) {
      this.variables = variables;
      this.cacheState = cacheState;
    }
  }

  private static final Getter<ScopeVariables> EMPTY_SCOPE_VARIABLES_OPTIONAL =
      new Getter<ScopeVariables>() {
        private final ScopeVariables value =
          new ScopeVariables(Collections.<JsVariable>emptyList(), Integer.MAX_VALUE);

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