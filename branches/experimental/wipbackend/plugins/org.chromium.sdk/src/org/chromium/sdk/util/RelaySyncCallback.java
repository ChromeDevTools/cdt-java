// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;

/**
 * A utility class for handling {@link SyncCallback} in chained asynchronous operations.
 * Whatever happens to each step of operation, {@link SyncCallback} must be called once
 * in the end. {@link RelaySyncCallback} wraps the {@link SyncCallback} for the whole
 * multistep operation. It works in pair with {@link RelaySyncCallback.Guard} class.
 */
public class RelaySyncCallback {
  private final SyncCallback syncCallback;

  public RelaySyncCallback(SyncCallback syncCallback) {
    this.syncCallback = syncCallback;
  }

  public Guard newGuard() {
    return new Guard();
  }

  public SyncCallback getSyncCallback() {
    return syncCallback;
  }

  /**
   * Finish relay by calling {@link SyncCallback}.
   */
  public RelayOk finish() {
    return finish(syncCallback);
  }

  public static RelayOk finish(SyncCallback syncCallback) {
    if (syncCallback != null) {
      syncCallback.callbackDone(null);
    }
    return FINISH_RELAY_OK;
  }

  /**
   * Responsible for calling SyncCallback unless
   * operation has been successfully relayed to the next step.
   */
  public class Guard {
    private final AtomicBoolean discharged = new AtomicBoolean(false);

    /**
     * This method should be called when operations was successfully relayed. This is typically
     * a last statement in operation step, right before relay call.
     * Failing to call {@link #discharge} (because of abnormal finishing) would cause guard to
     * call {@link SyncCallback} meaning the termination of the operation.
     * @param relayed
     */
    public void discharge(RelayOk relayed) {
      discharged.set(true);
    }

    /**
     * @return guard wrapped as {@link SyncCallback} that that would let the guard to fulfill its
     *      main contract
     */
    public SyncCallback asSyncCallback() {
      return innerSyncCallback;
    }

    public RelaySyncCallback getRelay() {
      return RelaySyncCallback.this;
    }

    private final SyncCallback innerSyncCallback = new SyncCallback() {
      @Override
      public void callbackDone(RuntimeException e) {
        boolean updated = discharged.compareAndSet(false, true);
        if (updated) {
          if (syncCallback != null) {
            syncCallback.callbackDone(e);
          }
        }
      }
    };
  }

  private static final RelayOk FINISH_RELAY_OK = new RelayOk() {};
}
