// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * This interface adds methods for handling function properties of JsObject.
 */
public interface JsFunction extends JsObject {

  /**
   * @return script the function resides in or null if script is not available
   */
  Script getScript();

  /**
   * Returns position of opening parenthesis of function arguments. Position is absolute
   * within resource (not relative to script start position).
   * @return position or null if position is not available
   */
  TextStreamPosition getOpenParenPosition();
}
