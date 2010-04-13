// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * This interface is a part of {@link Script} interface. It extends {@link Script} in order
 * to support experimental feature and is under development.
 */
public interface UpdatableScript extends Script {
  /**
   * Demands that script text should be replaced with a new one if possible.
   * @param newSource new text of script
   */
  void setSourceOnRemote(String newSource, UpdateCallback callback, SyncCallback syncCallback);

  interface UpdateCallback {
    /**
     * Script text has been successfully changed. {@link DebugEventListener#scriptChanged} will
     * be called additionally. Besides, a current context may be dismissed and recreated after this
     * event. The order of all listed event notifications is not currently specified.
     */
    void success(Object report);
    void failure(String message);
  }
}
