// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that destructs unfinished objects. It is needed when Java GC is not enough.
 * It requires to be explicitly discharged if all went OK and destruction should be cancelled.
 * Using this class may be more convenient that try/finally in Java.
 */
public class DestructingGuard {
  /**
   * Confirms that constructing has finished OKAY and no destruction is needed from now.
   */
  public void discharge() {
    discharged = true;
  }

  /**
   * This method is supposed to be called from finally clause. It performs destructing
   * unless {@link #discharge()} has been called.
   */
  public void doFinally() {
    if (discharged) {
      return;
    }
    for (int i = destructables.size() - 1; i >= 0; i--) {
      destructables.get(i).destruct();
    }
    discharged = true;
  }

  /**
   * Adds another value that should be destructed. Added values are destructed in reversed order.
   */
  public void addValue(Destructable destructable) {
    this.destructables.add(destructable);
  }

  private List<Destructable> destructables = new ArrayList<Destructable>(1);
  private boolean discharged = false;
}
