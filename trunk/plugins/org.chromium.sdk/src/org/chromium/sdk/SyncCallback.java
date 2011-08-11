// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Secondary callback that should be called after main callback has been
 * called; it gets called regardless of whether main callback  finished
 * normally or thrown an exception.
 * It helps to separate callback logic (which may fail) from multi-thread
 * synchronization (which shouldn't fail). Typically client may release
 * his semaphore in this callback.
 * <p>
 * It could also be called 'finally callback', implying that is resembles
 * try-finally control flow.
 */
public interface SyncCallback {
  /**
   * @param e an exception main callback raised or null if none is reported
   */
  void callbackDone(RuntimeException e);
}
