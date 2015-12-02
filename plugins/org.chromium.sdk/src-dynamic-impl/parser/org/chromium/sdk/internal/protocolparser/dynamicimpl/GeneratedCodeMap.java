// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.util.Map;

/**
 * Maps json type interfaces to full class name of their generated implementations.
 */
public class GeneratedCodeMap {
  private final Map<Class<?>, String> type2ImplClassName;

  public GeneratedCodeMap(Map<Class<?>, String> type2ImplClassName) {
    this.type2ImplClassName = type2ImplClassName;
  }

  String getTypeImplementationReference(Class<?> type) {
    return type2ImplClassName.get(type);
  }
}
