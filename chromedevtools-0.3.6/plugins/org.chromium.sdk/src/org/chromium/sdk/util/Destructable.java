// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

/**
 * An interface to destruct some object. Used from {@link DestructingGuard}.
 */
public interface Destructable {
  /**
   * Destructs object wrapped in the interface. As usual exceptions are not
   * welcome from destruct method.
   */
  void destruct();
}
