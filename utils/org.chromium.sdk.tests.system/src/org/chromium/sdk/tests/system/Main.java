// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.Factory;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsEvaluateContext.ResultOrException;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsScope.Declarative;
import org.chromium.sdk.JsScope.ObjectBased;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.WipBackendFactory;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;

/**
 * A small automatic test that connects to Chromium browser using ChromeDevTools SDK and try some
 * actions like setting a breakpoint or reading a value of a local variables.
 */
public class Main {

  private static final String TAB_URL_SUFFIX = "/main.html";
  private static final String SCRIPT_ONE_NAME = "/script1.js";
  private static final String BREAKPOINT_MARK = "#breakpoint#1#";
  private static final String FIBONACCI_EXPRESSION = "Fibonacci(4)";

  public static void main(String[] args) throws SmokeException {
    CommandLineArgs commandLineArgs = readArguments(args);

    InetSocketAddress address =
        new InetSocketAddress(commandLineArgs.getHost(), commandLineArgs.getPort());

    StateManager stateManager = new StateManager();

    stateManager.setDefaultReceiver(EXPECT_NOTHING_VISITOR);

    ConnectionLogger.Factory connectionLoggerFactory = new ConnectionLogger.Factory() {
      public ConnectionLogger newConnectionLogger() {
        return new SystemOutConnectionLogger();
      }
    };
    JavascriptVm vm;
    try {
      vm = commandLineArgs.getProtocolType().connect(address, stateManager,
          connectionLoggerFactory);
    } catch (IOException e) {
      throw new SmokeException("Failed to connect", e);
    }

    Collection<Script> scripts = loadScripts(vm);

    // Finding script1.js script.
    Script scriptOne;
    lookForScript: {
      for (Script script : scripts) {
        String name = script.getName();
        if (name != null && name.endsWith(SCRIPT_ONE_NAME)) {
          scriptOne = script;
          break lookForScript;
        }
      }
      throw new SmokeException("Failed to find script " + SCRIPT_ONE_NAME);
    }

    // Getting a number of the line with the marker.
    int breakLine = findSourceMark(scriptOne, BREAKPOINT_MARK);
    if (breakLine == -1) {
      throw new SmokeException("Failed to find mark in script");
    }

    // Setting a breakpoint.
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    Breakpoint.Target breakpointTarget = new Breakpoint.Target.ScriptName(scriptOne.getName());
    RelayOk relayOk = vm.setBreakpoint(breakpointTarget, breakLine, 0, true, null,
        null, callbackSemaphore);
    callbackSemaphore.acquireDefault(relayOk);

    // First time just suspend on breakpoint and go on.
    {
      DebugContext context = stateManager.expectEvent(EXPECT_SUSPENDED_VISITOR);
      context.continueVm(DebugContext.StepAction.CONTINUE, 0, null);
      stateManager.expectEvent(EXPECT_RESUMED_VISITOR);
    }

    // Second time check variables and expressions.
    {
      DebugContext context = stateManager.expectEvent(EXPECT_SUSPENDED_VISITOR);

      {
        // Check cache dropping.
        JsObject root = evaluateSync(context.getGlobalEvaluateContext(),
            "(debug_value_1 = {a:2})").asObject();
        if (root == null) {
          throw new RuntimeException();
        }
        String aValue;
        aValue = root.getProperty("a").getValue().getValueString();
        if (!"2".equals(aValue)) {
          throw new SmokeException();
        }
        evaluateSync(context.getGlobalEvaluateContext(), "debug_value_1.a = 3");

        root.getRemoteValueMapping().clearCaches();

        aValue = root.getProperty("a").getValue().getValueString();
        if (!"3".equals(aValue)) {
          throw new SmokeException();
        }
      }

      {
        // Check literals.
        for (LiteralTestCase literal : TEST_LITERALS) {
          JsValue resultValue = evaluateSync(context.getGlobalEvaluateContext(),
              literal.javaScriptExpression);
          if (resultValue.getType() != literal.expectedType) {
            throw new SmokeException("Unexpected type of '" + literal.javaScriptExpression +
                "': " + resultValue.getType());
          }
          if (!literal.expectedDescription.equals(resultValue.getValueString())) {
            throw new SmokeException("Unexpected string value of '" +
                literal.javaScriptExpression + "': " + resultValue.getValueString());
          }
        }
      }

      // Do not block dispatcher thread.
      stateManager.setDefaultReceiver(IGNORE_SCRIPTS_VISITOR);

      List<? extends CallFrame> callFrames = context.getCallFrames();
      CallFrame topFrame = callFrames.get(0);

      JsScope localScope;
      findScope: {
        for (JsScope scope : topFrame.getVariableScopes()) {
          if (scope.getType() == JsScope.Type.LOCAL) {
            localScope = scope;
            break findScope;
          }
        }
        throw new SmokeException("Failed to find local scope");
      }

      JsVariable xVar = getVariable(localScope, "x");
      if (!"1".equals(xVar.getValue().getValueString())) {
        throw new SmokeException("Unexpected value of local variable");
      }
      JsVariable yVar = getVariable(localScope, "y");
      if (!"2".equals(yVar.getValue().getValueString())) {
        throw new SmokeException("Unexpected value of local variable");
      }

      for (CallFrame frame : callFrames) {
        checkExpression(frame);
      }

      context.continueVm(DebugContext.StepAction.CONTINUE, 0, null);
      stateManager.expectEvent(EXPECT_RESUMED_VISITOR);
    }

    stateManager.setDefaultReceiver(IGNORE_ALL_VISITOR);
    vm.detach();

    System.out.println("Test passed OK");
  }

