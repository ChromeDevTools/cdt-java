// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Represents a technical scope that defines remote value representation in debugger.
 * This scope controls life-cycle of internal value tables and caching strategies.
 * TODO: add methods that describe mapping life-cycle, methods that creates new mapping
 *     instances.
 */
public interface RemoteValueMapping {
  void clearCaches();
}
