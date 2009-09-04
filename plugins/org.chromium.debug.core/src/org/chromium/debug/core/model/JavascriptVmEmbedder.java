// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.io.IOException;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.eclipse.core.runtime.CoreException;

/**
 * Abstraction of application embedding JavaScript VM. Technically subtypes
 * of {@code JavascriptVm} describe embedding application themselves.
 * This interface simply holds reference to {@code JavascriptVm} and adapts
 * various subtypes of {@code JavascriptVm} to a uniform interface
 * suitable for {@code DebugTargetImpl}. Notably, it has polymorphous method
 * {@code #attach(Listener, DebugEventListener)}, which {@code JavascriptVm}
 * lacks.
 */
public interface JavascriptVmEmbedder {

  /**
   * Intermediate object that works as a factory for
   * {@code JavascriptVmEmbedder}. When working it can interact with user
   * via UI.
   */
  interface Attachable {
    /**
     * This method may open a dialog window; e.g. suggesting choosing
     * a particular tab.
     * @return null if user chose to cancel operation
     */
    JavascriptVmEmbedder selectVm() throws CoreException;
  }

  /**
   * Note that this method returns {@code JavascriptVm} event before attach
   * is called.
   * @return not null
   */
  JavascriptVm getJavascriptVm();

  /**
   * @return true if successfully attached
   */
  boolean attach(Listener embedderListener, DebugEventListener debugEventListener)
      throws IOException;

  String getTargetName();

  String getThreadName();

  /**
   * Listener that should handle embedder-specific events.
   * TODO(peter.rybin): clean-up this interface; maybe decrease number of
   * methods.
   */
  interface Listener {
    /**
     * State of VM has been reset. All scripts might have been changed, name of
     * target and thread might have been changed. E.g. browser tab might have
     * been navigated from one url to another.
     */
    void reset();

    void closed();
  }
}
