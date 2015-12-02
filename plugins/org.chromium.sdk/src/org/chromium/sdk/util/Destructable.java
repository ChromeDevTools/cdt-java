// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
