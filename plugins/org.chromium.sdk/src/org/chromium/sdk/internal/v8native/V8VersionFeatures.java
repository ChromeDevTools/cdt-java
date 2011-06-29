// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.Version;

/**
 * Stores milestone version numbers that marks when a particular feature was implemented.
 */
public class V8VersionFeatures {
  private final static Version ACCURATE_RUNNING_FIELD = new Version(1, 3, 16);

  public static boolean isRunningAccurate(Version vmVersion) {
    return vmVersion != null && ACCURATE_RUNNING_FIELD.compareTo(vmVersion) <= 0;
  }

  private final static Version EVALUATE_WITH_CONTEXT = new Version(3, 0, 0, 1);

  public static boolean isEvaluateWithContextSupported(Version vmVersion) {
    return vmVersion != null && EVALUATE_WITH_CONTEXT.compareTo(vmVersion) < 0;
  }

  private final static Version REG_EXP_BREAKPOINT = new Version(3, 4, 7);

  public static boolean isRegExpBreakpointSupported(Version vmVersion) {
    return vmVersion != null && REG_EXP_BREAKPOINT.compareTo(vmVersion) <= 0;
  }
}
