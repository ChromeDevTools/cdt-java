// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.io.IOException;
import java.util.Iterator;

import org.chromium.debug.core.tools.ToolHandler;
import org.chromium.debug.core.tools.ToolName;

/**
 * An interface to be implemented by an agent performing the communications
 * with the debugged instance.
 */
public interface Connection {

  /**
   * Associates the handler with the toolName.
   */
  void setToolHandler(ToolName toolName, ToolHandler handler);

  /**
   * Removes a handler for the given toolName.
   */
  void removeToolHandler(ToolName toolName);

  /**
   * @return an iterator for all known ToolHandlers
   */
  Iterator<ToolHandler> toolHandlersIterator();

  /**
   * Sends the specified message over the wire.
   */
  void send(Message message);

  /**
   * @return the exception which caused the transport shutdown, or null if none
   */
  Exception getTerminatedException();

  /**
   * Start up the transport and acquire all needed resources.
   *
   * @throws IOException
   */
  void startup() throws IOException;

  /**
   * Shut down the transport freeing all acquired resources.
   *
   * @param lameduckMode
   *          whether to enter lameduck mode and shutdown after a timeout
   *          (otherwise shutdown immediately)
   */
  void shutdown(boolean lameduckMode);
}
