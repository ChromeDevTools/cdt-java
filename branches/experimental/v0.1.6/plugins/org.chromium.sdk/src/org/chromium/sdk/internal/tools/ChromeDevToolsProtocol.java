// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools;

/**
 * Fields of a ChromeDevTools protocol message.
 */
public enum ChromeDevToolsProtocol {
  COMMAND("command"),
  DATA("data"),
  RESULT("result"),
  ;

  public final String key;

  private ChromeDevToolsProtocol(String key) {
    this.key = key;
  }
}
