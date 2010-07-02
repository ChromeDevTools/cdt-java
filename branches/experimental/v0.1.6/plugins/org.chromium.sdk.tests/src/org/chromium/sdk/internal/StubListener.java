// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.Script;
import org.chromium.sdk.TabDebugEventListener;

// TODO(peter.rybin): get rid of semaphore once we are single-threaded
class StubListener implements DebugEventListener, TabDebugEventListener {
  private DebugContext debugContext = null;
  private Semaphore semaphore;

  public void closed() {
  }

  public void disconnected() {
  }

  public DebugEventListener getDebugEventListener() {
    return this;
  }

  public void navigated(String newUrl) {
  }

  public void resumed() {
    debugContext = null;
  }

  public void suspended(DebugContext context) {
    debugContext = context;
    if (semaphore != null) {
      semaphore.release();
    }
  }

  public void scriptLoaded(Script newScript) {
  }

  public void scriptCollected(Script script) {
  }

  void expectSuspendedEvent() {
    if (semaphore != null) {
      throw new IllegalStateException();
    }
    semaphore = new Semaphore(0);
  }

  DebugContext getDebugContext() {
    boolean res;
    try {
      res = semaphore.tryAcquire(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (!res) {
      throw new RuntimeException();
    }
    semaphore = null;
    if (debugContext == null) {
      throw new IllegalStateException();
    }
    return debugContext;
  }
}