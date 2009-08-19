// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.processor;

import org.chromium.sdk.internal.DebugContextImpl;
import org.json.simple.JSONObject;

/**
 * An abstract base implementation of DebugContextImpl-aware
 * reply handlers for certain V8 commands.
 * <p>
 * NB! The {@link #messageReceived(org.json.simple.JSONObject)} implementation
 * MUST NOT perform debugger commands in a blocking way the current thread.
 */
public abstract class V8ResponseCallback {

  private final DebugContextImpl context;

  public V8ResponseCallback(DebugContextImpl context) {
    this.context = context;
  }

  public abstract void messageReceived(JSONObject response);

  protected DebugContextImpl getDebugContext() {
    return context;
  }
}
