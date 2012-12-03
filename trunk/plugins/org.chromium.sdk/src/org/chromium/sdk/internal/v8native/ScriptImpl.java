package org.chromium.sdk.internal.v8native;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptBase;
import org.chromium.sdk.internal.liveeditprotocol.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.V8Helper.ScriptLoadCallback;
import org.chromium.sdk.internal.v8native.protocol.input.ChangeLiveBody;
import org.chromium.sdk.internal.v8native.protocol.input.FailedCommandResponse.ErrorDetails;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.ScriptHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.protocol.output.ChangeLiveMessage;
import org.chromium.sdk.internal.v8native.value.HandleManager;

public class ScriptImpl extends ScriptBase<Long> {
  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(ScriptImpl.class.getName());

  private final DebugSession debugSession;

  public ScriptImpl(Descriptor<Long> descriptor, DebugSession debugSession) {
    super(descriptor);
    this.debugSession = debugSession;
  }

  @Override
  public RelayOk setSourceOnRemote(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = createScriptUpdateCallback(callback, false);
    return debugSession.sendMessageAsync(new ChangeLiveMessage(getId(), newSource, Boolean.FALSE),
        true, v8Callback, syncCallback);
  }

  @Override
  public RelayOk previewSetSource(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = createScriptUpdateCallback(callback, true);
    return debugSession.sendMessageAsync(new ChangeLiveMessage(getId(), newSource, Boolean.TRUE),
        true, v8Callback, syncCallback);
  }

  private V8CommandProcessor.V8HandlerCallback createScriptUpdateCallback(
      final UpdateCallback callback, final boolean previewOnly) {
    return new V8CommandCallbackBase() {
      @Override
      public void success(SuccessCommandResponse successResponse) {
        ChangeLiveBody body;
        try {
          body = successResponse.body().asChangeLiveBody();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }

        LiveEditResult resultDescription = body.getResultDescription();
        if (!previewOnly) {
          ScriptLoadCallback scriptCallback = new ScriptLoadCallback() {
            @Override
            public void failure(String message) {
              LOGGER.log(Level.SEVERE,
                  "Failed to reload script after LiveEdit script update; " + message);
            }

            @Override
            public void success() {
              DebugEventListener listener = debugSession.getDebugEventListener();
              if (listener != null) {
                listener.scriptContentChanged(ScriptImpl.this);
              }
            }
          };
          V8Helper.reloadScriptAsync(debugSession, Collections.singletonList(getId()),
              scriptCallback, null);

          if (body.stepin_recommended() == Boolean.TRUE) {
            DebugContext debugContext = debugSession.getContextBuilder().getCurrentDebugContext();
            if (debugContext == null) {
              // We may have already issued 'continue' since the moment that change live command
              // was sent so the context was dropped. Ignore this case.
            } else {
              debugContext.continueVm(DebugContext.StepAction.IN, 0, null);
            }
          } else {
            if (resultDescription != null && resultDescription.stack_modified()) {
              debugSession.recreateCurrentContext();
            }
          }
        }

        if (callback != null) {
          callback.success(body.getChangeLog(),
              UpdateResultParser.wrapChangeDescription(resultDescription));
        }
      }

      @Override
      public void failure(String message, ErrorDetails errorDetails) {
        callback.failure(message);
      }
    };
  }

  public static Long getScriptId(HandleManager handleManager, long scriptRef) {
    SomeHandle handle = handleManager.getHandle(scriptRef);
    if (handle == null) {
      return -1L; // not found
    }
    ScriptHandle scriptHandle;
    try {
      scriptHandle = handle.asScriptHandle();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    return scriptHandle.id();
  }
}
