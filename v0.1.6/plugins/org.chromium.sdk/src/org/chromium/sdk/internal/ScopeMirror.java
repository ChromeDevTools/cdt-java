// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

/**
 * Datum that describes scope piece of information that comes from "scopes" response.
 */
public class ScopeMirror {

  public ScopeMirror(int type, int index) {
    this.type = type;
    this.index = index;
  }

  int getType() {
    return type;
  }

  int getIndex() {
    return index;
  }

  @Override
  public String toString() {
    return "scope type=" + type + " index=" + index;
  }

  private final int type;
  private final int index;
}
