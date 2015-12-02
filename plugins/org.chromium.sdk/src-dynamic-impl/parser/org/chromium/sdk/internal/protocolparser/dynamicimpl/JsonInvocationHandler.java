// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The implementation of {@link InvocationHandler} for JSON types. It dispatches calls to method
 * handlers from the map.
 */
class JsonInvocationHandler implements InvocationHandler {
  private final ObjectData objectData;
  private final Map<Method, MethodHandler> methodHandlerMap;

  JsonInvocationHandler(ObjectData objectData, Map<Method, MethodHandler> methodHandlerMap) {
    this.objectData = objectData;
    this.methodHandlerMap = methodHandlerMap;
  }

  ObjectData getObjectData() {
    return objectData;
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    MethodHandler methodHandler = methodHandlerMap.get(method);
    if (methodHandler == null) {
      throw new RuntimeException("No method handler for " + method);
    }
    return methodHandler.handle(objectData, args);
  }
}
