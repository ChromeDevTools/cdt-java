// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

/**
 * Defines a strategy the JSON parser should use for a particular field.
 */
public enum FieldLoadStrategy {
  /**
   * Parse field immediately.
   */
  EAGER,

  /**
   * Parse field on demand.
   */
  LAZY,

  /**
   * Compiler should choose strategy itself.
   */
  AUTO;
}
