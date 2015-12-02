// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
