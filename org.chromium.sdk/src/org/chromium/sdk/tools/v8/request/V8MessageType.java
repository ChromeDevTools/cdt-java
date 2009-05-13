// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

/**
 * Message types.
 */
public enum V8MessageType {

  // TODO(apavlov): revisit this
  REQUEST("request"), //$NON-NLS-1$
  RESPONSE("response"), //$NON-NLS-1$
  EVENT("event"), //$NON-NLS-1$
  ;

  public String value;

  V8MessageType(String value) {
    this.value = value;
  }
}
