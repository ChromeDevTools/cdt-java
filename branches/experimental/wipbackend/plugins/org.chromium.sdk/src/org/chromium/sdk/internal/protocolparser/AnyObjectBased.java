// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import org.json.simple.JSONArray;

/**
 * Optional base interface for JSON type interface. Underlying object becomes available
 * to user this way. The JSON type instance may be created from any supported object
 * (e.g. from {@link JSONArray}), but may take advantage of this liberty only if it has no fields.
 */
public interface AnyObjectBased {
  Object getUnderlyingObject();
}
