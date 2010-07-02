// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Optional base interface for JSON type interface. Underlying JSON object becomes available
 * to user this way. The JSON type instance may be created from {@link JSONObject} only
 * (not from {@link JSONArray} or whatever).
 */
public interface JsonObjectBased extends AnyObjectBased {
  JSONObject getUnderlyingObject();
}
