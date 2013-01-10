// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An object that represents a variable in a browser JavaScript VM call frame.
 */
public interface JsVariable {

  /**
   * @return whether it is possible to read this variable
   */
  boolean isReadable();

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
   * TODO: do not decorate property name with square brackets,
   *       http://code.google.com/p/chromedevtools/issues/detail?id=77
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
   * @param syncCallback to report the end of any processing
   * @see #isMutable()
   * @throws UnsupportedOperationException if this variable is not mutable
   */
  RelayOk setValue(JsValue newValue, SetValueCallback callback, SyncCallback syncCallback)
      throws UnsupportedOperationException;

  /**
   * A callback to use while setting a variable value.
   */
  interface SetValueCallback {
    /**
     * Variable is successfully updated. New value is available in {@link JsVariable#getValue()}.
     */
    void success();

    /**
     * Variable hasn't been updated because exception was thrown. Most probably this exception was
     * thrown from setter function.
     */
    void exceptionThrown(JsValue exception);

    /**
     * Variable hasn't been updated for unknown reason.
     */
    void failure(Exception cause);
  }

  /**
   * Returns object property data if variable is an object property and its descriptor
   * is available.
   */
  JsObjectProperty asObjectProperty();
}
