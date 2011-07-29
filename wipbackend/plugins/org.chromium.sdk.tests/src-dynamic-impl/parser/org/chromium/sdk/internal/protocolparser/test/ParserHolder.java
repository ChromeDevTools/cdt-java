// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.test;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;

/**
 * The class holding a parser and suitable for static field (it saves initialization problem).
 * There is no reason to create new instance of parser for every test. On the other hand,
 * it there were a problem with it, every test should get a proper exception.
 */
class ParserHolder {
  private final InitializedValue<DynamicParserImpl> parser;

  ParserHolder(final Class<?>[] interfaces) {
    InitializedValue.Initializer<DynamicParserImpl> initializer =
        new InitializedValue.Initializer<DynamicParserImpl>() {
      public DynamicParserImpl calculate() {
        try {
          return new DynamicParserImpl(interfaces);
        } catch (JsonProtocolModelParseException e) {
          throw new RuntimeException(e);
        }
      }
    };
    parser = new InitializedValue<DynamicParserImpl>(initializer);
  }

  DynamicParserImpl getParser() {
    return parser.get();
  }
}