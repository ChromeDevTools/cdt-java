package org.chromium.sdk.internal.tools.v8;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.processor.AfterCompileProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.tools.v8.processor.ContinueProcessor;
import org.chromium.sdk.internal.tools.v8.processor.V8ResponseCallback;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONObject;

public class DefaultResponseHandler {

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(DefaultResponseHandler.class.getName());

  /** The breakpoint processor. */
  private final BreakpointProcessor bpp;

  /** The "afterCompile" event processor. */
  private final AfterCompileProcessor afterCompileProcessor;

  private final ContinueProcessor continueProcessor;

  public DefaultResponseHandler(DebugSession debugSession) {
    this.bpp = new BreakpointProcessor(debugSession);
    this.afterCompileProcessor = new AfterCompileProcessor(debugSession);
    this.continueProcessor = new ContinueProcessor(debugSession);
  }

  /**
   * @param type response type ("response" or "event")
   * @param response from the V8 VM debugger
   */
  public void handleResponseWithHandler(V8MessageType type, final JSONObject response) {
    String commandString = JsonUtil.getAsString(response, V8MessageType.RESPONSE == type
        ? V8Protocol.KEY_COMMAND
        : V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command == null) {
      LOGGER.log(Level.WARNING,
          "Unknown command in V8 debugger reply JSON: {0}", commandString);
      return;
    }
    final HandlerGetter handlerGetter = command2HandlerGetter.get(command);
    if (handlerGetter == null) {
      return;
    }
    handlerGetter.get(this).messageReceived(response);
  }

  private static abstract class HandlerGetter {
    abstract V8ResponseCallback get(DefaultResponseHandler instance);
  }

  /**
   * The handlers that should be invoked when certain command responses arrive.
   */
  private static final Map<DebuggerCommand, HandlerGetter> command2HandlerGetter;
  static {
    command2HandlerGetter = new HashMap<DebuggerCommand, HandlerGetter>();
    HandlerGetter bppGetter = new HandlerGetter() {
      @Override
      BreakpointProcessor get(DefaultResponseHandler instance) {
        return instance.bpp;
      }
    };
    command2HandlerGetter.put(DebuggerCommand.CHANGEBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.SETBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.CLEARBREAKPOINT, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.BREAK /* event */, bppGetter);
    command2HandlerGetter.put(DebuggerCommand.EXCEPTION /* event */, bppGetter);

    command2HandlerGetter.put(DebuggerCommand.AFTER_COMPILE /* event */,
        new HandlerGetter() {
      @Override
      AfterCompileProcessor get(DefaultResponseHandler instance) {
        return instance.afterCompileProcessor;
      }
    });

    command2HandlerGetter.put(DebuggerCommand.CONTINUE,
        new HandlerGetter() {
      @Override
      ContinueProcessor get(DefaultResponseHandler instance) {
        return instance.continueProcessor;
      }
    });
  }
}
