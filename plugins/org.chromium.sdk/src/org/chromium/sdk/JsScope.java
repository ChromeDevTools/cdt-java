// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.List;

/**
 * An object that represents a scope in JavaScript.
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
   */
  List<? extends JsVariable> getVariables();

  /**
   * Subtype that exposes the value of the 'with' statement expression (the value might be
   * already converted by ToObject).
   */
  interface WithScope extends JsScope {
    JsValue getWithArgument();
  }
}
