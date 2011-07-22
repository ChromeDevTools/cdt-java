// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Breakpoint.Target;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.JavascriptVm.ExceptionCatchType;
import org.chromium.sdk.JavascriptVm.GenericCallback;
import org.chromium.sdk.JavascriptVm.ListBreakpointsCallback;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptRegExpBreakpointTarget;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.BreakpointImpl.FunctionTarget;
import org.chromium.sdk.internal.v8native.protocol.input.BreakpointBody;
import org.chromium.sdk.internal.v8native.protocol.input.CommandResponseBody;
import org.chromium.sdk.internal.v8native.protocol.input.FlagsBody;
import org.chromium.sdk.internal.v8native.protocol.input.FlagsBody.FlagInfo;
import org.chromium.sdk.internal.v8native.protocol.input.ListBreakpointsBody;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.data.BreakpointInfo;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.protocol.output.FlagsMessage;
import org.chromium.sdk.internal.v8native.protocol.output.ListBreakpointsMessage;
import org.chromium.sdk.util.RelaySyncCallback;

public class BreakpointManager {
  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(BreakpointManager.class.getName());

  /**
   * This map shall contain only breakpoints with valid IDs.
   * Complex operations must be explicitly synchronized on this instance.
   */
  private final Map<Long, BreakpointImpl> idToBreakpoint =
      Collections.synchronizedMap(new HashMap<Long, BreakpointImpl>());

  private final DebugSession debugSession;

  public BreakpointManager(DebugSession debugSession) {
    this.debugSession = debugSession;
  }

  public BreakpointTypeExtension getBreakpointTypeExtension() {
    return breakpointTypeExtension;
  }

