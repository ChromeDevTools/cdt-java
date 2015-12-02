// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.IgnoreCountBreakpointExtension;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptRegExpBreakpointTarget;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Success;
import org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue;
import org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData;
import org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse;
import org.chromium.sdk.internal.wip.protocol.output.debugger.LocationParam;
import org.chromium.sdk.internal.wip.protocol.output.debugger.RemoveBreakpointParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetBreakpointByUrlParams;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetBreakpointParams;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Wip-based breakpoint implementation.
 * The implementation is based on volatile fields and expects client code to do some
 * synchronization (serialize calls to setters, {@link #flush} and {@link #clear}).
 */
public class WipBreakpointImpl implements Breakpoint {
  private final WipBreakpointManager breakpointManager;

  private final Target target;

  private final int lineNumber;
  private final int columnNumber;

  private final int sdkId;
  private volatile String protocolId = null;

  private volatile String condition;
  private volatile boolean enabled;
  private volatile boolean isDirty;

  // Access only from Dispatch thread.
  private Set<ActualLocation> actualLocations = new HashSet<ActualLocation>(2);

  public WipBreakpointImpl(WipBreakpointManager breakpointManager, int sdkId, Target target,
      int lineNumber, int columnNumber, String condition, boolean enabled) {
    this.breakpointManager = breakpointManager;
    this.sdkId = sdkId;
    this.target = target;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.condition = condition;
    this.enabled = enabled;

    this.isDirty = false;
  }

  @Override
  public Target getTarget() {
    return target;
  }

  @Override
  public long getId() {
    return sdkId;
  }

  public static final BreakpointTypeExtension TYPE_EXTENSION = new BreakpointTypeExtension() {
    @Override
    public FunctionSupport getFunctionSupport() {
      return null;
    }
    @Override
    public ScriptRegExpSupport getScriptRegExpSupport() {
      return scriptRegExpSupport;
    }

    private final ScriptRegExpSupport scriptRegExpSupport = new ScriptRegExpSupport() {
      @Override
      public Target createTarget(String regExp) {
        return new ScriptRegExpBreakpointTarget(regExp);
      }
    };
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
    if (enabled == this.enabled) {
      return;
    }
    this.enabled = enabled;
    isDirty  = true;
  }

  @Override
  public String getCondition() {
    return condition;
  }

  @Override
  public void setCondition(String condition) {
    if (eq(this.condition, condition)) {
      return;
    }
    this.condition = condition;
    isDirty = true;
  }

  @Override
  public IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtension() {
    return getIgnoreCountBreakpointExtensionImpl();
  }

  public static IgnoreCountBreakpointExtension getIgnoreCountBreakpointExtensionImpl() {
    // TODO(peter.rybin): implement when protocol supports.
    return null;
  }


  void setRemoteData(String protocolId, Collection<ActualLocation> actualLocations) {
    this.protocolId = protocolId;
    this.actualLocations.clear();
    this.actualLocations.addAll(actualLocations);
    this.breakpointManager.getDb().setIdMapping(this, protocolId);
  }

  void addResolvedLocation(LocationValue locationValue) {
    ActualLocation location = locationFromProtocol(locationValue);
    actualLocations.add(location);
  }

  Set<ActualLocation> getActualLocations() {
    return actualLocations;
  }

  void clearActualLocations() {
    actualLocations.clear();
  }

  void deleteSelfFromDb() {
    if (protocolId != null) {
      breakpointManager.getDb().setIdMapping(this, null);
    }
    breakpointManager.getDb().removeBreakpoint(this);
  }

  @Override
  public RelayOk clear(final BreakpointCallback callback, SyncCallback syncCallback) {
    // TODO: make sure this is thread-safe.
    if (protocolId == null) {
      breakpointManager.getDb().removeBreakpoint(this);
      callback.success(this);
      return RelaySyncCallback.finish(syncCallback);
    }

    RemoveBreakpointParams params = new RemoveBreakpointParams(protocolId);

    WipCommandCallback commandCallback;
    if (callback == null) {
      commandCallback = null;
    } else {
      commandCallback = new WipCommandCallback.Default() {
        @Override protected void onSuccess(Success success) {
          breakpointManager.getDb().setIdMapping(WipBreakpointImpl.this, null);
          breakpointManager.getDb().removeBreakpoint(WipBreakpointImpl.this);
          callback.success(WipBreakpointImpl.this);
        }
        @Override protected void onError(String message) {
          callback.failure(message);
        }
      };
    }

    return breakpointManager.getCommandProcessor().send(params, commandCallback, syncCallback);
  }

  @Override
  public RelayOk flush(final BreakpointCallback callback, final SyncCallback syncCallback) {
    final RelaySyncCallback relay = new RelaySyncCallback(syncCallback);

    if (!isDirty) {
      if (callback != null) {
        callback.success(this);
      }
      return RelaySyncCallback.finish(syncCallback);
    }

    isDirty = false;

    if (protocolId == null) {
      // Breakpoint was disabled, it doesn't exist in VM, immediately start step 2.
      return recreateBreakpointAsync(callback, relay);
    } else {
      // Call syncCallback if something goes wrong after we sent request.
      final RelaySyncCallback.Guard guard = relay.newGuard();

      WipCommandCallback removeCallback = new WipCommandCallback.Default() {
        @Override
        protected void onSuccess(Success success) {
          setRemoteData(null, Collections.<ActualLocation>emptyList());
          RelayOk relayOk = recreateBreakpointAsync(callback, relay);
          guard.discharge(relayOk);
        }

        @Override
        protected void onError(String message) {
          throw new RuntimeException("Failed to remove breakpoint: " + message);
        }
      };

      // Call syncCallback if something goes wrong.
      return breakpointManager.getCommandProcessor().send(new RemoveBreakpointParams(protocolId),
          removeCallback, guard.asSyncCallback());
    }
  }

  static class ActualLocation {
    private final String sourceId;
    private final long lineNumber;
    private final Long columnNumber;

    ActualLocation(String sourceId, long lineNumber, Long columnNumber) {
      this.sourceId = sourceId;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    @Override
    public boolean equals(Object obj) {
      ActualLocation other = (ActualLocation) obj;
      return this.sourceId.equals(other.sourceId) &&
          this.lineNumber == other.lineNumber &&
          eq(this.columnNumber, other.columnNumber);
    }

    @Override
    public int hashCode() {
      int column;
      if (columnNumber == null) {
        column = 0;
      } else {
        column = columnNumber.intValue();
      }
      return sourceId.hashCode() + 31 * (int) lineNumber + column;
    }

    @Override
    public String toString() {
      return "<sourceId=" + sourceId + ", line=" + lineNumber + ", column=" + columnNumber + ">";
    }
  }

  private RelayOk recreateBreakpointAsync(final BreakpointCallback flushCallback,
      RelaySyncCallback relay) {

    if (enabled) {
      SetBreakpointCallback setCommandCallback = new SetBreakpointCallback() {
        @Override
        public void onSuccess(String protocolId, Collection<ActualLocation> actualLocations) {
          setRemoteData(protocolId, actualLocations);
          if (flushCallback != null) {
            flushCallback.success(WipBreakpointImpl.this);
          }
        }

        @Override
        public void onFailure(Exception exception) {
          if (flushCallback != null) {
            flushCallback.failure(exception.getMessage());
          }
        }
      };

      RelaySyncCallback.Guard guard = relay.newGuard();

      if (condition == null) {
        condition = "";
      }
      return sendSetBreakpointRequest(target, lineNumber, columnNumber, condition,
          setCommandCallback, guard.asSyncCallback(),
          breakpointManager.getCommandProcessor());
    } else {
      // Breakpoint is disabled, do not create it.
      RelayOk relayOk;
      try {
        if (flushCallback != null) {
          flushCallback.success(WipBreakpointImpl.this);
        }
      } finally {
        relayOk = relay.finish();
      }
      return relayOk;
    }
  }

  interface SetBreakpointCallback {
    void onSuccess(String breakpointId, Collection<ActualLocation> actualLocations);
    void onFailure(Exception cause);
  }

  /**
   * @param callback a generic callback that receives breakpoint protocol id
   * @return
   */
  static RelayOk sendSetBreakpointRequest(Target target, final int lineNumber,
      int columnNumber, final String condition,
      final SetBreakpointCallback callback, final SyncCallback syncCallback,
      final WipCommandProcessor commandProcessor) {

    final Long columnNumberParam;
    if (columnNumber == Breakpoint.EMPTY_VALUE) {
      columnNumberParam = null;
    } else {
      columnNumberParam = Long.valueOf(columnNumber);
    }

    return target.accept(new BreakpointTypeExtension.ScriptRegExpSupport.Visitor<RelayOk>() {
      @Override
      public RelayOk visitScriptName(String scriptName) {
        return sendRequest(scriptName, RequestHandler.FOR_URL);
      }

      @Override
      public RelayOk visitRegExp(String regExp) {
        return sendRequest(regExp, RequestHandler.FOR_REGEXP);
      }

      @Override
      public RelayOk visitScriptId(Object scriptId) {
        String scriptIdString = WipScriptManager.convertAlienSourceId(scriptId);
        return sendRequest(scriptIdString, RequestHandler.FOR_ID);
      }

      @Override
      public RelayOk visitUnknown(Target target) {
        throw new IllegalArgumentException();
      }

      private <T, DATA, PARAMS extends WipParamsWithResponse<DATA>> RelayOk sendRequest(
          T parameter, final RequestHandler<T, DATA, PARAMS> handler) {
        PARAMS requestParams =
            handler.createRequestParams(parameter, lineNumber, columnNumberParam, condition);

        GenericCallback<DATA> wrappedCallback;
        if (callback == null) {
          wrappedCallback = null;
        } else {
          wrappedCallback = new GenericCallback<DATA>() {
            @Override
            public void success(DATA data) {
              String breakpointId = handler.getBreakpointId(data);
              Collection<LocationValue> locationValues = handler.getActualLocations(data);
              List<ActualLocation> locationList =
                  new ArrayList<ActualLocation>(locationValues.size());
              for (LocationValue value : locationValues) {
                locationList.add(locationFromProtocol(value));
              }
              callback.onSuccess(breakpointId, locationList);
            }

            @Override
            public void failure(Exception exception) {
              callback.onFailure(exception);
            }
          };
        }

        return commandProcessor.send(requestParams, wrappedCallback, syncCallback);
      }
    });
  }

  private static abstract class RequestHandler<T,
      DATA, PARAMS extends WipParamsWithResponse<DATA>> {

    abstract PARAMS createRequestParams(T parameter, long lineNumber, Long columnNumberOpt,
        String conditionOpt);

    abstract String getBreakpointId(DATA data);

    abstract Collection<LocationValue> getActualLocations(DATA data);

    static abstract class ForUrlOrRegExp
        extends RequestHandler<String, SetBreakpointByUrlData, SetBreakpointByUrlParams> {
      @Override
      String getBreakpointId(SetBreakpointByUrlData data) {
        return data.breakpointId();
      }

      @Override
      Collection<LocationValue> getActualLocations(SetBreakpointByUrlData data) {
        return data.locations();
      }
    }

    static final ForUrlOrRegExp FOR_URL = new ForUrlOrRegExp() {
      @Override
      SetBreakpointByUrlParams createRequestParams(String url,
          long lineNumber, Long columnNumber, String condition) {
        return new SetBreakpointByUrlParams(lineNumber, url, null, columnNumber, condition);
      }
    };

    static final ForUrlOrRegExp FOR_REGEXP = new ForUrlOrRegExp() {
      @Override
      SetBreakpointByUrlParams createRequestParams(String url,
          long lineNumber, Long columnNumber, String condition) {
        return new SetBreakpointByUrlParams(lineNumber, null, url, columnNumber, condition);
      }
    };

    static final RequestHandler<String, SetBreakpointData, SetBreakpointParams> FOR_ID =
        new RequestHandler<String, SetBreakpointData, SetBreakpointParams>() {
          @Override
          SetBreakpointParams createRequestParams(String sourceId,
              long lineNumber, Long columnNumber, String condition) {
            LocationParam locationParam =
                new LocationParam(sourceId, lineNumber, columnNumber);
            return new SetBreakpointParams(locationParam, condition);
          }

          @Override
          String getBreakpointId(SetBreakpointData data) {
            return data.breakpointId();
          }

          @Override
          Collection<LocationValue> getActualLocations(SetBreakpointData data) {
            return Collections.singletonList(data.actualLocation());
          }
        };
  }

  private static ActualLocation locationFromProtocol(LocationValue locationValue) {
    return new ActualLocation(locationValue.scriptId(), locationValue.lineNumber(),
        locationValue.columnNumber());
  }

  private static <T> boolean eq(T left, T right) {
    return left == right || (left != null && left.equals(right));
  }
}
