// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.Collection;

import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;

/**
 * A compound JsValue that has zero or more properties.
 */
public interface JsObject extends JsValue {

  /**
   * @return the class name of this object
   */
  String getClassName();

  /**
   * @return the properties of this compound value
   * @throws MethodIsBlockingException if called from a callback because it may
   *         need to load value from remote
   */
  Collection<? extends JsVariable> getProperties() throws MethodIsBlockingException;

  /**
   * @return the internal properties of this compound value (e.g. those properties which
   *         are not detectable with the "in" operator: __proto__ etc)
   * @throws MethodIsBlockingException if called from a callback because it may
   *         need to load value from remote
   */
  Collection<? extends JsVariable> getInternalProperties() throws MethodIsBlockingException;

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

  /**
   * Optionally returns unique id for this object. No two distinct objects can have the same id.
   * Lifetime of id may be limited by lifetime of {@link DebugContext}.
   * @return object id or null
   */
  String getRefId();
}
