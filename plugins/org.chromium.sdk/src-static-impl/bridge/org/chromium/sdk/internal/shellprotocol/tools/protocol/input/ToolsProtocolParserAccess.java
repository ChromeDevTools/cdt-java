// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.shellprotocol.tools.protocol.input;

/**
 * An accessor to generated implementation of DevTools protocol parser.
 */
public class ToolsProtocolParserAccess {
  public static ToolsProtocolParser get() {
    return INSTANCE;
  }

  private static final GeneratedToolsProtocolParser INSTANCE = new GeneratedToolsProtocolParser();
}
