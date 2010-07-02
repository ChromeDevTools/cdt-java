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
   * Returns position inside a script. The position is of opening parenthesis of
   * function arguments, measured in unicode characters from the beginning of script text file.
   * @return position or -1 if position is not available
   */
  int getSourcePosition();
}