  private interface CommandLineArgs {
    String getHost();
    int getPort();
    ProtocolType getProtocolType();
  }

  private static CommandLineArgs readArguments(String[] argv) {

    List<String> simpleArgs = new ArrayList<String>(2);
    Map<String, String> keyToValueMap = new LinkedHashMap<String, String>(1);
    int pos = 0;
    while (pos < argv.length) {
      String s = argv[pos];
      if (s.startsWith("--")) {
        s = s.substring(2);
        String key;
        String value;
        int eqPos = s.indexOf('=');
        if (eqPos == -1) {
          key = s;
          value = null;
        } else {
          key = s.substring(0, eqPos);
          value = s.substring(eqPos + 1);
        }
        keyToValueMap.put(key, value);
        pos++;
      } else {
        simpleArgs.add(s);
        pos++;
      }
    }

    if (simpleArgs.size() != 2) {
      throw new IllegalArgumentException("2 arguments expected: debug socket host and port");
    }

    class CommandLineArgsImpl implements CommandLineArgs {
      public String getHost() {
        return host;
      }
      public int getPort() {
        return port;
      }
      public ProtocolType getProtocolType() {
        return protocolType;
      }

      String host;
      int port;
      ProtocolType protocolType = ProtocolType.DEBUGGING;
    }

    CommandLineArgsImpl result = new CommandLineArgsImpl();

    result.host = simpleArgs.get(0);
    String portStr = simpleArgs.get(1);
    result.port = Integer.parseInt(portStr);

    for (Map.Entry<String, String> entry : keyToValueMap.entrySet()) {
      String key = entry.getKey();
      if ("protocol".equals(key)) {
        result.protocolType = ProtocolType.valueOf(entry.getValue());
      } else {
        throw new IllegalArgumentException("Unknown parameter: " + key);
      }
    }

    return result;
  }

  // TODO: drop this enum as we now have only one protocol.
  private enum ProtocolType {
    // WIP (new) protocol enabled by --remote-debugging-port parameter
    DEBUGGING {
      @Override
      public JavascriptVm connect(InetSocketAddress address,
          StateManager stateManager, final Factory connectionLoggerFactory)
          throws SmokeException, IOException {
        WipBrowserFactory.LoggerFactory wipLoggerFactory = new WipBrowserFactory.LoggerFactory() {
          @Override public ConnectionLogger newBrowserConnectionLogger() {
            return connectionLoggerFactory.newConnectionLogger();
          }

          @Override public ConnectionLogger newTabConnectionLogger() {
            return connectionLoggerFactory.newConnectionLogger();
          }
        };
        WipBrowser browser = WipBrowserFactory.INSTANCE.createBrowser(address, wipLoggerFactory);

        WipBackend wipBackend = new WipBackendFactory().create();
        List<? extends WipBrowser.WipTabConnector> tabs;
        try {
          tabs = browser.getTabs(wipBackend);
        } catch (IOException e) {
          throw new SmokeException(e);
        }
        if (tabs.isEmpty()) {
          throw new SmokeException("No tabs");
        }
        WipBrowser.WipTabConnector firstTab = tabs.get(0);
        String url = firstTab.getUrl();
        if (url == null || !url.endsWith(TAB_URL_SUFFIX)) {
          throw new SmokeException("Unexpected URL: " + url);
        }
        return firstTab.attach(stateManager.getTabListener()).getJavascriptVm();
      }
    };

    public abstract JavascriptVm connect(InetSocketAddress address, StateManager stateManager,
        Factory connectionLoggerFactory) throws SmokeException, IOException;
  }

