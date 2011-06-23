// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;

/**
 * Represents a result of asynchronous operation. Unlike {@link Future}, the result may be obtained
 * both synchronously (blocking getter method) and asynchronously.
 * <p>The class provides a low-level service and should be used with {@link AtomicReference} class.
 * {@link AsyncFutureRef} offers a slightly more convenient interface for the price of extra object
 * (you may find it significant when there are tons of operations).
 * <p>The owner of the future operation must have a permanent field of type
 * {@link AtomicReference&lt;AsyncFuture&gt;}. It will consequently have the following values:
 * <ol>
 * <li>null -- operation is idle (hasn't been started yet); user will start it some time later;
 * <li>{@link AsyncFuture} instance that is performing the operation;
 * <li>{@link AsyncFuture} stub instance that simply keeps a result value and returns it very
 * quickly;
 * </ol>
 * The user only creates an empty {@link AtomicReference} object. Not before the result value is
 * actually needed the other objects are created and the operation is started. This is typically
 * happens in some getter. The method should:
 * <ul>
 * <li>check the reference; if it still holds null value, the user should initialize it by
 * {@link AsyncFuture#initializeReference} method; at this moment the operation is actually
 * prepared and starts; if several threads are doing this simultaneously only one operation
 * will be actually started;
 * <li>read {@link AsyncFuture} instance from the reference -- is will be not null at this moment;
 * <li>get the result from it by {@link #getSync()} or {@link #getAsync} methods.
 * </ul>
 * @param <T> type of the future result
 */
public abstract class AsyncFuture<T> {
  /**
   * Initializes the reference with the a new instance of {@link AsyncFuture} if the reference
   * still holds null. This has a semantics of starting the operation. If the reference already
   * holds non-null value, the method does nothing.
   * <p>This method is thread-safe.
   */
  public static <T> void initializeReference(AtomicReference<AsyncFuture<T>> ref,
      Operation<T> operation) {
    initializeReference(ref, operation, false);
  }

  /**
   * Initializes the reference with the a new instance of {@link AsyncFuture}. This
   * always works even if the reference has already been initialized. This has a semantics of
   * re-starting the operation and waiting for the new result.
   * <p>This method is thread-safe.
   */
  public static <T> void reinitializeReference(AtomicReference<AsyncFuture<T>> ref,
      Operation<T> operation) {
    initializeReference(ref, operation, true);
  }

  /**
   * Initializes the reference with the a new instance of {@link AsyncFuture} that already
   * holds a result. This method skips the calculation phase and may be needed to support
   * some trivial cases.
   * <p>This method is thread-safe.
   */
  public static <T> void initializeTrivial(AtomicReference<AsyncFuture<T>> ref, T result) {
    boolean updated;
    updated = ref.compareAndSet(null, new Done<T>(result));
  }

  /**
   * Operation may work synchronously. This method will block in this case.
   * @param <T>
   * @param ref
   * @param operation
   * @param forceRefresh
   */
  private static <T> void initializeReference(AtomicReference<AsyncFuture<T>> ref,
      Operation<T> operation, boolean forceRefresh) {
    // Creating worker not yet started (with fake relayOk).
    Working<T> working = new Working<T>(ref);
    boolean updated;
    if (forceRefresh) {
      // We exposed worker not yet started now.
      ref.set(working);
      updated = true;
    } else {
      // We possibly exposed worker not yet started now.
      updated = ref.compareAndSet(null, working);
    }
    if (updated) {
      // Make sure we started worker.
      RelayOk relayOk = working.start(operation);
      // It is important that this method returns RelayOk.
      // RelayOk symbolize we have started operation as we had to.
    }
  }

  /**
   * Returns the operation result. If the result is not ready yet, the method will block until
   * the operation finished.
   * @see #isDone()
   * @return the operation result
   */
  public abstract T getSync();

  /**
   * Obtains the operation result. The result is passed to callback immediately (synchronously) or
   * asynchronosuly later.
   * @param callback may be null
   * @param syncCallback may be null
   */
  public abstract RelayOk getAsync(Callback<? super T> callback, SyncCallback syncCallback);

  /**
   * Returns whether the operation is done. If the method returns true, the following calls
   * to {@link #getSync()} will return immediately. The following calls to {@link #getSync()}
   * of other instance of {@link AsyncFuture} that is held in the reference will also be
   * non-blocking, until {@link #reinitializeReference} is called with this reference.
   */
  public abstract boolean isDone();

