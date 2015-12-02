// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

/**
 * An accessor to generated implementation of a v8 protocol parser.
 */
public class V8ProtocolParserAccess {

  private static final GeneratedV8ProtocolParser PARSER = new GeneratedV8ProtocolParser();

  public static V8NativeProtocolParser get() {
    return PARSER;
  }

}
