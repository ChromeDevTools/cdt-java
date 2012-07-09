// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.List;

import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * An object that represents a scope in JavaScript.
 * TODO: consider adding object getter for both with and global scopes.
 */
public interface JsScope {

  enum Type {
    GLOBAL,
    LOCAL,
    WITH,
    CLOSURE,
    CATCH,
    UNKNOWN
  }

  /**
   * @return type of the scope
   */
  Type getType();

  /**
   * @return optional subtype when type is {@link Type#WITH} and null otherwise
   */
  WithScope asWithScope();

  /**
   * @return the variables known in this scope, in lexicographical order
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  List<? extends JsVariable> getVariables() throws MethodIsBlockingException;

  /**
   * Subtype that exposes the value of the 'with' statement expression (the value might be
   * already converted by ToObject).
   */
  interface WithScope extends JsScope {
    /**
     * @throws MethodIsBlockingException because it may need to load value from remote
     */
    JsValue getWithArgument() throws MethodIsBlockingException;
  }
}
