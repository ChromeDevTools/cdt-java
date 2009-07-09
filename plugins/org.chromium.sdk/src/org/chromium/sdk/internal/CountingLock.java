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

  public synchronized void lock() {
    ++lockedCount;
  }

  public synchronized void unlock() {
    --lockedCount;
    if (lockedCount == 0) {
      notifyAll();
    } else if (lockedCount < 0) {
      throw new IllegalStateException("unlock() without lock()");
    }
  }

  public synchronized void await() {
    while (lockedCount > 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        // Consider it an unlocking event.
        break;
      }
    }
  }
}
