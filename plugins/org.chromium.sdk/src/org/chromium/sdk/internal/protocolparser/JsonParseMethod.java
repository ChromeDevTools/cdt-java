// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.json.simple.JSONObject;


/**
 * Annotates 'parse' method in user-provided 'root' interface
 * (see {@link JsonParserRoot}). 'Parse' method is the only user access to a JSON parser.
 * <p>
 * 'Parse' method must conform following requirements:
 * <ul>
 * <li>return one of {@link JsonType}-annotated types supported by parser,
 * <li>have exactly one parameter: either {@link Object} or {@link JSONObject},
 * <li>throw {@link JsonProtocolParseException}.
 * </ul>
 * Method name is unspecified.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JsonParseMethod {
}
