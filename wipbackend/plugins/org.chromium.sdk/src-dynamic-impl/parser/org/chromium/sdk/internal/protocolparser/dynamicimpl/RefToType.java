// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

/**
 * Late-resolvable reference to {@link TypeHandler}, for building {@link JsonTypeParser}.
 */
abstract class RefToType<T> {
  /**
   * Returns json type.
   */
  abstract Class<?> getTypeClass();

  /**
   * Returns {@link TypeHandler} corresponding to {@link #getTypeClass()}. The method becomes
   * available only after cross-reference resolving has been finished in depths of
   * {@link DynamicParserImpl} constructor.
   */
  abstract TypeHandler<T> get();
}
