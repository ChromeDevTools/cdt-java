// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.V8DebuggerToolHandler;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;

/**
 * A generic IDebugElement implementation.
 */
public class DebugElementImpl extends PlatformObject implements IDebugElement {

  private V8DebuggerToolHandler handler;

  public DebugElementImpl(V8DebuggerToolHandler handler) {
    this.handler = handler;
  }

  public V8DebuggerToolHandler getHandler() {
    return handler;
  }

  @Override
  public DebugTargetImpl getDebugTarget() {
    return handler.getDebugTarget();
  }

  @Override
  public ILaunch getLaunch() {
    return getDebugTarget().getLaunch();
  }

  @Override
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
