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
   * First intermediate object that corresponds to already connected server.
   * This does not refer to a particular Javascript VM though:
   * the server may contain several VMs to choose from.
   */
  interface ConnectionToRemote {
    /**
     * This method performs selecting a particular Javascript VM. This is
     * likely to be a user-assisted activity, so this method may block
     * indefinitely.
     * @return null if no VM has been chosen and we should cancel the operation
     */
    VmConnector selectVm() throws CoreException;

    void disposeConnection();
  }

  /**
   * Intermediate object that works as an intermediate factory
   * for {@code JavascriptVmEmbedder}.
   */
  interface VmConnector {
    JavascriptVmEmbedder attach(Listener embedderListener, DebugEventListener debugEventListener)
        throws IOException;
  }

  /**
   * @return not null
   */
  JavascriptVm getJavascriptVm();

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
