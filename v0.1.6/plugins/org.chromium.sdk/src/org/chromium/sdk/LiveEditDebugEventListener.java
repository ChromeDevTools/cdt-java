// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An optional extension of {@link DebugEventListener} that supports experimental LiveEdit-related
 * events.
 */
public interface LiveEditDebugEventListener extends DebugEventListener {
  /**
   * Reports that script source has been altered in remote VM.
   */
  void scriptContentChanged(UpdatableScript newScript);
}
