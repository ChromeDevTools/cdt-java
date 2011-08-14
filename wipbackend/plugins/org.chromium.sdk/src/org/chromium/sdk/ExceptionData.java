// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A JavaScript exception data holder for exceptions reported by a JavaScript
 * virtual machine.
 */
public interface ExceptionData {

  /**
   * @return the thrown exception value
   */
  JsValue getExceptionValue();

  /**
   * @return whether this exception is uncaught
   */
  boolean isUncaught();

  /**
   * @return the text of the source line where the exception was thrown or null
   */
  String getSourceText();

  /**
   * @return the exception description (plain text)
   */
  String getExceptionMessage();
}
