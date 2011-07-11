// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import java.util.List;

import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.internal.v8native.InternalContext;

/**
 * The elaborate factory and storage for {@link ValueMirror}'s, that loads values from remote and
 * caches them. The data may come from server with more
 * or less fields, so it creates {@link ValueMirror} accordingly.
 * {@link ValueMirror} is an immutable wrapper around JSON data. Several data instances
 * may occur, the map should always hold the fullest (and the less expired) version.
 */
public abstract class ValueLoader implements RemoteValueMapping {

  /**
   * @return internal cache state variable value
   */
  abstract int getCurrentCacheState();

  /**
   * Looks up {@link ValueMirror} in map, loads them if needed or reloads them
   * if property data is unavailable (or expired).
   */
  public abstract SubpropertiesMirror getOrLoadSubproperties(Long ref);

  /**
   * For each PropertyReference from propertyRefs tries to either: 1. read it from PropertyReference
   * (possibly cached value) or 2. lookup value by refId from remote
   */
  public abstract List<ValueMirror> getOrLoadValueFromRefs(
      List<? extends PropertyReference> propertyRefs);

  public abstract InternalContext getInternalContext();
}
