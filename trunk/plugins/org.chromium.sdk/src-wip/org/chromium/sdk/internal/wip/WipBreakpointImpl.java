// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.JavascriptVm.GenericCallback;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData;
import org.chromium.sdk.internal.wip.protocol.output.debugger.RemoveBreakpointParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetBreakpointByUrlParams;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructableWrapper;
import org.chromium.sdk.util.DestructingGuard;

/**
 * Wip-based breakpoint implementation.
 */
public class WipBreakpointImpl implements Breakpoint {
  private final WipTabImpl tabImpl;

  private final ScriptUrlOrId script;

  private final int lineNumber;
  private final int columnNumber;

  private final int sdkId;
  private volatile String protocolId;

  private volatile String condition;
  private volatile boolean enabled;
  private volatile boolean isDirty;

  public WipBreakpointImpl(WipTabImpl tabImpl, int sdkId, String protocolId,
      ScriptUrlOrId script, int lineNumber, int columnNumber,
      String condition, boolean enabled) {
    this.tabImpl = tabImpl;
    this.sdkId = sdkId;
    this.protocolId = protocolId;
    this.script = script;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.condition = condition;
    this.enabled = enabled;

    this.isDirty = false;
  }

  @Override
  public Type getType() {
    return script.accept(SCRIPT_TYPE_VISITOR);
  }

  private static final ScriptUrlOrId.Visitor<Type> SCRIPT_TYPE_VISITOR =
      new ScriptUrlOrId.Visitor<Type>() {
    @Override public Type forUrl(String url) {
      return Type.SCRIPT_NAME;
    }
    @Override public Type forId(long sourceId) {
      return Type.SCRIPT_ID;
    }
  };

  @Override
  public long getId() {
    return sdkId;
  }

  @Override
  public String getScriptName() {
    return script.accept(SCRIPT_URL_VISITOR);
  }

  private static final ScriptUrlOrId.Visitor<String> SCRIPT_URL_VISITOR =
      new ScriptUrlOrId.Visitor<String>() {
    @Override public String forUrl(String url) {
      return url;
    }
    @Override public String forId(long sourceId) {
      return null;
    }
  };

  @Override
  public Long getScriptId() {
    return script.accept(SCRIPT_ID_VISITOR);
  }

  private static final ScriptUrlOrId.Visitor<Long> SCRIPT_ID_VISITOR =
      new ScriptUrlOrId.Visitor<Long>() {
    @Override public Long forUrl(String url) {
      return null;
    }
    @Override public Long forId(long sourceId) {
      return sourceId;
    }
  };

  @Override
  public long getLineNumber() {
    return lineNumber;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    isDirty  = true;
  }

  @Override
  public int getIgnoreCount() {
    // TODO: support.
    return 0;
  }

  @Override
  public void setIgnoreCount(int ignoreCount) {
    // TODO: support.
  }

  @Override
  public String getCondition() {
    return condition;
  }

  @Override
  public void setCondition(String condition) {
    this.condition = condition;
    isDirty = true;
  }

  @Override
  public void clear(final BreakpointCallback callback, SyncCallback syncCallback) {
    RemoveBreakpointParams params = new RemoveBreakpointParams(protocolId);

    WipCommandCallback commandCallback;
    if (callback == null) {
      commandCallback = null;
    } else {
      commandCallback = new WipCommandCallback.Default() {
        @Override protected void onSuccess(Success success) {
          callback.success(WipBreakpointImpl.this);
        }
        @Override protected void onError(String message) {
          callback.failure(message);
        }
      };
    }

    tabImpl.getCommandProcessor().send(params, commandCallback, syncCallback);
  }

  @Override
  public void flush(final BreakpointCallback callback, final SyncCallback syncCallback) {
    {
      // Is it thread-safe-enough for us?
      if (!isDirty) {
        return;
      }
    }
    isDirty = false;

    final Destructable callbackAsDestructable =
        DestructableWrapper.callbackAsDestructable(syncCallback);

    // Call syncCallback if something goes wrong after we sent request.
    final DestructingGuard guard = new DestructingGuard();

    WipCommandCallback removeCallback = new WipCommandCallback.Default() {
      @Override
      protected void onSuccess(Success success) {
        recreateBreakpointAsync(callback, callbackAsDestructable);
        guard.discharge();
      }

      @Override
      protected void onError(String message) {
        throw new RuntimeException("Failed to remove breakpoint: " + message);
      }
    };

    // Call syncCallback if something goes wrong.
    guard.addValue(callbackAsDestructable);
    tabImpl.getCommandProcessor().send(new RemoveBreakpointParams(protocolId), removeCallback,
        DestructableWrapper.guardAsCallback(guard));
  }

  private void recreateBreakpointAsync(final BreakpointCallback flushCallback,
      Destructable callbackAsDestructable) {
    GenericCallback<String> setCommandCallback = new GenericCallback<String>() {
      @Override
      public void success(String protocolId) {
        WipBreakpointImpl.this.protocolId = protocolId;
        if (flushCallback != null) {
          flushCallback.success(WipBreakpointImpl.this);
        }
      }

      @Override
      public void failure(Exception exception) {
        if (flushCallback != null) {
          flushCallback.failure(exception.getMessage());
        }
      }
    };

    DestructingGuard guard = new DestructingGuard();
    guard.addValue(callbackAsDestructable);

    if (condition == null) {
      condition = "";
    }
    // TODO: support enabled.
    sendSetBreakpointRequest(script, lineNumber, columnNumber, condition,
        setCommandCallback, DestructableWrapper.guardAsCallback(guard),
        tabImpl.getCommandProcessor());
  }

  /**
   * @param callback a generic callback that receives breakpoint protocol id
   */
  static void sendSetBreakpointRequest(ScriptUrlOrId scriptRef, final int lineNumber,
      final int columnNumber, final String condition,
      final GenericCallback<String> callback, final SyncCallback syncCallback,
      final WipCommandProcessor commandProcessor) {
    scriptRef.accept(new ScriptUrlOrId.Visitor<Void>() {
      @Override
      public Void forId(long sourceId) {
        // TODO: implement.
        return WipBrowserImpl.throwUnsupported();
      }

      @Override
      public Void forUrl(String url) {
        // TODO: support enabled.
        SetBreakpointByUrlParams request =
            new SetBreakpointByUrlParams(url, lineNumber, (long) columnNumber, condition);

        JavascriptVm.GenericCallback<SetBreakpointByUrlData> wrappedCallback;
        if (callback == null) {
          wrappedCallback = null;
        } else {
          wrappedCallback = new JavascriptVm.GenericCallback<SetBreakpointByUrlData>() {
            @Override
            public void success(SetBreakpointByUrlData data) {
              callback.success(data.breakpointId());
            }

            @Override
            public void failure(Exception exception) {
              callback.failure(exception);
            }
          };
        }

        commandProcessor.send(request, wrappedCallback, syncCallback);
        return null;
      }
    });
  }
}
