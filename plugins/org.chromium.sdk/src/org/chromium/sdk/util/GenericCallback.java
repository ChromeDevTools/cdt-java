// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

/**
 * A generic callback used in asynchronous operations that either fail with exception
 * or return a result.
 */
public interface GenericCallback<T> {
  void success(T value);
  void failure(Exception exception);
}