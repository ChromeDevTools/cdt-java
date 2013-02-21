// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A variable from JavaScript declarative scope. This variable is backed by a property
 * of any object, instead it is a variable that has been explicitly declared in program,
 * for example with 'var' keyword.
 * <p>This variable explicitly supports mutation operation.
 * @see {@link JsScope.Declarative}
 */
public interface JsDeclarativeVariable extends JsVariable {
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
     * Variable hasn't been updated for unknown reason.
     */
    void failure(Exception cause);
  }
}