  private static JsVariable getVariable(JsScope scope, String name) throws SmokeException {
    Collection<? extends JsVariable> variables = scope.accept(
        new JsScope.Visitor<Collection<? extends JsVariable>>() {
          @Override
          public Collection<? extends JsVariable> visitDeclarative(Declarative declarativeScope) {
            return declarativeScope.getVariables();
          }
          @Override
          public Collection<? extends JsVariable> visitObject(ObjectBased objectScope) {
            return objectScope.getScopeObject().getProperties();
          }
        });
    for (JsVariable var : variables) {
      if (name.equals(var.getName())) {
        return var;
      }
    }
    throw new SmokeException("Failed to find variable " + name);
  }

  /**
   * Calls fibonacci expression in context for stack frame and checks the result value.
   */
  private static void checkExpression(CallFrame frame) throws SmokeException {
    final ValueHolder<JsValue> variableHolder = new ValueHolder<JsValue>();
    JsEvaluateContext.EvaluateCallback callback = new JsEvaluateContext.EvaluateCallback() {
      @Override
      public void success(ResultOrException result) {
        result.accept(new ResultOrException.Visitor<Void>() {
          @Override
          public Void visitResult(JsValue value) {
            variableHolder.setValue(value);
            return null;
          }

          @Override
          public Void visitException(JsValue exception) {
            variableHolder.setException(
                new Exception("Caught exception: " + exception.getValueString()));
            return null;
          }
        });
      }

      @Override public void failure(Exception cause) {
        variableHolder.setException(new Exception(cause));
      }
    };
    frame.getEvaluateContext().evaluateSync(FIBONACCI_EXPRESSION, null, callback);
    JsValue value = variableHolder.get();
    String resString = value.getValueString();
    if (!"24".equals(resString)) {
      throw new SmokeException("Wrong expression value");
    }
  }

  private static class LiteralTestCase {
    final String javaScriptExpression;
    final JsValue.Type expectedType;
    final String expectedDescription;

    LiteralTestCase(String javaScriptExpression, JsValue.Type expectedType,
        String expectedDescription) {
      this.javaScriptExpression = javaScriptExpression;
      this.expectedType = expectedType;
      this.expectedDescription = expectedDescription;
    }
  }

  private static final List<LiteralTestCase> TEST_LITERALS = Arrays.asList(
      new LiteralTestCase("2011", JsValue.Type.TYPE_NUMBER, "2011"),
      new LiteralTestCase("0", JsValue.Type.TYPE_NUMBER, "0"),
      new LiteralTestCase("-5", JsValue.Type.TYPE_NUMBER, "-5"),
      new LiteralTestCase("123.4567", JsValue.Type.TYPE_NUMBER, "123.4567"),
      new LiteralTestCase("NaN", JsValue.Type.TYPE_NUMBER, "NaN"),
      new LiteralTestCase("Infinity", JsValue.Type.TYPE_NUMBER, "Infinity"),
      new LiteralTestCase("-Infinity", JsValue.Type.TYPE_NUMBER, "-Infinity"),
      new LiteralTestCase("null", JsValue.Type.TYPE_NULL, "null"),
      new LiteralTestCase("(void 0)", JsValue.Type.TYPE_UNDEFINED, "undefined"),
      new LiteralTestCase("true", JsValue.Type.TYPE_BOOLEAN, "true"),
      new LiteralTestCase("false", JsValue.Type.TYPE_BOOLEAN, "false"),
      new LiteralTestCase("'abc'", JsValue.Type.TYPE_STRING, "abc"),
      new LiteralTestCase("'\"'", JsValue.Type.TYPE_STRING, "\""),
      new LiteralTestCase("'\u0424'", JsValue.Type.TYPE_STRING, "\u0424"),
      new LiteralTestCase("'\0'", JsValue.Type.TYPE_STRING, "\0"),
      new LiteralTestCase("\"rrr\"", JsValue.Type.TYPE_STRING, "rrr"));

  /**
   * @return 1-base number of the source line containing marker or -1
   */
  private static int findSourceMark(Script script, String mark) {
    String source = script.getSource();
    int pos = 0;
    int line = 1;
    String rest;
    while (true) {
      int lineEnd = source.indexOf('\n', pos);
      if (lineEnd == -1) {
        rest = source.substring(lineEnd + 1);
        line++;
        break;
      }
      if (source.substring(pos, lineEnd).contains(mark)) {
        return line;
      }
      line++;
      pos = lineEnd + 1;
    }
    if (rest.contains(mark)) {
      return line;
    }
    return -1;
  }

  private static Collection<Script> loadScripts(JavascriptVm javascriptVm) throws SmokeException {
    final ValueHolder<Collection<Script>> result = new ValueHolder<Collection<Script>>();

    JavascriptVm.ScriptsCallback scriptsCallback = new JavascriptVm.ScriptsCallback() {
      public void failure(final String errorMessage) {
        result.setException(new Exception("Failed to read scripts: " + errorMessage));
      }

      public void success(final Collection<Script> scripts) {
        result.setValue(scripts);
      }
    };
    javascriptVm.getScripts(scriptsCallback);
    return result.get();
  }