  public RelayOk setBreakpoint(final Breakpoint.Target target,
      final int line, int column, final boolean enabled, final String condition,
      final int ignoreCount, final JavascriptVm.BreakpointCallback callback,
      SyncCallback syncCallback) {
    return debugSession.sendMessageAsync(
        DebuggerMessageFactory.setBreakpoint(target, toNullableInteger(line),
            toNullableInteger(column), enabled, condition,
            toNullableInteger(ignoreCount)),
        true,
        new V8CommandCallbackBase() {
          @Override
          public void success(SuccessCommandResponse successResponse) {
            BreakpointBody body;
            try {
              body = successResponse.body().asBreakpointBody();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }
            long id = body.breakpoint();

            final BreakpointImpl breakpoint =
                new BreakpointImpl(id, target, line, enabled, ignoreCount,
                    condition, BreakpointManager.this);

            idToBreakpoint.put(breakpoint.getId(), breakpoint);
            if (callback != null) {
              callback.success(breakpoint);
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

  public Breakpoint getBreakpoint(Long id) {
    return idToBreakpoint.get(id);
  }

  public RelayOk clearBreakpoint(
      final BreakpointImpl breakpointImpl, final BreakpointCallback callback,
      SyncCallback syncCallback, long originalId) {
    long id = originalId;
    if (id == Breakpoint.INVALID_ID) {
      return RelaySyncCallback.finish(syncCallback);
    }
    idToBreakpoint.remove(id);
    return debugSession.sendMessageAsync(
        DebuggerMessageFactory.clearBreakpoint(id),
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

  public RelayOk changeBreakpoint(final BreakpointImpl breakpointImpl,
      final BreakpointCallback callback, SyncCallback syncCallback) {
    return debugSession.sendMessageAsync(
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
   * @return
   */
  public RelayOk reloadBreakpoints(final ListBreakpointsCallback callback,
      SyncCallback syncCallback) {
    V8CommandCallbackBase v8Callback = new V8CommandCallbackBase() {
      @Override
      public void failure(String message) {
        callback.failure(new Exception(message));
      }
      @Override
      public void success(SuccessCommandResponse successResponse) {
        CommandResponseBody body = successResponse.body();
        ListBreakpointsBody listBreakpointsBody;
        try {
          listBreakpointsBody = body.asListBreakpointsBody();
        } catch (JsonProtocolParseException e) {
          callback.failure(new Exception("Failed to read server response", e));
          return;
        }
        List<BreakpointInfo> infos = listBreakpointsBody.breakpoints();
        Collection<Breakpoint> updatedBreakpoints;
        try {
          updatedBreakpoints = syncBreakpoints(infos);
        } catch (RuntimeException e) {
          callback.failure(new Exception("Failed to read server response", e));
          return;
        }
        callback.success(Collections.unmodifiableCollection(updatedBreakpoints));
      }
    };
    return debugSession.sendMessageAsync(new ListBreakpointsMessage(), true, v8Callback,
        syncCallback);
  }

  public RelayOk enableBreakpoints(Boolean enabled, final GenericCallback<Boolean> callback,
      SyncCallback syncCallback) {
    return setRemoteFlag("breakPointsActive", enabled, callback, syncCallback);
  }

  public RelayOk setBreakOnException(ExceptionCatchType catchType, Boolean enabled,
      final GenericCallback<Boolean> callback, SyncCallback syncCallback) {
    String flagName;
    switch (catchType) {
      case CAUGHT:
        flagName = "breakOnCaughtException";
        break;
      case UNCAUGHT:
        flagName = "breakOnUncaughtException";
        break;
      default:
        throw new RuntimeException();
    }
    return setRemoteFlag(flagName, enabled, callback, syncCallback);
  }

  private RelayOk setRemoteFlag(final String flagName, Boolean enabled,
      final GenericCallback<Boolean> callback, SyncCallback syncCallback) {
    Map<String, Object> flagMap = Collections.singletonMap(flagName, (Object) enabled);
    V8CommandProcessor.V8HandlerCallback v8Callback;
    if (callback == null) {
      v8Callback = null;
    } else {
      v8Callback = new V8CommandCallbackBase() {
        @Override public void success(SuccessCommandResponse successResponse) {
          FlagsBody body;
          try {
            body = successResponse.body().asFlagsBody();
          } catch (JsonProtocolParseException e) {
            throw new RuntimeException(e);
          }
          FlagsBody.FlagInfo flag;
          List<FlagInfo> flagList = body.flags();
          findCorrectFlag: {
            for (int i = 0; i < flagList.size(); i++) {
              if (flagName.equals(flagList.get(i).name())) {
                flag = flagList.get(i);
                break findCorrectFlag;
              }
            }
            throw new RuntimeException("Failed to find the correct flag in response");
          }
          Object value = flag.value();
          Boolean resValue;
          if (value instanceof Boolean == false) {
            LOGGER.info("Flag value has a wrong type");
            resValue = null;
          } else {
            resValue = (Boolean) value;
          }
          callback.success(resValue);
        }
        @Override public void failure(String message) {
          callback.failure(new Exception(message));
        }
      };
    }
    return debugSession.sendMessageAsync(new FlagsMessage(flagMap), true, v8Callback, syncCallback);
  }

  private static Integer toNullableInteger(int value) {
    return value == Breakpoint.EMPTY_VALUE
        ? null
        : value;
  }

  private Collection<Breakpoint> syncBreakpoints(List<BreakpointInfo> infoList) {
    synchronized (idToBreakpoint) {
      ArrayList<Breakpoint> result = new ArrayList<Breakpoint>();
      Map<Long, BreakpointImpl> actualBreakpoints = new HashMap<Long, BreakpointImpl>();
      // Wrap all loaded BreakpointInfo as BreakpointImpl, possibly reusing old instances.
      // Also check that all breakpoint id's in loaded list are unique.
      for (BreakpointInfo info : infoList) {
        if (info.type() == BreakpointInfo.Type.FUNCTION) {
          // We don't support function type breakpoints and ignore them.
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
        result.add(breakpoint);
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
          result.add(breakpoint);
        }
      }
      return result;
    }
  }

  private final BreakpointTypeExtension breakpointTypeExtension = new BreakpointTypeExtension() {
    @Override
    public FunctionSupport getFunctionSupport() {
      return functionSupport;
    }

    private final FunctionSupport functionSupport = new FunctionSupport() {
      @Override
      public Target createTarget(String expression) {
        return new FunctionTarget(expression);
      }
    };

    @Override
    public ScriptRegExpSupport getScriptRegExpSupport() {
      if (!V8VersionFeatures.isRegExpBreakpointSupported(debugSession.getVmVersion())) {
        return null;
      }
      return scriptRegExpSupport;
    }

    private final ScriptRegExpSupport scriptRegExpSupport = new ScriptRegExpSupport() {
      @Override
      public Target createTarget(String regExp) {
        return new ScriptRegExpBreakpointTarget(regExp);
      }
    };

  };
}
