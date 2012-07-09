// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;

/**
 * An internal facility for navigating from object of base type to object of subtype. Used only
 * when user wants to parse JSON object as subtype.
 * It works in terms of {@link ObjectData}.
 */
abstract class SubtypeCaster {
  private final Class<?> baseType;
  private final RefToType<?> subtypeRef;

  SubtypeCaster(Class<?> baseType, RefToType<?> subtypeRef) {
    this.baseType = baseType;
    this.subtypeRef = subtypeRef;
  }

  abstract ObjectData getSubtypeObjectData(ObjectData baseObjectData)
      throws JsonProtocolParseException;

  Class<?> getSubtype() {
    return subtypeRef.getTypeClass();
  }

  TypeHandler<?> getSubtypeHandler() {
    return subtypeRef.get();
  }

  Class<?> getBaseType() {
    return baseType;
  }

  abstract void writeJava(ClassScope scope, String expectedTypeName, String superTypeValueRef,
      String resultRef);
}
