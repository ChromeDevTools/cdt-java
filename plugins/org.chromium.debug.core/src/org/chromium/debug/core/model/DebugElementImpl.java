// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;

/**
 * A generic IDebugElement implementation.
 */
public class DebugElementImpl extends PlatformObject implements IDebugElement {

  private final DebugTargetImpl debugTarget;

  public DebugElementImpl(DebugTargetImpl debugTarget) {
    this.debugTarget = debugTarget;
  }

  public DebugTargetImpl getDebugTarget() {
    return debugTarget;
  }

  public ILaunch getLaunch() {
    return getDebugTarget().getLaunch();
  }

  public String getModelIdentifier() {
    return ChromiumDebugPlugin.DEBUG_MODEL_ID;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (adapter == IDebugElement.class) {
      return this;
    }
    return super.getAdapter(adapter);
  }

}
