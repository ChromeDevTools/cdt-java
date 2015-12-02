// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a method that corresponds to a type field (i.e. a property of JSON object).
 * Its use is optional, because all methods by default are recognized as field-reading methods.
 * Should be used to specify JSON property name.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonField {
  /**
   * Specifies JSON property name, which otherwise is derived from the method name (optional "get"
   * prefix is truncated with the first letter decapitalization).
   */
  String jsonLiteralName() default "";

  FieldLoadStrategy loadStrategy() default FieldLoadStrategy.AUTO;
}
