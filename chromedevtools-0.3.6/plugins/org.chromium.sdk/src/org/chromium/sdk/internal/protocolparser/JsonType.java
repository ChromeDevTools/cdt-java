// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Marks an interface as interface to json type object. This way user may hold JSON object in
 * form of statically typed Java interface. The interface provides methods for reading properties
 * (here called fields, because we imply there are "types" in JSON) and for accessing subtypes.
 * <p>
 * In this design casting to subtypes means getting a different object of the subtype interface.
 * For a type interface, a set of subtypes is defined by its methods
 * with {@link JsonSubtypeCasting} annotation. These methods provide access to subtype objects.
 * From the parsing point of view, subtypes are supported in 2 different ways, as controlled
 * by {@link #subtypesChosenManually()} flag:
 * <ul>
 * <li>{@link #subtypesChosenManually()} is false; when parsing, a particular subtype is selected
 * automatically from set of all possible subtypes. JsonSubtypeCondition* annotations in subtypes
 * define conditions for selection. Subtype object (together with sub-subtype object, etc) is
 * created at the time of parsing. An empty subtype, which is selected if nothing else matches,
 * may be declared with void-returning {@link JsonSubtypeCasting}-marked method.
 * <li>{@link #subtypesChosenManually()} is true; subtype is not determined automatically. Instead,
 * clients may choose a casting method themselves and invoke parsing and object creation at runtime.
 * JsonType objects with {@link #subtypesChosenManually()}=true may be built not only on
 * {@link JSONObject}, but also on {@link JSONArray} etc.
 * </ul>
 * <p>
 * To provide access to underlying {@link JSONObject} the type interface may extend
 * {@link JsonObjectBased} interface. To provide access to underlying object
 * (not necessarily JSONObject) type interface may extend {@link AnyObjectBased} interface.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonType {
  boolean subtypesChosenManually() default false;
  boolean allowsOtherProperties() default false;
}
