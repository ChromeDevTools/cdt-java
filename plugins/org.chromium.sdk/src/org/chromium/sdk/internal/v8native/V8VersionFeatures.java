// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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

  private final static Version REG_EXP_BREAKPOINT = new Version(3, 4, 7);

  public static boolean isRegExpBreakpointSupported(Version vmVersion) {
    return vmVersion != null && REG_EXP_BREAKPOINT.compareTo(vmVersion) <= 0;
  }

  private final static Version FUNCTION_SCOPE = new Version(3, 10, 7);

  public static boolean isFunctionScopeSupported(Version vmVersion) {
    return vmVersion != null && FUNCTION_SCOPE.compareTo(vmVersion) <= 0;
  }

  private final static Version RESTART_FRAME = new Version(3, 12, 0);

  public static boolean isRestartFrameSupported(Version vmVersion) {
    return vmVersion != null && RESTART_FRAME.compareTo(vmVersion) <= 0;
  }
}
