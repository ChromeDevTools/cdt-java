// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Adapts a certain type of JavaScipt breakpoints by wrapping them as {@link WrappedBreakpoint}.
 * This abstraction allows working with JavaScript breakpoints from any provider
 * (e.g. of JSDT as well as of org.chromium.debug.*), though it doesn't support their creation.
 * <p>
 * A particular implementation of adapter should be registered as an extension.
 */
public interface JavaScriptBreakpointAdapter {
  String EXTENSION_POINT_ID = "org.chromium.debug.core.model.JavaScriptBreakpointAdapter";

  WrappedBreakpoint tryWrapBreakpoint(IBreakpoint breakpoint);

  String getModelId();
}
