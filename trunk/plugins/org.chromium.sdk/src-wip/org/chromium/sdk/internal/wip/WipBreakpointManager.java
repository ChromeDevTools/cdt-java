// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.protocol.input.SetBreakpointData;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.output.SetJavaScriptBreakpoint;

/**
 * A manager that works as factory for breakpoints.
 * TODO: add a map id -> breakpoint.
 */
public class WipBreakpointManager {
  private final WipTabImpl tabImpl;
  private final AtomicInteger breakpointUniqueId = new AtomicInteger(0);

  WipBreakpointManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  void setBreakpoint(Type type, String target, int line, int column,
      boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback, SyncCallback syncCallback) {

    ScriptUrlOrId script;
    switch (type) {
    case FUNCTION:
      throw new UnsupportedOperationException();
    case SCRIPT_ID:
      script = ScriptUrlOrId.forId(Long.parseLong(target));
      break;
    case SCRIPT_NAME:
      script = ScriptUrlOrId.forUrl(target);
      break;
    default:
      throw new UnsupportedOperationException();
    }

    SetJavaScriptBreakpoint request =
        new SetJavaScriptBreakpoint(script, line, column, condition, enabled);
    WipCommandCallback commandCallback =
        createBreakpointCallback(callback, script, line, column, condition, enabled);
    tabImpl.getCommandProcessor().send(request, commandCallback, syncCallback);
  }

  WipCommandCallback createBreakpointCallback(final BreakpointCallback callback,
      final ScriptUrlOrId script, final int lineNumber, final int columnNumber,
      final String condition, final boolean enabled) {
    return new WipCommandCallback.Default() {
      @Override
      protected void onSuccess(Success success) {
        SetBreakpointData data;
        try {
          data = success.data().asSetBreakpointData();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }
        int sdkId = breakpointUniqueId.getAndAdd(1);
        String protocolId = data.breakpointId();

        WipBreakpointImpl breakpointImpl = new WipBreakpointImpl(tabImpl, sdkId, protocolId,
            script, lineNumber, columnNumber, condition, enabled);
        WipBreakpointImpl breakpoint = breakpointImpl;
        if (callback != null) {
          callback.success(breakpoint);
        }
      }

      @Override
      protected void onError(String message) {
        if (callback != null) {
          callback.failure(message);
        }
      }
    };
  }
}
