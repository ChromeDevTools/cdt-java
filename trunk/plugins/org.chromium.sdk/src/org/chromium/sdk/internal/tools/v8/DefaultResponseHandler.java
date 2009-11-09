package org.chromium.sdk.internal.tools.v8;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.tools.v8.processor.AfterCompileProcessor;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.tools.v8.processor.V8EventProcessor;
import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
import org.json.simple.JSONObject;

public class DefaultResponseHandler {

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(DefaultResponseHandler.class.getName());

  /** The breakpoint processor. */
  private final BreakpointProcessor bpp;

  /** The "afterCompile" event processor. */
  private final AfterCompileProcessor afterCompileProcessor;

  public DefaultResponseHandler(DebugSession debugSession) {
    this.bpp = new BreakpointProcessor(debugSession);
    this.afterCompileProcessor = new AfterCompileProcessor(debugSession);
  }

  public BreakpointProcessor getBreakpointProcessor() {
    return bpp;
  }

  /**
   * @param type response type ("response" or "event")
   * @param response from the V8 VM debugger
   */
  public void handleResponseWithHandler(V8MessageType type, final JSONObject response) {
    if (type != V8MessageType.EVENT) {
      // Currently only events are supported.
      return;
    }
    String commandString = JsonUtil.getAsString(response, V8Protocol.KEY_EVENT);
    DebuggerCommand command = DebuggerCommand.forString(commandString);
    if (command == null) {
      LOGGER.log(Level.WARNING,
          "Unknown command in V8 debugger reply JSON: {0}", commandString);
      return;
    }
    final ProcessorGetter handlerGetter = command2EventProcessorGetter.get(command);
    if (handlerGetter == null) {
      return;
    }
    handlerGetter.get(this).messageReceived(response);
  }

  private static abstract class ProcessorGetter {
    abstract V8EventProcessor get(DefaultResponseHandler instance);
  }

  /**
   * The handlers that should be invoked when certain command responses arrive.
   */
  private static final Map<DebuggerCommand, ProcessorGetter> command2EventProcessorGetter;
  static {
    command2EventProcessorGetter = new HashMap<DebuggerCommand, ProcessorGetter>();
    ProcessorGetter bppGetter = new ProcessorGetter() {
      @Override
      BreakpointProcessor get(DefaultResponseHandler instance) {
        return instance.bpp;
      }
    };
    command2EventProcessorGetter.put(DebuggerCommand.BREAK /* event */, bppGetter);
    command2EventProcessorGetter.put(DebuggerCommand.EXCEPTION /* event */, bppGetter);

    command2EventProcessorGetter.put(DebuggerCommand.AFTER_COMPILE /* event */,
        new ProcessorGetter() {
      @Override
      AfterCompileProcessor get(DefaultResponseHandler instance) {
        return instance.afterCompileProcessor;
      }
    });
  }
}
