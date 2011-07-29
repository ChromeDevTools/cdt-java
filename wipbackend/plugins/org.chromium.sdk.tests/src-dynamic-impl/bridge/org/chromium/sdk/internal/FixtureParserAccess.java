// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;

/**
 * An accessor to dynamic implementation of a fixture parser. Should be replaceable with a similar
 * class that provides access to generated parser implementation.
 */
public class FixtureParserAccess {
  public static JsonProtocolParser get() {
    return FixtureDynamicParser.get();
  }
}
