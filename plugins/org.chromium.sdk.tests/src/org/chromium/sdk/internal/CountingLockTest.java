// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

/**
 * A test for the CountingLock class.
 */
public class CountingLockTest {

  private final CountDownLatch sync = new CountDownLatch(3);

  private class LockingThread extends Thread {

    LockingThread() {
      setDaemon(true);
    }

    @Override
    public void run() {
      lock.lock();
    }
  }

  private class UnlockingThread extends Thread {

    UnlockingThread() {
      setDaemon(true);
    }

    @Override
    public void run() {
      lock.unlock();
      sync.countDown();
    }
  }

  private CountingLock lock;

  @Before
  public void setUpClass() {
    this.lock = new CountingLock();
  }

  @Test(timeout = 5000)
  public void testConcurrentLocking() {
    startLockingThread();
    Thread awaitingThread = new Thread(new Runnable() {
      public void run() {
        lock.await();
      }
    });
    awaitingThread.setDaemon(true);
    awaitingThread.start();
    startLockingThread();
    startLockingThread();
    startUnlockingThread();
    startUnlockingThread();
    startUnlockingThread();
    lock.await();
    try {
      awaitingThread.join();
    } catch (InterruptedException e) {
      fail();
    }
  }

  private void startLockingThread() {
    new LockingThread().start();
  }

  private void startUnlockingThread() {
    new UnlockingThread().start();
  }
}
