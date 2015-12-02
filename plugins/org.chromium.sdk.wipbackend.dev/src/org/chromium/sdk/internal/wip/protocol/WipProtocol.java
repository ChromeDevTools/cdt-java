// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol;

/**
 * Main utility class of Wip protocol implementation.
 */
public class WipProtocol {

  public static int parseInt(Object obj) {
    if (obj instanceof String) {
      String str = (String) obj;
      float f = Float.parseFloat(str);
      return Math.round(f);
    } else if (obj instanceof Number) {
      Number number = (Number) obj;
      return number.intValue();
    } else {
      throw new IllegalArgumentException();
    }
  }
}
