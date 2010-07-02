// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

class EnumParser<T extends Enum<T>> extends QuickParser<T> {
  public static <T extends Enum<T>> EnumParser<T> create(Class<T> enumTypeClass,
      boolean isNullable) throws JsonProtocolModelParseException {
    return new EnumParser<T>(enumTypeClass, isNullable);
  }

  private final Method methodValueOf;
  private final boolean isNullable;
  private final Class<T> enumClass;

  private EnumParser(Class<T> enumClass, boolean isNullable)
      throws JsonProtocolModelParseException {
    this.enumClass = enumClass;
    this.isNullable = isNullable;
    try {
      this.methodValueOf = enumClass.getMethod("valueOf", String.class);
    } catch (NoSuchMethodException e) {
      throw new JsonProtocolModelParseException(
          "Failed to find valueOf method for parsing strings", e);
    }
  }

  @Override
  public T parseValueQuick(Object value) throws JsonProtocolParseException {
    if (isNullable && value == null) {
      return null;
    }
    if (value instanceof String == false) {
      throw new JsonProtocolParseException("String value expected");
    }
    String stringValue = (String) value;
    T result;
    try {
      result = enumClass.cast(methodValueOf.invoke(null, stringValue));
    } catch (IllegalArgumentException e) {
      throw new JsonProtocolParseException("Failed to parse enum constant " + stringValue, e);
    } catch (IllegalAccessException e) {
      throw new JsonProtocolParseException("Failed to call valueOf method", e);
    } catch (InvocationTargetException e) {
      throw new JsonProtocolParseException("Failed to call valueOf method", e);
    }
    if (result == null) {
      throw new JsonProtocolParseException("Failed to parse value " + value + " as enum");
    }
    return result;
  }
}
