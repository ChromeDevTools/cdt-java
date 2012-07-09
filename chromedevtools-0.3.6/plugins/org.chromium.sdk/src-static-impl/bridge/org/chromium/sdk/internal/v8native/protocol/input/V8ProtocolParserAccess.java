// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
