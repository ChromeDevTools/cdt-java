// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
