// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Version;

/**
 * Stores milestone version numbers that marks when a particular feature was implemented.
 */
public class V8VersionMilestones {
  private final static Version ACCURATE_RUNNING_FIELD = new Version(1, 3, 16);

  public static boolean isRunningAccurate(Version vmVersion) {
    return vmVersion != null && ACCURATE_RUNNING_FIELD.compareTo(vmVersion) <= 0;
  }
}
