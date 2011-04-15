// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.FileScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.MethodScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.Util;
import org.json.simple.JSONObject;

/**
 * A parser that generates dynamic proxy implementation of JsonType interface
 * for a {@link JSONObject}.
 * It creates dynamic proxy instance in 2 steps. First {@link #parseValue(Object, ObjectData)}
 * outputs {@link ObjectData}, which gets stored in field storage array. Later, when we are
 * about to return the value to a user, it is converted to a dynamic proxy instance by
 * {@link #VALUE_FINISHER} converter. We have to store an intermediate value for easier data
 * manipulation (dynamic proxy does not have any interfaces that we could make use of).
 */
class JsonTypeParser<T> extends SlowParser<ObjectData> {
  private final RefToType<T> refToType;
  private final boolean isNullable;
  private final boolean isSubtyping;

  JsonTypeParser(RefToType<T> refToType, boolean isNullable, boolean isSubtyping) {
    this.refToType = refToType;
    this.isNullable = isNullable;
    this.isSubtyping = isSubtyping;
  }

  RefToType<T> getType() {
    return refToType;
  }

  @Override
  public ObjectData parseValue(Object value, ObjectData thisData)
      throws JsonProtocolParseException {
    if (isNullable && value == null) {
      return null;
    }
    if (value == null) {
      throw new JsonProtocolParseException("null input");
    }
    TypeHandler<T> typeHandler = refToType.get();
    if (isSubtyping) {
      return typeHandler.parse(value, thisData);
    } else {
      return typeHandler.parseRootImpl(value);
    }
  }

  @Override
  public FieldLoadedFinisher getValueFinisher() {
    return VALUE_FINISHER;
  }

  @Override
  public JsonTypeParser<?> asJsonTypeParser() {
    return this;
  }

  public boolean isSubtyping() {
    return isSubtyping;
  }

  private static final FieldLoadedFinisher VALUE_FINISHER = new FieldLoadedFinisher() {
    @Override
    Object getValueForUser(Object cachedValue) {
      if (cachedValue == null) {
        return null;
      }
      ObjectData data = (ObjectData) cachedValue;
      return data.getProxy();
    }
  };

  @Override
  public void appendFinishedValueTypeNameJava(FileScope scope) {
    scope.append(refToType.getTypeClass().getCanonicalName());
  }

  @Override
  public void appendInternalValueTypeNameJava(FileScope classScope) {
    classScope.append(classScope.getTypeImplReference(refToType.get()));
  }

  @Override
  void writeParseCode(MethodScope scope, String valueRef,
      String superValueRef, String resultRef) {
    String typeName = scope.getTypeImplReference(refToType.get());
    scope.startLine(typeName + " " + resultRef + ";\n");

    if (isNullable) {
      scope.startLine("if (" + valueRef+ " == null) {\n");
      scope.startLine("  " + resultRef+ " = null;\n");
      scope.startLine("}\n");
    }
    scope.startLine("if (" + valueRef+ " == null) {\n");
    scope.startLine("  throw new " + Util.BASE_PACKAGE +
        ".JsonProtocolParseException(\"null input\");\n");
    scope.startLine("}\n");
    if (isSubtyping) {
      scope.startLine(resultRef + " = new " + typeName + "(" + valueRef + ", " +
          superValueRef + ");\n");
    } else {
      scope.startLine(resultRef + " = " + typeName + ".parse(" + valueRef + ");\n");
    }
  }

  @Override
  boolean javaCodeThrowsException() {
    return false;
  }
}
