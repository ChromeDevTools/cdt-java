// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.GenericCallback;

/**
 * A {@link RemoteValueMapping} that outlives suspend/resume cycle of debugger.
 * It represents both remote pointer table and local property caches.
 * The remote table should be explicitly deleted when the mapping is no longer used.
 * The table has a unique id.
 */
public interface PermanentRemoteValueMapping extends RemoteValueMapping {
  String getId();

  void delete(GenericCallback<Void> callback, SyncCallback syncCallback);
}
