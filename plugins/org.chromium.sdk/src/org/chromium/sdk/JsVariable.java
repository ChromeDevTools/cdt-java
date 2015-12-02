// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

/**
 * An object that represents a variable in a browser JavaScript VM, a call frame
 * variable and/or an object property.
 */
public interface JsVariable {
  /**
   * Returns the value of this variable.
   *
   * @return a [probably compound] JsValue corresponding to this variable.
   *         {@code null} if there was an error reading the value data
   *         or the property has accessor descriptor
   * @see #isReadable()
   * @throws UnsupportedOperationException if this variable is not readable
   */
  JsValue getValue() throws UnsupportedOperationException;

  /**
   * Returns variable name. If the variable is an object property, in some implementations
   * (namely V8 Standalone protocol) the numeric property name may be decorated
   * with square brackets.
   * @return the name of this variable
   */
  String getName();

  /**
   * Returns object property data if variable is an object property and its descriptor
   * is available.
   */
  JsObjectProperty asObjectProperty();

  /**
   * Casts this to declarative variable type if available or returns null.
   */
  JsDeclarativeVariable asDeclarativeVariable();
}
