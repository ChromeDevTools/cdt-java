// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Convenient implementation of {@code SyncCallback}. Client may create one,
 * then call asynchronous command, and finally wait on blocking method
 * {@code #tryAcquire()}.
 */
public class CallbackSemaphore implements SyncCallback {
  private static final long OPERATION_TIMEOUT_MS = 120000;

  private final Semaphore sem = new Semaphore(0);
  private Exception savedException;

  /**
   * Tries to acquire semaphore with some reasonable default timeout.
   * @return false if {@code #OPERATION_TIMEOUT_MS} was exceeded and we gave up
   */
  public boolean tryAcquireDefault() {
    return tryAcquire(OPERATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
  }

  /**
   * Tries to acquire semaphore.
   * @return false if {@code timeout} was exceeded and we gave up
   */
  public boolean tryAcquire(long timeout, TimeUnit unit) {
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
  public void callbackDone(RuntimeException e) {
    if (e == null) {
      savedException = null;
    } else {
      savedException = new Exception("Exception saved from callback");
    }
    sem.release();
  }
}