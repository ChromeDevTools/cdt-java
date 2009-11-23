// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import org.json.simple.JSONObject;

/**
 * A stub interface for parser.
 */
public interface TheParser {
  <T> T parse(JSONObject jsonObject, Class<T> type);
  <T> T parseAnything(Object object, Class<T> type);
}
