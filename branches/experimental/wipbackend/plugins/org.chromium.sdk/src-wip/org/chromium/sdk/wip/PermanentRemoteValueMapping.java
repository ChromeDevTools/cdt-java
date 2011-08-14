// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.RelayOk;
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

  /**
   * Asynchronously deletes mapping on remote VM. No values from this {@link RemoteValueMapping}
   * must be used after this call.
   */
  RelayOk delete(GenericCallback<Void> callback, SyncCallback syncCallback);

  /**
   * Returns {@link JsEvaluateContext} that is tied with this {@link RemoteValueMapping}.
   * By default all evaluate result values will be using this {@link RemoteValueMapping}.
   * (This can be overriden by {@link EvaluateToMappingExtension}).
   */
  JsEvaluateContext getEvaluateContext();
}
