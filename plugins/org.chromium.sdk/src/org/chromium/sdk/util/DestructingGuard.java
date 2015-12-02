// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class that destructs unfinished objects. It is needed when Java GC is not enough.
 * It requires to be explicitly discharged if all went OK and destruction should be cancelled.
 * Using this class may be more convenient that try/finally in Java.
 * <p>User may subclass this class to override exception logging in handleFinallyProblem
 * methods.
 */
public class DestructingGuard {
  private static final Logger LOGGER = Logger.getLogger(DestructingGuard.class.getName());

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
      try {
        destructables.get(i).destruct();
      } catch (RuntimeException e) {
        handleFinallyProblem(e);
      } catch (Error e) {
        handleFinallyProblem(e);
      }
    }
    discharged = true;
  }

  /**
   * Adds another value that should be destructed. Added values are destructed in reversed order.
   */
  public void addValue(Destructable destructable) {
    this.destructables.add(destructable);
  }

  protected void handleFinallyProblem(RuntimeException e) {
    LOGGER.log(Level.SEVERE, "Exception in finally handler", e);
  }

  protected void handleFinallyProblem(Error e) {
    LOGGER.log(Level.SEVERE, "Exception in finally handler", e);
  }

  private List<Destructable> destructables = new ArrayList<Destructable>(1);
  private boolean discharged = false;
}
