// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructingGuard;

/**
 * A utility class that connects SyncCallback paradigm with Destructable paradigm.
 * The former helps never miss a lock release call (it gets called even if the main
 * "logic" callback failed). The later ensures that a resource gets released sooner or later
 * for a multi-step operations (where each step may fail).
 * This wrapper is useful for multi-step SyncCallback-driven processes.
 */
class DestructableWrapper {
  /**
   * Wraps sync callback as {@link Destructable}, so that we could use a standard idiom
   * for making sure we call it sooner or later.
   */
  static Destructable callbackAsDestructable(final SyncCallback syncCallback) {
    return new Destructable() {
      @Override
      public void destruct() {
        if (syncCallback != null) {
          syncCallback.callbackDone(null);
        }
      }
    };
  }

  /**
   * Ties {@link DestructingGuard} with a sync callback, so that a sync callback call
   * worked as a 'finally' call to the guard.
   */
  static SyncCallback guardAsCallback(final DestructingGuard guard) {
    return new SyncCallback() {
      @Override
      public void callbackDone(RuntimeException e) {
        guard.doFinally();
      }
    };
  }
}
