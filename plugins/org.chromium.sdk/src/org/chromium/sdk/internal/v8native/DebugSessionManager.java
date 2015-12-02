// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.DebugEventListener;

/**
 * Type that manages debug session as it's represented to V8 core debugging
 * classes. Basically it's an internal interface of JavascriptVm object.
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
