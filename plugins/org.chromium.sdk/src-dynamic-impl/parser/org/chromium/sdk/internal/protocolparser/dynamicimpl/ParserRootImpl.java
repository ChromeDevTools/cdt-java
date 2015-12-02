// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;

import static org.chromium.sdk.util.BasicUtil.*;
import org.json.simple.JSONObject;

/**
 * Dynamic implementation of user 'root' interface to parser.
 * @param <R> 'root' interface type
 * @see JsonParserRoot
 */
class ParserRootImpl<R> {
  private final Class<R> rootClass;
  private final InvocationHandlerImpl invocationHandler;
  private final R instance;

  ParserRootImpl(Class<R> rootClass, Map<Class<?>, TypeHandler<?>> type2TypeHandler)
      throws JsonProtocolModelParseException {
    this.rootClass = rootClass;
    ParseInterfaceSession session = new ParseInterfaceSession(type2TypeHandler);
    session.run(rootClass);
    this.invocationHandler = session.createInvocationHandler();
    Object result = Proxy.newProxyInstance(rootClass.getClassLoader(),
        new Class<?>[] { rootClass }, invocationHandler);
    this.instance = (R) result;
  }

  R getInstance() {
    return instance;
  }

  private static class ParseInterfaceSession {
    private final Map<Class<?>, TypeHandler<?>> type2TypeHandler;
    private final Set<Class<?>> visitedInterfaces = new HashSet<Class<?>>(1);
    private final Map<Method, MethodDelegate> methodMap = new HashMap<Method, MethodDelegate>();

    ParseInterfaceSession(Map<Class<?>, TypeHandler<?>> type2TypeHandler) {
      this.type2TypeHandler = type2TypeHandler;
    }

    void run(Class<?> clazz) throws JsonProtocolModelParseException {
      parseInterfaceRecursive(clazz);
      for (Method method : BaseHandlersLibrary.OBJECT_METHODS) {
        methodMap.put(method, new SelfCallDelegate(method));
      }
    }

    private void parseInterfaceRecursive(Class<?> clazz) throws JsonProtocolModelParseException {
      if (containsSafe(visitedInterfaces, clazz)) {
        return;
      }
      visitedInterfaces.add(clazz);
      if (!clazz.isInterface()) {
        throw new JsonProtocolModelParseException(
            "Parser root type must be an interface: " + clazz);
      }
      JsonParserRoot jsonParserRoot = clazz.getAnnotation(JsonParserRoot.class);
      if (jsonParserRoot == null) {
        throw new JsonProtocolModelParseException(
            JsonParserRoot.class.getCanonicalName() + " annotation is expected in " + clazz);
      }
      for (Method m : clazz.getMethods()) {
        JsonParseMethod jsonParseMethod = m.getAnnotation(JsonParseMethod.class);
        if (jsonParseMethod == null) {
          throw new JsonProtocolModelParseException(
              JsonParseMethod.class.getCanonicalName() + " annotation is expected in " + clazz);
        }

        Class<?>[] exceptionTypes = m.getExceptionTypes();
        if (exceptionTypes.length > 1) {
          throw new JsonProtocolModelParseException("Too many exception declared in " + m);
        }
        if (exceptionTypes.length < 1 || exceptionTypes[0] != JsonProtocolParseException.class) {
          throw new JsonProtocolModelParseException(
              JsonProtocolParseException.class.getCanonicalName() +
              " exception must be declared in " + m);
        }

        Type returnType = m.getGenericReturnType();
        TypeHandler<?> typeHandler = type2TypeHandler.get(returnType);
        if (typeHandler == null) {
          throw new JsonProtocolModelParseException("Unknown return type in " + m);
        }

        Type[] arguments = m.getGenericParameterTypes();
        if (arguments.length != 1) {
          throw new JsonProtocolModelParseException("Exactly one argument is expected in " + m);
        }
        Type argument = arguments[0];
        MethodDelegate delegate;
        if (argument == JSONObject.class) {
          delegate = new ParseDelegate(typeHandler);
        } else if (argument == Object.class) {
          delegate = new ParseDelegate(typeHandler);
        } else {
          throw new JsonProtocolModelParseException("Unrecognized argument type in " + m);
        }
        methodMap.put(m, delegate);
      }

      for (Type baseType : clazz.getGenericInterfaces()) {
        if (baseType instanceof Class == false) {
          throw new JsonProtocolModelParseException("Base interface must be class in " + clazz);
        }
        Class<?> baseClass = (Class<?>) baseType;
        parseInterfaceRecursive(baseClass);
      }
    }

    InvocationHandlerImpl createInvocationHandler() {
      return new InvocationHandlerImpl(methodMap);
    }
  }

  private static class InvocationHandlerImpl implements InvocationHandler {
    private final Map<Method, MethodDelegate> map;

    InvocationHandlerImpl(Map<Method, MethodDelegate> map) {
      this.map = map;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return getSafe(map, method).invoke(proxy, this, args);
    }

    public void writeStaticMethodJava(ClassScope scope) {
      for (Map.Entry<Method, MethodDelegate> en : map.entrySet()) {
        en.getValue().writeStaticMethodJava(scope, en.getKey());
      }
    }
  }

  private static abstract class MethodDelegate {
    abstract Object invoke(Object proxy, InvocationHandlerImpl invocationHandlerImpl,
        Object[] args) throws Throwable;

    abstract void writeStaticMethodJava(ClassScope scope, Method key);
  }

  private static class ParseDelegate extends MethodDelegate {
    private final TypeHandler<?> typeHandler;

    ParseDelegate(TypeHandler<?> typeHandler) {
      this.typeHandler = typeHandler;
    }

    @Override
    Object invoke(Object proxy, InvocationHandlerImpl invocationHandlerImpl, Object[] args)
        throws JsonProtocolParseException {
      Object obj = args[0];
      return typeHandler.parseRoot(obj);
    }

    @Override
    void writeStaticMethodJava(ClassScope scope, Method method) {
      MethodHandler.writeMethodDeclarationJava(scope, method, STATIC_METHOD_PARAM_NAME_LIST);
      scope.append(JavaCodeGenerator.Util.THROWS_CLAUSE + " {\n");
      scope.indentRight();

      scope.startLine("return " + scope.getTypeImplReference(typeHandler) + ".parse(" +
          STATIC_METHOD_PARAM_NAME + ");\n");
      scope.indentLeft();
      scope.startLine("}\n");
      scope.append("\n");
    }

    private static final String STATIC_METHOD_PARAM_NAME = "obj";

    private static final List<String> STATIC_METHOD_PARAM_NAME_LIST =
        Collections.singletonList(STATIC_METHOD_PARAM_NAME);
  }

  private static class SelfCallDelegate extends MethodDelegate {
    private final Method method;

    SelfCallDelegate(Method method) {
      this.method = method;
    }

    @Override
    Object invoke(Object proxy, InvocationHandlerImpl invocationHandlerImpl, Object[] args)
        throws Throwable {
      try {
        return method.invoke(invocationHandlerImpl, args);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    void writeStaticMethodJava(ClassScope scope, Method method) {
    }
  }

  public Class<R> getType() {
    return rootClass;
  }

  public void writeStaticMethodJava(ClassScope rootClassScope) {
    invocationHandler.writeStaticMethodJava(rootClassScope);
  }
}