  /**
   * A callback used in operation and in {@link AsyncFuture#getAsync} method.
   * @param <RES>
   */
  public interface Callback<RES> {
    void done(RES res);
  }

  /**
   * An operation that results in a value of type RES.
   */
  public interface Operation<RES> {
    /**
     * Starts the operation. The method can be blocking and perform the entire operation
     * or its part. In this case the corresponding call to {@link AsyncFuture#initializeReference}
     * or {@link AsyncFuture#reinitializeReference} will be blocking as well.
     */
    RelayOk start(Callback<RES> callback, SyncCallback syncCallback);
  }

  private static class Working<T> extends AsyncFuture<T> {
    private final AtomicReference<AsyncFuture<T>> ref;
    private final List<CallbackPair<T>> callbacks = new ArrayList<CallbackPair<T>>(1);
    private boolean resultReady = false;
    private T result;

    public Working(AtomicReference<AsyncFuture<T>> ref) {
      this.ref = ref;
    }

    public RelayOk start(Operation<T> operation) {
      Callback<T> callback = new Callback<T>() {
        @Override
        public void done(T res) {
          resultIsReady(res);
        }
      };
      SyncCallback syncCallback = new SyncCallback() {
        @Override
        public void callbackDone(RuntimeException e) {
          resultIsReadySync(e);
        }
      };
      return operation.start(callback, syncCallback);
    }

    @Override
    public RelayOk getAsync(Callback<? super T> callback, SyncCallback syncCallback) {
      synchronized (this) {
        if (!resultReady) {
          callbacks.add(new CallbackPair<T>(callback, syncCallback));
          return OPERATION_SHOULD_BE_RUNNING_RELAY_OK;
        }
      }
      return deliverResultImmediately(result, callback, syncCallback);
    }

    // We added callback to the chain. Will be called later.
    private static final RelayOk OPERATION_SHOULD_BE_RUNNING_RELAY_OK = new RelayOk() {};

    @Override
    public T getSync() {
      synchronized (this) {
        if (resultReady) {
          return result;
        }
      }
      class CallbackImpl implements Callback<T> {
        private T res;
        @Override
        public synchronized void done(T res) {
          this.res = res;
        }
        synchronized T get() {
          return res;
        }
      }
      CallbackImpl callback = new CallbackImpl();
      CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
      RelayOk relayOk = getAsync(callback, callbackSemaphore);
      callbackSemaphore.acquireDefault(relayOk);
      return callback.get();
    }

    @Override
    public boolean isDone() {
      synchronized (this) {
        return resultReady;
      }
    }

    private void resultIsReady(T result) {
      Done<T> resultDone = new Done<T>(result);
      boolean updated = ref.compareAndSet(this, resultDone);
      if (!updated) {
        throw new IllegalStateException();
      }
      synchronized (this) {
        this.resultReady = true;
        this.result = result;
      }
      for (CallbackPair<T> pair : callbacks) {
        if (pair.callback != null) {
          pair.callback.done(result);
        }
      }
    }

    private void resultIsReadySync(RuntimeException e) {
      // Double-check that result is marked ready.
      synchronized (this) {
        this.resultReady = true;
      }
      for (CallbackPair<T> pair : callbacks) {
        if (pair.syncCallback != null) {
          pair.syncCallback.callbackDone(e);
        }
      }
    }

    private static class CallbackPair<RES> {
      final Callback<? super RES> callback;
      final SyncCallback syncCallback;

      CallbackPair(Callback<? super RES> callback, SyncCallback syncCallback) {
        this.callback = callback;
        this.syncCallback = syncCallback;
      }
    }
  }

  private static class Done<T> extends AsyncFuture<T> {
    private final T result;

    public Done(T result) {
      this.result = result;
    }

    @Override
    public T getSync() {
      return result;
    }

    @Override
    public RelayOk getAsync(Callback<? super T> callback, SyncCallback syncCallback) {
      return deliverResultImmediately(result, callback, syncCallback);
    }

    @Override
    public boolean isDone() {
      return true;
    }
  }

  private static <T> RelayOk deliverResultImmediately(T result, Callback<T> callback,
      SyncCallback syncCallback) {
    if (callback != null) {
      callback.done(result);
    }
    return RelaySyncCallback.finish(syncCallback);
  }
}
