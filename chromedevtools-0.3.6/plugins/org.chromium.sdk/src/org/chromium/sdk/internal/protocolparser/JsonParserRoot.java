// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a user-provided main interface to JSON parser -- 'root' interface.
 * All methods of the interface must be 'parse' methods (see {@link JsonParseMethod}).
 * The interface may extend other {@link JsonParserRoot}-annotated interfaces
 * explicitly and implicitly.
 * @see JsonParseMethod
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsonParserRoot {
}
