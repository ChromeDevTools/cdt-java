// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Operation;

/**
 * A wrapper around {@link AtomicReference} and {@link AsyncFuture} that makes the source code
 * cleaner and shorter.
 */
public class AsyncFutureRef<T> {
  private final AtomicReference<AsyncFuture<T>> ref = new AtomicReference<AsyncFuture<T>>(null);

  public void initializeRunning(Operation<T> requester) {
    AsyncFuture.initializeReference(ref, requester);
  }

  public void initializeTrivial(T value) {
    AsyncFuture.initializeTrivial(ref, value);
  }

  public boolean isInitialized() {
    return ref.get() != null;
  }

  public T getSync() {
    return ref.get().getSync();
  }

  public RelayOk getAsync(AsyncFuture.Callback<T> callback, SyncCallback syncCallback) {
    return ref.get().getAsync(callback, syncCallback);
  }

  public boolean isDone() {
    return ref.get().isDone();
  }
}
