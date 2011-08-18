// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

/**
 * A generic callback used in asynchronous operations that either fail with exception
 * or return a result.
 */
public interface GenericCallback<T> {
  void success(T value);
  void failure(Exception exception);
}