// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * Extends {@link JsObject} interface with the methods for function-specific properties.
 */
public interface JsFunction extends JsObject {

  /**
   * @return script the function resides in or null if script is not available
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  Script getScript() throws MethodIsBlockingException;

  /**
   * Returns position of opening parenthesis of function arguments. Position is absolute
   * within resource (not relative to script start position).
   * @return position or null if position is not available
   * @throws MethodIsBlockingException because it may need to load value from remote
   */
  TextStreamPosition getOpenParenPosition() throws MethodIsBlockingException;
}
