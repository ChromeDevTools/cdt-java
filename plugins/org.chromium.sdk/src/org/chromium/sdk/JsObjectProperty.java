// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * Exposes additional data if variable is a property of object and its property descriptor
 * is available.
 */
public interface JsObjectProperty extends JsVariable {
  /**
   * @return whether property described as 'writable'
   */
  boolean isWritable();

  /**
   * @return property getter value (function or undefined) or null if not an accessor property
   */
  JsValue getGetter();

  /**
   * @return {@link #getGetter()} result as function or null if cannot cast
   */
  JsFunction getGetterAsFunction();

  /**
   * @return property setter value (function or undefined) or null if not an accessor property
   */
  JsValue getSetter();

  /**
   * @return whether property described as 'configurable'
   */
  boolean isConfigurable();

  /**
   * @return whether property described as 'enumerable'
   */
  boolean isEnumerable();

  /**
   * Asynchronously evaluates property getter and returns property value. Must only be used
   * if {@link #getGetterAsFunction()} returns not null; otherwise behavior is undefined and
   * implementation-specific.
   */
  RelayOk evaluateGet(JsEvaluateContext.EvaluateCallback callback, SyncCallback syncCallback);
}
