// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.internal.browserfixture.FixtureChromeStub.FixtureParser;

/**
 * An accessor to generated implementation of a fixture parser.
 */
public class FixtureParserAccess {

  public static FixtureParser get() {
    return PARSER;
  }

  private static final GeneratedV8FixtureParser PARSER = new GeneratedV8FixtureParser();
}
