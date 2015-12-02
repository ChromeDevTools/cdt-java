// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.wip;

import org.chromium.sdk.internal.wip.WipBackendImpl;

public class WipBackendFactory implements WipBackend.Factory {
  @Override public WipBackend create() {
    return new WipBackendImpl();
  }
}
