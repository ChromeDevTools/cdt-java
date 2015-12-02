// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

import java.util.List;

import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * An object that represents a scope in JavaScript. It could be either declarative or object
 * scope.
 */
public interface JsScope {

  enum Type {
    GLOBAL,
    LOCAL,
    WITH,
    CLOSURE,
    CATCH,
    UNKNOWN
    // TODO: add "block" type (corresponds to block scope in JavaScript 1.7).
  }

  /**
   * @return type of the scope
   */
  Type getType();

  /**
   * @return optional subtype when type is a native JavaScript scope and null otherwise
   */
  Declarative asDeclarativeScope();

  /**
   * @return optional subtype when type is {@link Type#WITH} and null otherwise
   */
  ObjectBased asObjectBased();

  <R> R accept(Visitor<R> visitor);

  interface Visitor<R> {
    R visitDeclarative(Declarative declarativeScope);
    R visitObject(ObjectBased objectScope);
  }

  /**
   * Mirrors <i>declarative</i> scope. It's all scopes except 'with' and 'global'. This scope
   * has a well-defined set of variables.
   */
  interface Declarative extends JsScope {
    /**
     * @return the variables known in this scope, in lexicographical order
     * @throws MethodIsBlockingException because it may need to load value from remote
     */
    List<? extends JsDeclarativeVariable> getVariables() throws MethodIsBlockingException;
  }

  /**
   * Mirrors <i>object</i> scope, i.e. the one built above a JavaScript object. It's either
   * 'with' or 'global' scope. Such scope contains all properties of the object, including
   * indirect ones from the prototype chain.
   */
  interface ObjectBased extends JsScope {
    /**
     * @throws MethodIsBlockingException because it may need to load value from remote
     */
    JsObject getScopeObject() throws MethodIsBlockingException;
  }
}
