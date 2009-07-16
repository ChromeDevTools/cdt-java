// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A token that denotes a debug context validity.
 */
public class ContextToken {

  private static final AtomicInteger GLOBAL_ID = new AtomicInteger(0);

  private final int id = GLOBAL_ID.incrementAndGet();

  private volatile boolean isValid = true;

  public void invalidate() {
    isValid = false;
  }

  public boolean isValid() {
    return isValid;
  }

  public int getId() {
    return id;
  }
}
