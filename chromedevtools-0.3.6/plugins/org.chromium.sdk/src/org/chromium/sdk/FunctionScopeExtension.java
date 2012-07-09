// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.List;

import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * An extension to {@link JsFunction} API that returns function (closure) hidden scope.
 * @see JavascriptVm#getFunctionScopeExtension()
 */
public interface FunctionScopeExtension {
  /**
   * @return list of hidden function scopes (possibly empty)
   * @throws MethodIsBlockingException because function may need to load scope data
   *     on demand
   */
  List<? extends JsScope> getScopes(JsFunction function) throws MethodIsBlockingException;
}
