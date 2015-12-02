// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol;

import org.chromium.sdk.internal.wip.protocol.input.WipProtocolParser;

/**
 * An accessor to dynamic implementation of a WebInspector protocol parser.
 * Should be replaceable with a similar class that provides access to generated
 * parser implementation.
 */
public class WipParserAccess {
  public static WipProtocolParser get() {
    return PARSER;
  }

  private static final WipProtocolParser PARSER = WipDynamicParser.create().getParserRoot();
}
