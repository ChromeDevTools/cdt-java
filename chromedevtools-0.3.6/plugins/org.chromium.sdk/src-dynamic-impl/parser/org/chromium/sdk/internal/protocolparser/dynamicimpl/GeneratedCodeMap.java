// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
