// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
