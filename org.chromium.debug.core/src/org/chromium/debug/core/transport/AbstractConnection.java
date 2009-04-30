// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.chromium.debug.core.tools.ToolHandler;
import org.chromium.debug.core.tools.ToolName;
import org.eclipse.osgi.util.NLS;

/**
 * An abstract ToolHandler-aware base implementation of Connection.
 */
public abstract class AbstractConnection implements Connection {

  protected final Map<ToolName, ToolHandler> toolToHandlerMap =
      new HashMap<ToolName, ToolHandler>();

  @Override
  public void removeToolHandler(ToolName toolName) {
    if (toolToHandlerMap.remove(toolName) == null) {
      throw new IllegalStateException(NLS.bind(
          "Tool for {0} not registered", toolName)); //$NON-NLS-1$
    }
  }

  @Override
  public void setToolHandler(ToolName toolName, ToolHandler handler) {
    if (toolToHandlerMap.put(toolName, handler) != null) {
      throw new IllegalStateException(NLS.bind(
          "Tool for {0} already registered", toolName)); //$NON-NLS-1$
    }
  }

  @Override
  public Iterator<ToolHandler> toolHandlersIterator() {
    return toolToHandlerMap.values().iterator();
  }
}
