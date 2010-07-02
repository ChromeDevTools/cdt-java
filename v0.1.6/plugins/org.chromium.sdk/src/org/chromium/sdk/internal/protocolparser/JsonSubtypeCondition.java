// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a condition for a field-reading method. It is one from group of annotations
 * that mark key fields for choosing particular subtype. They set conditions on JSON fields in
 * a subtype interface that drive subtype auto-selection at parsing time.
 * <p>
 * Specifies a general condition; it requires that:
 * <ul>
 * <li>field should be null, if {@link #valueIsNull()} is true,
 * <li>field should be absent, if {@link #fieldIsAbsent()} is true,
 * <li>field should exist, if no options are set.
 * </ul>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSubtypeCondition {
  boolean fieldIsAbsent() default false;
  boolean valueIsNull() default false;
}
