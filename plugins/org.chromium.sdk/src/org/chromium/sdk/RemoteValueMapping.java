// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

/**
 * Represents a technical scope that manages remote value representation in debugger.
 * It controls life-cycle of {@link JsObject}s and their caching strategies.
 * @see JsObject#getRemoteValueMapping()
 * @see DebugContext#getDefaultRemoteValueMapping()
 */
// TODO: add methods that describe mapping life-cycle and its other properties.
public interface RemoteValueMapping {

  /**
   * Clears local caches that store object properties. This method should be called if
   * property value has been changed in VM (for example as an expression side-effect).
   */
  void clearCaches();

}
