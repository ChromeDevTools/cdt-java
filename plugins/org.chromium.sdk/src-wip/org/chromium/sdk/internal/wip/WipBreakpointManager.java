// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Breakpoint.Target;
import org.chromium.sdk.CallFrame;
import org.chromium.sdk.JavascriptVm.BreakpointCallback;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.wip.protocol.input.debugger.BreakpointResolvedEventData;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A manager that works as factory for breakpoints.
 */
public class WipBreakpointManager {
  private final WipTabImpl tabImpl;
  private final AtomicInteger breakpointUniqueId = new AtomicInteger(0);
  private final Db db = new Db();

  WipBreakpointManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  RelayOk setBreakpoint(Breakpoint.Target target, final int line, final int column,
      final boolean enabled, String condition, int ignoreCount,
      final BreakpointCallback callback, SyncCallback syncCallback) {

    int sdkId = breakpointUniqueId.getAndAdd(1);

    final WipBreakpointImpl breakpointImpl = new WipBreakpointImpl(this, sdkId,
        target, line, column, condition, enabled);

    db.addBreakpoint(breakpointImpl);

    if (enabled) {
      if (condition == null) {
        condition = "";
      }

      final String conditionFinal = condition;

      WipBreakpointImpl.SetBreakpointCallback wrappedCallback =
          new WipBreakpointImpl.SetBreakpointCallback() {
        @Override
        public void onSuccess(String protocolId,
            Collection<WipBreakpointImpl.ActualLocation> actualLocations) {
          breakpointImpl.setRemoteData(protocolId, actualLocations);
          if (callback != null) {
            callback.success(breakpointImpl);
          }
        }

        @Override
        public void onFailure(Exception exception) {
          if (callback != null) {
            callback.failure(exception.getMessage());
          }
        }
      };

      return WipBreakpointImpl.sendSetBreakpointRequest(target, line, column, condition,
          wrappedCallback, syncCallback, tabImpl.getCommandProcessor());
    } else {
      callback.success(breakpointImpl);
      return RelaySyncCallback.finish(syncCallback);
    }
  }

  Db getDb() {
    return db;
  }

  void clearNonProvisionalBreakpoints() {
    db.visitAllBreakpoints(new Db.Visitor<Void>() {
      @Override
      public Void visitAllBreakpoints(Set<WipBreakpointImpl> breakpoints) {
        List<WipBreakpointImpl> deleteList = new ArrayList<WipBreakpointImpl>();
        for (WipBreakpointImpl breakpoint : breakpoints) {
          if (breakpoint.getTarget().accept(IS_SCRIPT_ID_VISITOR)) {
            deleteList.add(breakpoint);
          } else {
            breakpoint.clearActualLocations();
          }
        }
        for (WipBreakpointImpl breakpoint : deleteList) {
          breakpoint.deleteSelfFromDb();
        }
        return null;
      }
    });
  }

  private static final Breakpoint.Target.Visitor<Boolean> IS_SCRIPT_ID_VISITOR =
      new Breakpoint.Target.Visitor<Boolean>() {
    @Override
    public Boolean visitScriptName(String scriptName) {
      return Boolean.FALSE;
    }

    @Override
    public Boolean visitScriptId(long scriptId) {
      return Boolean.TRUE;
    }

    @Override
    public Boolean visitUnknown(Target target) {
      return Boolean.FALSE;
    }
  };

  WipCommandProcessor getCommandProcessor() {
    return tabImpl.getCommandProcessor();
  }

  Collection<WipBreakpointImpl> getAllBreakpoints() {
    return db.visitAllBreakpoints(new Db.Visitor<Collection<WipBreakpointImpl>>() {
      @Override
      public Collection<WipBreakpointImpl> visitAllBreakpoints(
          Set<WipBreakpointImpl> breakpoints) {
        return new ArrayList<WipBreakpointImpl>(breakpoints);
      }
    });
  }

  void breakpointReportedResolved(BreakpointResolvedEventData eventData) {
    String breakpointId = eventData.breakpointId();
    WipBreakpointImpl breakpoint = db.getBreakpoint(breakpointId);
    if (breakpoint == null) {
      throw new RuntimeException("Failed to find breakpoint by id: " + breakpointId);
    }
    breakpoint.addResolvedLocation(eventData.location());
  }

  // Accessed from Dispatch thread.
  public Collection<? extends Breakpoint> findRelatedBreakpoints(CallFrame topFrame) {
    TextStreamPosition position = topFrame.getStatementStartPosition();
    int line = position.getLine();
    int column = position.getColumn();

    String scriptId = String.valueOf(topFrame.getScript().getId());
    final WipBreakpointImpl.ActualLocation location =
        new WipBreakpointImpl.ActualLocation(scriptId, line, Long.valueOf(column));

    return db.visitAllBreakpoints(new Db.Visitor<List<Breakpoint>>() {
      @Override
      public List<Breakpoint> visitAllBreakpoints(Set<WipBreakpointImpl> breakpoints) {
        List<Breakpoint> result = new ArrayList<Breakpoint>(1);
        for (WipBreakpointImpl breakpoint : breakpoints) {
          if (breakpoint.getActualLocations().contains(location)) {
            result.add(breakpoint);
          }
        }
        return result;
      }
    });
  }

  /**
   * Breakpoint data-base. Keeps track of all instances and their protocol-id -> instance mapping.
   * The name implies that it doesn't manage anything, only stores data.
   */
  static class Db {
    // Accessed from any thread.
    private final Set<WipBreakpointImpl> breakpoints = new HashSet<WipBreakpointImpl>();

    // Access from Dispatch thread only.
    private final Map<String, WipBreakpointImpl> idToBreakpoint =
        new HashMap<String, WipBreakpointImpl>();

    void addBreakpoint(WipBreakpointImpl breakpoint) {
      synchronized (breakpoints) {
        breakpoints.add(breakpoint);
      }
    }

    void removeBreakpoint(WipBreakpointImpl breakpoint) {
      synchronized (breakpoints) {
        breakpoints.remove(breakpoint);
      }
    }

    void setIdMapping(WipBreakpointImpl breakpoint,
        String protocolId) {
      if (protocolId == null) {
        idToBreakpoint.remove(protocolId);
      } else {
        idToBreakpoint.put(protocolId, breakpoint);
      }
    }

    WipBreakpointImpl getBreakpoint(String breakpointId) {
      return idToBreakpoint.get(breakpointId);
    }

    /**
     * Gives a synchronized access to all breakpoints.
     */
    <R> R visitAllBreakpoints(Visitor<R> visitor) {
      synchronized (breakpoints) {
        return visitor.visitAllBreakpoints(breakpoints);
      }
    }

    interface Visitor<R> {
      R visitAllBreakpoints(Set<WipBreakpointImpl> breakpoints);
    }
  }
}
