// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.JavascriptVm.GenericCallback;
import org.chromium.sdk.SyncCallback;

/**
 * A manager that works as factory for breakpoints.
 * TODO: add a map id -> breakpoint.
 */
public class WipBreakpointManager {
  private final WipTabImpl tabImpl;
  private final AtomicInteger breakpointUniqueId = new AtomicInteger(0);

  private final Set<WipBreakpointImpl> breakpoints = new HashSet<WipBreakpointImpl>();

  WipBreakpointManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  void setBreakpoint(Type type, String target, final int line, final int column,
      final boolean enabled, String condition, int ignoreCount,
      final BreakpointCallback callback, SyncCallback syncCallback) {

    final ScriptUrlOrId script;
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

    int sdkId = breakpointUniqueId.getAndAdd(1);

    final WipBreakpointImpl breakpointImpl = new WipBreakpointImpl(this, sdkId,
        script, line, column, condition, enabled);

    synchronized (breakpoints) {
      breakpoints.add(breakpointImpl);
    }

    if (enabled) {
      if (condition == null) {
        condition = "";
      }

      final String conditionFinal = condition;

      GenericCallback<String> wrappedCallback = new GenericCallback<String>() {
        @Override
        public void success(String protocolId) {
          breakpointImpl.setProtocolId(protocolId);
          if (callback != null) {
            callback.success(breakpointImpl);
          }
        }

        @Override
        public void failure(Exception exception) {
          if (callback != null) {
            callback.failure(exception.getMessage());
          }
        }
      };

      WipBreakpointImpl.sendSetBreakpointRequest(script, line, column, condition,
          wrappedCallback, syncCallback, tabImpl.getCommandProcessor());
    } else {
      callback.success(breakpointImpl);
      syncCallback.callbackDone(null);
    }
  }

  void breakpointDeleted(WipBreakpointImpl breakpoint) {
    synchronized (breakpoints) {
      breakpoints.remove(breakpoint);
    }
  }

  WipCommandProcessor getCommandProcessor() {
    return tabImpl.getCommandProcessor();
  }

  Collection<WipBreakpointImpl> getAllBreakpoints() {
    synchronized (breakpoints) {
      return new ArrayList<WipBreakpointImpl>(breakpoints);
    }
  }
}
