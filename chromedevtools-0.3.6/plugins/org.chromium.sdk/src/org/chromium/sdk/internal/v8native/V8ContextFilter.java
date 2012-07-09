// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.internal.v8native.protocol.input.data.ContextHandle;

/**
 * Embedder-specific filter for V8 VM contexts.
 */
public interface V8ContextFilter {
  /**
   * Given a context handler, it should check whether it is our context or not.
   * The field {@link ContextHandle#data()} of embedder-specific type should be used.
   * @return whether the context is ours
   */
  boolean isContextOurs(ContextHandle contextHandle);
}
