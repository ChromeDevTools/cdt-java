// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

/**
 * This kind of lock counts the number of locking and unlocking operations
 * and allows synchronization on complete unlocking.
 */
public class CountingLock {
  private int lockedCount = 0;

  private final Object lockObject = new Object();

  public void lock() {
    synchronized (lockObject) {
      ++lockedCount;
    }
  }

  public void unlock() {
    synchronized (lockObject) {
      --lockedCount;
      if (lockedCount == 0) {
        lockObject.notifyAll();
      } else if (lockedCount < 0) {
        throw new IllegalStateException("unlock() without lock()");
      }
    }
  }

  public void await() {
    synchronized (lockObject) {
      while (lockedCount > 0) {
        try {
          lockObject.wait();
        } catch (InterruptedException e) {
          // Consider it an unlocking event.
          break;
        }
      }
    }
  }
}
