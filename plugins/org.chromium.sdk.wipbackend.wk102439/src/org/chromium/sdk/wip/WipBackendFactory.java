// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import org.chromium.sdk.internal.wip.WipBackendImpl;

public class WipBackendFactory implements WipBackend.Factory {
  @Override public WipBackend create() {
    return new WipBackendImpl();
  }
}
