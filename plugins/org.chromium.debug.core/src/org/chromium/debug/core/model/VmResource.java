// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.Script;

/**
 * A representation of V8 VM resource. The exact nature of the resource is unspecified, we
 * only know it may contain one or more {@link Script}s. Typically resource is .js or .html file.
 */

public interface VmResource {
  VmResourceId getId();

  /**
   * @return script if this resource entirely consists of 1 script, otherwise throws exception
   * @throws UnsupportedOperationException if this resource does not entirely consists of 1 script
   * TODO(peter.rybin): redesign this method to normally work with html resources.
   */
  Script getScript();

  String getFileName();
}
