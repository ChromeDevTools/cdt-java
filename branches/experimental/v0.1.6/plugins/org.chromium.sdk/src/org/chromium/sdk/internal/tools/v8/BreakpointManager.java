// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.JavascriptVm.ListBreakpointsCallback;
import org.chromium.sdk.internal.DebugSession;
import org.chromium.sdk.internal.protocol.BreakpointBody;
import org.chromium.sdk.internal.protocol.CommandResponseBody;
import org.chromium.sdk.internal.protocol.ListBreakpointsBody;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.BreakpointInfo;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.chromium.sdk.internal.tools.v8.request.ListBreakpointsMessage;

public class BreakpointManager {
  /**
   * This map shall contain only breakpoints with valid IDs.
   */
  private final Map<Long, BreakpointImpl> idToBreakpoint = new HashMap<Long, BreakpointImpl>();

  private final DebugSession debugSession;

  public BreakpointManager(DebugSession debugSession) {
    this.debugSession = debugSession;
  }

  public void setBreakpoint(final Breakpoint.Type type, final String target,
      final int line, int position, final boolean enabled, final String condition,
      final int ignoreCount, final JavascriptVm.BreakpointCallback callback,
      SyncCallback syncCallback) {

    final String scriptName;
    final Long scriptId;
    Object targetObject;
    if (type == Breakpoint.Type.SCRIPT_ID) {
      scriptName = null;
      scriptId = Long.parseLong(target);
      targetObject = scriptId;
    } else if (type == Breakpoint.Type.SCRIPT_NAME) {
      scriptName = target;
      scriptId = null;
      targetObject = scriptName;
    } else {
      throw new IllegalArgumentException("Unsupported breakpoint type " + type);
    }

    debugSession.sendMessageAsync(
        DebuggerMessageFactory.setBreakpoint(type, targetObject, toNullableInteger(line),
            toNullableInteger(position), enabled, condition,
            toNullableInteger(ignoreCount)),
        true,
        callback == null
            ? null
            : new V8CommandCallbackBase() {
              @Override
              public void success(SuccessCommandResponse successResponse) {
                BreakpointBody body;
                try {
                  body = successResponse.getBody().asBreakpointBody();
                } catch (JsonProtocolParseException e) {
                  throw new RuntimeException(e);
                }
                long id = body.getBreakpoint();

                final BreakpointImpl breakpoint =
                    new BreakpointImpl(type, id, scriptName, scriptId, line, enabled, ignoreCount,
                        condition, BreakpointManager.this);

                callback.success(breakpoint);
                idToBreakpoint.put(breakpoint.getId(), breakpoint);
              }
              @Override
              public void failure(String message) {
                if (callback != null) {
                  callback.failure(message);
                }
              }
            },
            syncCallback);
  }

  public Breakpoint getBreakpoint(Long id) {
    return idToBreakpoint.get(id);
  }

  public void clearBreakpoint(
      final BreakpointImpl breakpointImpl, final BreakpointCallback callback,
      SyncCallback syncCallback) {
    long id = breakpointImpl.getId();
    if (id == Breakpoint.INVALID_ID) {
      return;
    }
    idToBreakpoint.remove(id);
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.clearBreakpoint(breakpointImpl),
        true,
        new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            if (callback != null) {
              callback.success(null);
            }
          }
          @Override
          public void failure(String message) {
            if (callback != null) {
              callback.failure(message);
            }
          }
        },
        syncCallback);
  }

  public void changeBreakpoint(final BreakpointImpl breakpointImpl,
      final BreakpointCallback callback, SyncCallback syncCallback) {
    debugSession.sendMessageAsync(
        DebuggerMessageFactory.changeBreakpoint(breakpointImpl),
        true,
        new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            if (callback != null) {
              callback.success(breakpointImpl);
            }
          }
          @Override
          public void failure(String message) {
            if (callback != null) {
              callback.failure(message);
            }
          }
        },
        syncCallback);
  }

  /**
   * Reads a list of breakpoints from remote and updates local instances and the map.
   */
  public void reloadBreakpoints(final ListBreakpointsCallback callback, SyncCallback syncCallback) {
    V8CommandCallbackBase v8Callback = new V8CommandCallbackBase() {
      @Override
      public void failure(String message) {
        callback.failure(new Exception(message));
      }
      @Override
      public void success(SuccessCommandResponse successResponse) {
        CommandResponseBody body = successResponse.getBody();
        ListBreakpointsBody listBreakpointsBody;
        try {
          listBreakpointsBody = body.asListBreakpointsBody();
        } catch (JsonProtocolParseException e) {
          callback.failure(new Exception("Failed to read server response", e));
          return;
        }
        List<BreakpointInfo> infos = listBreakpointsBody.breakpoints();
        try {
          syncBreakpoints(infos);
        } catch (RuntimeException e) {
          callback.failure(new Exception("Failed to read server response", e));
          return;
        }
        callback.success(Collections.unmodifiableCollection(idToBreakpoint.values()));
      }
    };
    debugSession.sendMessageAsync(new ListBreakpointsMessage(), true, v8Callback, syncCallback);
  }

  private static Integer toNullableInteger(int value) {
    return value == Breakpoint.EMPTY_VALUE
        ? null
        : value;
  }

  private void syncBreakpoints(List<BreakpointInfo> infoList) {
    Map<Long, BreakpointImpl> actualBreakpoints = new HashMap<Long, BreakpointImpl>();
    // Wrap all loaded BreakpointInfo as BreakpointImpl, possibly reusing old instances.
    // Also check that all breakpoint id's in loaded list are unique.
    for (BreakpointInfo info : infoList) {
      if (info.type() == BreakpointInfo.Type.function) {
        // We does not support function type breakpoints and ignore them.
        continue;
      }
      BreakpointImpl breakpoint = idToBreakpoint.get(info.number());
      if (breakpoint == null) {
        breakpoint = new BreakpointImpl(info, this);
      } else {
        breakpoint.updateFromRemote(info);
      }
      Object conflict = actualBreakpoints.put(info.number(), breakpoint);
      if (conflict != null) {
        throw new RuntimeException("Duplicated breakpoint number " + info.number());
      }
    }

    // Remove all obsolete breakpoints from the map.
    for (Iterator<Long> it = idToBreakpoint.keySet().iterator(); it.hasNext(); ) {
      Long id = it.next();
      if (!actualBreakpoints.containsKey(id)) {
        it.remove();
      }
    }

    // Add breakpoints that are not in the main map yet.
    for (BreakpointImpl breakpoint : actualBreakpoints.values()) {
      if (!idToBreakpoint.containsKey(breakpoint.getId())) {
        idToBreakpoint.put(breakpoint.getId(), breakpoint);
      }
    }
  }
}
