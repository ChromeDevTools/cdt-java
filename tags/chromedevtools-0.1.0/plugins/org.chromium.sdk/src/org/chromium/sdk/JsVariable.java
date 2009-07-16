// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An object that represents a variable in a browser JavaScript VM call frame.
 */
public interface JsVariable {

  /**
   * A callback to use while setting a variable value.
   */
  interface SetValueCallback {
    void success();

    void failure(String errorMessage);
  }

  /**
   * @return whether it is possible to read this variable
   */
  boolean isReadable();

  /**
   * Returns the value of this variable.
   *
   * @return a [probably compound] JsValue corresponding to this variable.
   *         {@code null} if there was an error reading the value data
   * @see #isReadable()
   * @throws UnsupportedOperationException if this variable is not readable
   */
  JsValue getValue() throws UnsupportedOperationException;

  /**
   * @return the name of this variable
   */
  String getName();

  /**
   * @return whether it is possible to modify this variable
   */
  boolean isMutable();

  /**
   * Sets a new value for this variable.
   *
   * @param newValue to set
   * @param callback to report the operation result to
   * @see #isMutable()
   * @throws UnsupportedOperationException if this variable is not mutable
   */
  void setValue(String newValue, SetValueCallback callback)
      throws UnsupportedOperationException;

  /**
   * @return the fully qualified name of this variable relative to the context
   *         of its call frame
   */
  String getFullyQualifiedName();

}