  /**
   * Connection logger that simply prints all traffic out to System.out.
   * */
  private static class SystemOutConnectionLogger implements ConnectionLogger {
    public void handleEos() {
      System.out.println("EOS");
    }
    public void setConnectionCloser(ConnectionCloser connectionCloser) {
    }
    public void start() {
    }

    @Override
    public StreamListener getIncomingStreamListener() {
      return new StreamListenerImpl();
    }
    @Override
    public StreamListener getOutgoingStreamListener() {
      return new StreamListenerImpl();
    }

    private class StreamListenerImpl implements StreamListener {
      @Override
      public void addSeparator() {
        System.out.print("\n------------------\n");
      }

      @Override
      public void addContent(CharSequence sequence) {
        System.out.print(sequence);
      }
    }
  }

  private static class EventVisitorBase<RES> implements EventVisitor<RES> {
    public RES visitClosed() throws SmokeException {
      return handleDefault();
    }
    public RES visitDisconnected() throws SmokeException {
      return handleDefault();
    }
    public RES visitNavigated(String newUrl) throws SmokeException {
      return handleDefault();
    }
    public RES visitScriptLoaded(Script newScript) throws SmokeException {
      return handleDefault();
    }
    public RES visitScriptCollected(Script script) throws SmokeException {
      return handleDefault();
    }
    public RES visitSuspended(DebugContext context) throws SmokeException {
      return handleDefault();
    }
    public RES visitResumed() throws SmokeException {
      return handleDefault();
    }
    protected RES handleDefault() throws SmokeException {
      throw new SmokeException("Unexpected event");
    }
  }

  private static final EventVisitorBase<Void> EXPECT_NOTHING_VISITOR = new EventVisitorBase<Void>();

  /**
   * Do not pay attention to script afterCompile event.
   */
  private static class IgnoreScriptsVisitor<RES> extends EventVisitorBase<RES> {
    @Override
    public RES visitScriptLoaded(Script newScript) {
      return null;
    }
    @Override
    public RES visitScriptCollected(Script script) {
      return null;
    }
  }

  private static final IgnoreScriptsVisitor<Void> IGNORE_SCRIPTS_VISITOR =
      new IgnoreScriptsVisitor<Void>();

  private static final EventVisitor<DebugContext> EXPECT_SUSPENDED_VISITOR =
      new IgnoreScriptsVisitor<DebugContext>() {
        @Override
        public DebugContext visitSuspended(DebugContext context) {
          return context;
        }
     };

 private static final EventVisitor<Boolean> EXPECT_RESUMED_VISITOR =
     new IgnoreScriptsVisitor<Boolean>() {
       @Override
       public Boolean visitResumed() {
         return Boolean.TRUE;
       }
    };

  private static final EventVisitor<Void> IGNORE_ALL_VISITOR = new EventVisitorBase<Void>() {
    @Override
    protected Void handleDefault() {
      return null;
    }
  };

  /**
   * A utility class to pass a result from callback of async operation to a function that
   * is blocked waiting for this operation. Operation may complete with normal result value or
   * with exception.
   */
  private static class ValueHolder<T> {
    private T val = null;
    private Exception exception = null;

    void setValue(T val) {
      this.val = val;
    }
    void setException(Exception exception) {
      this.exception = exception;
    }
    T get() throws SmokeException {
      if (exception != null) {
        throw new SmokeException(exception);
      }
      return val;
    }
  }

  static class EvalCallbackImpl implements JsEvaluateContext.EvaluateCallback {
    JsValue value = null;
    Exception failure = null;

    JsValue get() {
      if (failure != null) {
        throw new RuntimeException("Failed to evaluate: " + failure);
      }
      return value;
    }

    @Override
    public void success(ResultOrException result) {
      result.accept(new ResultOrException.Visitor<Void>() {
        @Override
        public Void visitResult(JsValue value) {
          EvalCallbackImpl.this.value = value;
          return null;
        }

        @Override
        public Void visitException(JsValue exception) {
          failure = new Exception("JavaScript exception: " + exception.getValueString());
          return null;
        }
      });
    }

    @Override
    public void failure(Exception cause) {
      this.failure = cause;
    }
  }

  private static JsValue evaluateSync(JsEvaluateContext evaluateContext, String expression) {
    EvalCallbackImpl callbackImpl = new EvalCallbackImpl();
    evaluateContext.evaluateSync(expression, null, callbackImpl);
    return callbackImpl.get();
  }
}
