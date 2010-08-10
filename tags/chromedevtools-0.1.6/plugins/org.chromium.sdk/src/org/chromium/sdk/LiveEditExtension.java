// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Helper class for experimental support of LiveEdit feature. While regular API does not
 * support LiveEdit (not to break compatibility with existing clients), it gives access to extended
 * interfaces.
 * <p>
 * This class encapsulates all instanceofs/casts that are considered to be untrackable
 * in the main code and therefore harmful.
 */
public class LiveEditExtension {
  /**
   * Casts script to the interface that supports updating source on remote VM.
   * @return extended interface or null if unsupported
   */
  public static UpdatableScript castToUpdatableScript(Script script) {
    if (script instanceof UpdatableScript == false) {
      return null;
    }
    return (UpdatableScript) script;
  }

  /**
   * Casts listener to interface that accepts LiveEdit-related events.
   * @return extended interface or null if unsupported
   */
  public static LiveEditDebugEventListener castToLiveEditListener(
      DebugEventListener debugEventListener) {
    if (debugEventListener instanceof LiveEditDebugEventListener == false) {
      return null;
    }
    return (LiveEditDebugEventListener) debugEventListener;
  }
}
