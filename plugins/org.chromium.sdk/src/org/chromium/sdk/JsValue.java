// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An object that represents a browser JavaScript VM variable value (compound or
 * atomic.)
 */
public interface JsValue {

  /**
   * @return this value type
   */
  JsDataType getReferenceType();

  /**
   * @return a string representation of this value
   */
  String getValueString();

  /**
   * @return this value cast to {@link JsObject} or {@code null} if this value
   *         is not an object
   */
  JsObject asObject();

}
