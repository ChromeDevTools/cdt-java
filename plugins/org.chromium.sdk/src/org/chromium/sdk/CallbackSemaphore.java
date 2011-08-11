// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.internal.v8native.MethodIsBlockingException;

/**
 * Convenient implementation of {@code SyncCallback}. Client may create one,
 * then call asynchronous command, and finally wait on blocking method
 * {@code #tryAcquire()} or {@link #acquireDefault}.
 * <p>
 * Class uses symbolic parameter {@link RelayOk} in its methods that suggests that
 * user should first call some asynchronous method, then passe it to the acquire method.
 */
public class CallbackSemaphore implements SyncCallback {
  public static final long OPERATION_TIMEOUT_MS = 120000;

  private final Semaphore sem = new Semaphore(0);
  private Exception savedException;

  /**
   * Tries to acquire semaphore with some reasonable default timeout.
   * @param relayOk symbolic return value from the asynchronous operation that we are waiting for
   * @return false if {@code #OPERATION_TIMEOUT_MS} was exceeded and we gave up
   * @throws MethodIsBlockingException if called from a callback
   */
  public boolean tryAcquireDefault(RelayOk relayOk) throws MethodIsBlockingException {
    return tryAcquire(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  /**
   * @param relayOk symbolic return value from the asynchronous operation that we are waiting for
   */
  public void acquireDefault(RelayOk relayOk) throws MethodIsBlockingException {
    boolean res = tryAcquireDefault(relayOk);
    if (!res) {
      throw new RuntimeException("Failed to acquire semaphore (timeout)");
    }
  }

  /**
   * Tries to acquire the semaphore. This method blocks until the semaphore is
   * released; typically release call comes from a worker thread of
   * org.chromium.sdk, the same thread that may call all other callbacks.
   * It is vital not to call this method from any callback of org.chromium.sdk,
   * because it's a sure deadlock.
   * To prevent, this the method declares throwing
   * {@code MethodIsBlockingException} which is symbolically thrown whenever
   * someone violates this rule (i.e. invokes this method from a callback).
   * Though currently nobody actually throws it, such declarations help to
   * track blocking methods.
   * @return false if {@code timeout} was exceeded and we gave up
   * @throws MethodIsBlockingException if called from a callback
   */
  public boolean tryAcquire(long timeout, TimeUnit unit) throws MethodIsBlockingException {
    boolean res;
    try {
      res = sem.tryAcquire(timeout, unit);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (savedException != null) {
      throw new RuntimeException("Exception occured in callback", savedException);
    }
    return res;
  }
  /**
   * Implementation of {@code SyncCallback#callbackDone(RuntimeException)}.
   */
  @Override
  public void callbackDone(RuntimeException e) {
    if (e == null) {
      savedException = null;
    } else {
      savedException = new Exception("Exception saved from callback", e);
    }
    sem.release();
  }
}