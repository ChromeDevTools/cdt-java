// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.internal.SubpropertiesMirror.JsonBased;
import org.chromium.sdk.internal.protocolparser.AnyObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;
import org.json.simple.JSONObject;

/**
 * Contains dynamic proxy method handlers for several well-known methods.
 */
class BaseHandlersLibrary {
  public static BaseHandlersLibrary INSTANCE;

  public Map<Method, ? extends MethodHandler> getAllHandlers() {
    return method2Handler;
  }

  /**
   * Generates Java implementation of standard methods of JSON type class (if needed):
   * {@link JsonObjectBased#getUnderlyingObject()},
   * {@link AnyObjectBased#getUnderlyingObject()} and {@link JsonSubtype#getSuper()}
   */
  public static void writeBaseMethodsJava(ClassScope scope, TypeHandler<?> typeHandler) {
    Class<?> typeClass = typeHandler.getTypeClass();

    // Generated getUnderlyingObject method if it's in interface.
    writeGetUnderlyingObject: {
      Method method;
      try {
        method = typeClass.getMethod("getUnderlyingObject");
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        // Method not found, skip.
        break writeGetUnderlyingObject;
      }

      MethodHandler.writeMethodDeclarationJava(scope, method);
      scope.startLine("{\n");
      scope.startLine("  return underlying;\n");
      scope.startLine("}\n");
    }

    typeHandler.getSubtypeSupport().writeGetSuperMethodJava(scope);
  }

  private final Map<Method, MethodHandler> method2Handler;

  private BaseHandlersLibrary() throws NoSuchMethodException {
    method2Handler = new HashMap<Method, MethodHandler>();
    Method[] objectMethods = {
        Object.class.getMethod("equals", Object.class),
        Object.class.getMethod("hashCode"),
        Object.class.getMethod("toString")
    };
    for (Method m : objectMethods) {
      method2Handler.put(m, new SelfCallMethodHanlder(m));
    }
    fill(method2Handler, new GetJsonObjectMethodHaldler(), new GetAnyObjectMethodHaldler(),
        new GetSuperMethodHaldler());
  }

  private static void fill(Map<Method, MethodHandler> map, MethodHandlerBase ... handlers) {
    for (MethodHandlerBase handler : handlers) {
      map.put(handler.getMethod(), handler);
    }
  }

  private static abstract class MethodHandlerBase extends MethodHandler {
    private final Method method;
    MethodHandlerBase(Method method) {
      this.method = method;
    }
    Method getMethod() {
      return method;
    }
  }

  private static class SelfCallMethodHanlder extends MethodHandlerBase {
    SelfCallMethodHanlder(Method method) {
      super(method);
    }

    @Override
    Object handle(ObjectData objectData, Object[] args)
        throws IllegalAccessException, InvocationTargetException {
      return getMethod().invoke(objectData, args);
    }

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
      // Ignore.
    }

    @Override
    boolean requiresJsonObject() {
      return false;
    }
  }

  private static class GetJsonObjectMethodHaldler extends MethodHandlerBase {
    GetJsonObjectMethodHaldler() throws NoSuchMethodException {
      super(JsonObjectBased.class.getMethod("getUnderlyingObject"));
    }

    @Override
    JSONObject handle(ObjectData objectData, Object[] args) {
      return (JSONObject) objectData.getUnderlyingObject();
    }

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
    }

    @Override
    boolean requiresJsonObject() {
      return false;
    }
  }

  private static class GetAnyObjectMethodHaldler extends MethodHandlerBase {
    GetAnyObjectMethodHaldler() throws NoSuchMethodException {
      super(AnyObjectBased.class.getMethod("getUnderlyingObject"));
    }

    @Override
    Object handle(ObjectData objectData, Object[] args) {
      return objectData.getUnderlyingObject();
    }

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
    }

    @Override
    boolean requiresJsonObject() {
      return false;
    }
  }

  private static class GetSuperMethodHaldler extends MethodHandlerBase {
    GetSuperMethodHaldler() throws NoSuchMethodException {
      super(JsonSubtype.class.getMethod("getSuper"));
    }

    @Override
    Object handle(ObjectData objectData, Object[] args) {
      return objectData.getSuperObjectData().getProxy();
    }

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
      // Ignore.
    }

    @Override
    boolean requiresJsonObject() {
      return false;
    }
  }

  static {
    try {
      INSTANCE = new BaseHandlersLibrary();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
