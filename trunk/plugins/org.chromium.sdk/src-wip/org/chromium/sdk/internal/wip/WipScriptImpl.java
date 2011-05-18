// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptBase;

/**
 * Wip implementation of {@link Script}.
 */
class WipScriptImpl extends ScriptBase {
  WipScriptImpl(Descriptor descriptor) {
    super(descriptor);
  }

  @Override
  public void setSourceOnRemote(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    WipBrowserImpl.throwUnsupported();
  }

  @Override
  public void previewSetSource(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    WipBrowserImpl.throwUnsupported();
  }
}
