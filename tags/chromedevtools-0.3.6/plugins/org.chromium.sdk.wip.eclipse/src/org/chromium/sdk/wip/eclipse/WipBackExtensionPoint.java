// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip.eclipse;

import org.chromium.sdk.wip.WipBackend;

/**
 * Holds several constants that defines extension point used to fetch {@link WipBackend} instances
 * in Eclipse.
 */
public interface WipBackExtensionPoint {
  String ID = "org.chromium.sdk.wip.eclipse.WipBacked";
  String ELEMENT_NAME = "backend";
  String CLASS_PROPERTY = "class";
}
