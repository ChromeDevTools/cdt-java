// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse.Success;
import org.chromium.sdk.internal.rynda.protocol.input.SetBreakpointData;
import org.chromium.sdk.internal.rynda.protocol.output.RemoveBreakpointRequest;
import org.chromium.sdk.internal.rynda.protocol.output.SetJavaScriptBreakpoint;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructableWrapper;
import org.chromium.sdk.util.DestructingGuard;

/**
 * Rynda-based breakpoint implementation.
 */
public class RyndaBreakpointImpl implements Breakpoint {
  private final RyndaTabImpl ryndaTab;

  private final ScriptUrlOrId script;

  private final int lineNumber;
  private final int columnNumber;

  private final int sdkId;
  private volatile String protocolId;

  private volatile String condition;
  private volatile boolean enabled;
  private volatile boolean isDirty;

  public RyndaBreakpointImpl(RyndaTabImpl ryndaTab, int sdkId, String protocolId,
      ScriptUrlOrId script, int lineNumber, int columnNumber,
      String condition, boolean enabled) {
    this.ryndaTab = ryndaTab;
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
    return script.accept(SCRIPT_NAME_VISITOR);
  }

  private static final ScriptUrlOrId.Visitor<String> SCRIPT_NAME_VISITOR =
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
    RemoveBreakpointRequest request = new RemoveBreakpointRequest(protocolId);

    RyndaCommandCallback commandCallback;
    if (callback == null) {
      commandCallback = null;
    } else {
      commandCallback = new RyndaCommandCallback.Default() {
        @Override protected void onSuccess(Success success) {
          callback.success(RyndaBreakpointImpl.this);
        }
        @Override protected void onError(String message) {
          callback.failure(message);
        }
      };
    }

    ryndaTab.getCommandProcessor().send(request, commandCallback, syncCallback);
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

    RyndaCommandCallback removeCallback = new RyndaCommandCallback.Default() {
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
    ryndaTab.getCommandProcessor().send(new RemoveBreakpointRequest(protocolId), removeCallback,
        DestructableWrapper.guardAsCallback(guard));
  }

  private void recreateBreakpointAsync(final BreakpointCallback flushCallback,
      Destructable callbackAsDestructable) {
    RyndaCommandCallback setCommandCallback = new RyndaCommandCallback.Default() {
      @Override
      protected void onSuccess(Success success) {
        SetBreakpointData data;
        try {
          data = success.data().asSetBreakpointData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        String protocolId = data.breakpointId();
        RyndaBreakpointImpl.this.protocolId = protocolId;
        if (flushCallback != null) {
          flushCallback.success(RyndaBreakpointImpl.this);
        }
      }

      @Override
      protected void onError(String message) {
        if (flushCallback != null) {
          flushCallback.failure(message);
        }
      }
    };

    DestructingGuard guard = new DestructingGuard();
    guard.addValue(callbackAsDestructable);
    SetJavaScriptBreakpoint setRequest =
        new SetJavaScriptBreakpoint(script, lineNumber, columnNumber, condition, enabled);
    ryndaTab.getCommandProcessor().send(setRequest, setCommandCallback,
        DestructableWrapper.guardAsCallback(guard));
  }
}
