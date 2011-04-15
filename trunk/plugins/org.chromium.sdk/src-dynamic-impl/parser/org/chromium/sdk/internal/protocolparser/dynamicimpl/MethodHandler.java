// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;

/**
 * An abstract method handler for {@link JsonInvocationHandler}.
 */
abstract class MethodHandler {
  abstract Object handle(ObjectData objectData, Object[] args) throws Throwable;

  abstract void writeMethodImplementationJava(ClassScope classScope, Method m);

  protected static void appendMethodSignatureJava(ClassScope scope, Method m) {
    scope.append(m.getName());
    scope.append("(");
    boolean firstArg = true;
    for (Type arg : m.getGenericParameterTypes()) {
      if (!firstArg) {
        scope.append(", ");
      }
      JavaCodeGenerator.Util.writeJavaTypeName(arg, scope.getStringBuilder());
    }
    scope.append(")");
  }

  protected static void writeMethodDeclarationJava(ClassScope scope, Method m) {
    scope.startLine("@Override public ");
    JavaCodeGenerator.Util.writeJavaTypeName(m.getGenericReturnType(), scope.getStringBuilder());
    scope.append(" ");
    appendMethodSignatureJava(scope, m);
  }

  abstract boolean requiresJsonObject();
}
