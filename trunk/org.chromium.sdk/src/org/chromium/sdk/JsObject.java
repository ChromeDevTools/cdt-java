// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A compound JsValue that has zero or more properties.
 */
public interface JsObject extends JsValue {

  /**
   * @return the properties of this compound value
   */
  JsVariable[] getProperties();

  /**
   * @param name of the property to get
   * @return the property object or {@code null} if {@code name} does not
   *         designate an existing object property
   */
  JsVariable getProperty(String name);

  /**
   * @return this object cast to {@link JsArray} or {@code null} if this object
   *         is not an array
   */
  JsArray asArray();
}
