// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.DebugEventListener;

/**
 * Object that manages debug session as it's represented to V8 core debugging
 * classes.
 */
public interface DebugSessionManager {

  /**
   * Listener is kept by session manager.
   */
  DebugEventListener getDebugEventListener();

  /**
   * Debugger detached event goes through {@code DebugContextImpl},
   * and {@code DebugContextImpl} should notify upwards via this method.
   */
  void onDebuggerDetached();
}
