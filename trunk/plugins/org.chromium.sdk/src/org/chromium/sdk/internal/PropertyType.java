// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

/**
 * Known V8 VM property types. The default is NORMAL.
 */
public enum PropertyType {
  NORMAL(0),
  FIELD(1),
  CONSTANT_FUNCTION(2),
  CALLBACKS(3),
  INTERCEPTOR(4),
  MAP_TRANSITION(5),
  CONSTANT_TRANSITION(6),
  NULL_DESCRIPTOR(7),
  ;

  public int value;

  private PropertyType(int value) {
    this.value = value;
  }

}
