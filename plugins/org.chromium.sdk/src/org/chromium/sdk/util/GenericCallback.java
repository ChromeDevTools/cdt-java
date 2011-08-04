// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

/**
 * A generic callback used in operations that a remote variable value.
 */
public interface GenericCallback<T> {
  /**
   * Method is called after variable has been successfully updated.
   * @param value holds an actual new value of variable if provided or null
   */
  void success(T value);
  void failure(Exception exception);
}