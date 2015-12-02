// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.AsyncFuture.Callback;

/**
 * A class that provides an {@link AsyncFuture} for a group operation: one that consists of any
 * number of sub-operations and completes once all of them have completed.
 * The class does not care about nature of sub-operations, it only keeps their number. User
 * registers new sub-operations ({@link #addSubOperation()}) and reports on their completion
 * ({@link #subOperationDone} and ({@link #subOperationDoneSync}). One sub-operation is registered
 * by default. Once all sub-operations completed,
 * the main operation is considered done and the future returns the group result. Groups result is
 * a list of sub-operation results.
 *
 * @param <T> type of sub-operation result
 */
public class AsyncFutureMerger<T> {
  private final AtomicReference<AsyncFuture<List<T>>> futureRef;
  private AsyncFuture.Callback<List<T>> callback;
  private SyncCallback syncCallback;

  private final List<T> resultBuffer = new ArrayList<T>();
  private int subOperationCounter = 1;
  private int subOperationSyncCounter = 1;
  private RuntimeException savedException = null;

  public AsyncFutureMerger() {
    futureRef = new AtomicReference<AsyncFuture<List<T>>>();
    AsyncFuture.initializeReference(futureRef, new AsyncFuture.Operation<List<T>>() {
      @Override
      public RelayOk start(Callback<List<T>> callback, SyncCallback syncCallback) {
        AsyncFutureMerger.this.callback = callback;
        AsyncFutureMerger.this.syncCallback = syncCallback;
        return SOMEONE_CARES_ABOUT_RELAY_OK;
      }
    });
  }

  /**
   * Registers a new sub-operation.
   * This method is not thread-safe. User must synchronize access himself.
   */
  public void addSubOperation() {
    assert !futureRef.get().isDone();

    subOperationCounter++;
    subOperationSyncCounter++;
  }

  /**
   * Registers a sub-operation completion and saves its result.
   * This call is not mandatory and user may fail to make it; this won't cause waiting thread
   * to block forever.
   * This method is not thread-safe. User must synchronize access himself.
   */
  public void subOperationDone(T result) {
    resultBuffer.add(result);
    subOperationCounter--;
    if (subOperationCounter == 0) {
      callback.done(resultBuffer);
    }
  }

  /**
   * Additionally registers a sub-operation completion. This method should be called only after
   * {@link #subOperationDone} and this call is mandatory and must be made even in case of
   * sub-operation failure; this method is typically called form finally section of
   * the top-level procedure.
   * Other threads may block infinitely unless this method is being called a proper number of times.
   * This method is not thread-safe. User must synchronize access himself.
   * @param exception optional value of a failure that prevented corresponding call
   *     to {@link #subOperationDone}
   */
  public void subOperationDoneSync(RuntimeException exception) {
    if (exception != null && savedException == null) {
      savedException = exception;
    }
    subOperationSyncCounter--;
    if (subOperationSyncCounter == 0) {
      syncCallback.callbackDone(savedException);
    }
  }

  /**
   * Returns {@link AsyncFuture} that provides the group result.
   * This method is thread-safe.
   */
  public AsyncFuture<List<T>> getFuture() {
    return futureRef.get();
  }

  private static final RelayOk SOMEONE_CARES_ABOUT_RELAY_OK = new RelayOk() {};
}