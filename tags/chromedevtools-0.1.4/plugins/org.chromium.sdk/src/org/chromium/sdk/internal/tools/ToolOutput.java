// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools;

import org.chromium.sdk.internal.transport.Connection;

/**
 * Tool handler sends out its messages via this interface. This way tool handler does not
 * need to access {@link Connection}. Instance of {@link ToolOutput} should add a proper
 * header (with tool name and destination fields).
 * This interface is used by a tool handler implementation.
 */
public interface ToolOutput {
  /**
   * Sends text message to a remote tool.
   * @param content of message (without header)
   */
  void send(String content);
}
