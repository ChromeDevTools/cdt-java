// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
