// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.ConnectionLogger;
import org.chromium.sdk.ConnectionLogger.Factory;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.UnsupportedVersionException;
import org.chromium.sdk.util.ByteToCharConverter;
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

    BrowserTab tab;
    try {
      tab = connect(address, stateManager, commandLineArgs.getProtocolType());
    } catch (IOException e) {
      throw new SmokeException("Failed to connect", e);
    } catch (UnsupportedVersionException e) {
      throw new SmokeException("Failed to connect", e);
    }

    Collection<Script> scripts = loadScripts(tab);

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
    tab.setBreakpoint(Breakpoint.Type.SCRIPT_NAME, scriptOne.getName(), breakLine, 0, true, null,
        0, null, callbackSemaphore);
    callbackSemaphore.acquireDefault();

    // First time just suspend on breakpoint and go on.
    {
      DebugContext context = stateManager.expectEvent(EXPECT_SUSPENDED_VISITOR);
      context.continueVm(DebugContext.StepAction.CONTINUE, 0, null);
      stateManager.expectEvent(EXPECT_RESUMED_VISITOR);
    }

    // Second time check variables and expressions.
    {
      DebugContext context = stateManager.expectEvent(EXPECT_SUSPENDED_VISITOR);

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
    tab.detach();

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
      ProtocolType protocolType = ProtocolType.SHELL;
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

  private enum ProtocolType {
    // Old protocol enabled by --remote-shell-port parameter
    SHELL {
      @Override
      public Browser connect(InetSocketAddress address,
          Factory connectionLoggerFactory) {
        return BrowserFactory.getInstance().create(address, connectionLoggerFactory);
      }
    },
    // WIP (new) protocol enabled by --remote-debugging-port parameter
    DEBUGGING {
      @Override
      public Browser connect(InetSocketAddress address,
          final Factory connectionLoggerFactory) {
        WipBrowserFactory.LoggerFactory wipLoggerFactory = new WipBrowserFactory.LoggerFactory() {
          @Override public ConnectionLogger newBrowserConnectionLogger() {
            throw new UnsupportedOperationException();
          }

          @Override public ConnectionLogger newTabConnectionLogger() {
            return connectionLoggerFactory.newConnectionLogger();
          }
        };
        return WipBrowserFactory.INSTANCE.createBrowser(address, wipLoggerFactory);
      }
    };

    public abstract Browser connect(InetSocketAddress address, Factory connectionLoggerFactory);
  }

  private static JsVariable getVariable(JsScope scope, String name) throws SmokeException {
    for (JsVariable var : scope.getVariables()) {
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
    final ValueHolder<JsVariable> variableHolder = new ValueHolder<JsVariable>();
    JsEvaluateContext.EvaluateCallback callback = new JsEvaluateContext.EvaluateCallback() {
      public void failure(String errorMessage) {
        variableHolder.setException(new Exception(errorMessage));
      }
      public void success(JsVariable variable) {
        variableHolder.setValue(variable);
      }
    };
    frame.getEvaluateContext().evaluateSync(FIBONACCI_EXPRESSION, callback);
    JsVariable variable = variableHolder.get();
    String resString = variable.getValue().getValueString();
    if (!"24".equals(resString)) {
      throw new SmokeException("Wrong expression value");
    }
  }

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

  private static BrowserTab connect(InetSocketAddress address, StateManager stateManager,
      ProtocolType protocolType) throws SmokeException, IOException, UnsupportedVersionException {
    ConnectionLogger.Factory connectionLoggerFactory = new ConnectionLogger.Factory() {
      public ConnectionLogger newConnectionLogger() {
        return new SystemOutConnectionLogger();
      }
    };

    Browser browser = protocolType.connect(address, connectionLoggerFactory);

    Browser.TabFetcher tabFetcher = browser.createTabFetcher();
    List<? extends Browser.TabConnector> tabs = tabFetcher.getTabs();
    if (tabs.isEmpty()) {
      throw new SmokeException("No tabs");
    }
    Browser.TabConnector firstTab = tabs.get(0);
    String url = firstTab.getUrl();
    if (url == null || !url.endsWith(TAB_URL_SUFFIX)) {
      throw new SmokeException("Unexpected URL: " + url);
    }
    return firstTab.attach(stateManager.getTabListener());
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
    public LoggableReader wrapReader(final LoggableReader streamReader, final Charset charset) {
      final InputStream originalInputStream = streamReader.getInputStream();
      final InputStream wrappedInputStream = new InputStream() {
        private final ByteToCharConverter byteToCharConverter = new ByteToCharConverter(charset);
        @Override
        public int read() throws IOException {
          byte[] buffer = new byte[1];
          int res = readImpl(buffer, 0, 1);
          if (res <= 0) {
            return -1;
          } else {
            return buffer[0];
          }
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          return readImpl(b, off, len);
        }
        private int readImpl(byte[] buf, int off, int len) throws IOException {
          int res = originalInputStream.read(buf, off, len);
          if (res > 0) {
            convertAndPrintBytes(ByteBuffer.wrap(buf, off, res), byteToCharConverter, System.out);
          }
          return res;
        }
      };

      return new LoggableReader() {
        public InputStream getInputStream() {
          return wrappedInputStream;
        }
        public void markSeparatorForLog() {
          System.out.println("\n------------------");
        }
      };
    }
    public LoggableWriter wrapWriter(final LoggableWriter streamWriter, final Charset charset) {
      return new LoggableWriter() {
        private final ByteToCharConverter byteToCharConverter = new ByteToCharConverter(charset);

        public OutputStream getOutputStream() {
          final OutputStream originalOutput = streamWriter.getOutputStream();
          return new OutputStream() {
            @Override
            public void close() throws IOException {
              originalOutput.close();
            }
            @Override
            public void flush() throws IOException {
              originalOutput.flush();
            }
            @Override
            public void write(int b) throws IOException {
              write(new byte[] { (byte) b });
            }
            @Override
            public void write(byte[] buf, int off, int len) throws IOException {
              originalOutput.write(buf, off, len);
              convertAndPrintBytes(ByteBuffer.wrap(buf, off, len), byteToCharConverter, System.out);
            }
          };
        }
        public void markSeparatorForLog() {
          System.out.println("\n------------------");
        }
      };
    }

    private static void convertAndPrintBytes(ByteBuffer in, ByteToCharConverter converter,
        PrintStream out) {
      CharBuffer res = converter.convert(in);
      while (res.hasRemaining()) {
        out.print(res.get());
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
    private final CallbackSemaphore semaphore = new CallbackSemaphore();
    void setValue(T val) {
      this.val = val;
      semaphore.callbackDone(null);
    }
    void setException(Exception exception) {
      this.exception = exception;
      semaphore.callbackDone(null);
    }
    T get() throws SmokeException {
      semaphore.acquireDefault();
      if (exception != null) {
        throw new SmokeException(exception);
      }
      return val;
    }
  }
}
