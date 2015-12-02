// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
