// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

/**
 * A source path computer implementation that provides {@link VProjectSourceContainer} as
 * a default source files container for V8/Chrome debug sessions.
 */
public class ChromiumSourceComputer implements ISourcePathComputerDelegate {
  public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration,
      IProgressMonitor monitor) throws CoreException {
    return new ISourceContainer[] { new VProjectSourceContainer() };
  }
}
