// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A Javascript exception data holder for exceptions reported by a Javascript
 * virtual machine.
 */
public interface ExceptionData {

  /**
   * @return the thrown exception object
   */
  JsVariable getException();

  /**
   * @return whether this exception is uncaught
   */
  boolean isUncaught();

  /**
   * @return the text of the source line where the exception was thrown
   */
  String getSourceText();

  /**
   * @return the exception description (plain text)
   */
  String getExceptionText();
}
