package org.chromium.sdk.internal.v8native;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.internal.v8native.processor.AfterCompileProcessor;
import org.chromium.sdk.internal.v8native.processor.BreakpointProcessor;
import org.chromium.sdk.internal.v8native.processor.ScriptCollectedProcessor;
import org.chromium.sdk.internal.v8native.processor.V8EventProcessor;
import org.chromium.sdk.internal.v8native.protocol.input.EventNotification;
import org.chromium.sdk.internal.v8native.protocol.input.IncomingMessage;

public class DefaultResponseHandler {

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(DefaultResponseHandler.class.getName());

  /** The breakpoint processor. */
  private final BreakpointProcessor bpp;

  /** The "afterCompile" event processor. */
  private final AfterCompileProcessor afterCompileProcessor;

  /** The "scriptCollected" event processor. */
  private final ScriptCollectedProcessor scriptCollectedProcessor;

  public DefaultResponseHandler(DebugSession debugSession) {
    this.bpp = new BreakpointProcessor(debugSession);
    this.afterCompileProcessor = new AfterCompileProcessor(debugSession);
    this.scriptCollectedProcessor = new ScriptCollectedProcessor(debugSession);
  }

  public BreakpointProcessor getBreakpointProcessor() {
    return bpp;
  }

  /**
   * @param type response type ("response" or "event")
   * @param response from the V8 VM debugger
   */
  public void handleResponseWithHandler(IncomingMessage response) {
    EventNotification eventResponse = response.asEventNotification();
    if (eventResponse == null) {
      // Currently only events are supported.
      return;
    }
    String commandString = eventResponse.event();
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
    handlerGetter.get(this).messageReceived(eventResponse);
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
    command2EventProcessorGetter.put(DebuggerCommand.SCRIPT_COLLECTED /* event */,
        new ProcessorGetter() {
      @Override
      ScriptCollectedProcessor get(DefaultResponseHandler instance) {
        return instance.scriptCollectedProcessor;
      }
    });
  }
}
