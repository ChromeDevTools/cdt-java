// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

/**
 * An accessor to dynamic implementation of a v8 protocol parser. Should be replaceable with
 * a similar class that provides access to generated parser implementation.
 */
public class V8ProtocolParserAccess {
  public static V8NativeProtocolParser get() {
    return PARSER;
  }

  private static final V8NativeProtocolParser PARSER = V8DynamicParser.create().getParserRoot();
}
