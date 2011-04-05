// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;

/**
 * Main utility class of Wip protocol implementation.
 */
public class WipProtocol {

  public static JsonProtocolParser getParser() {
    return WipParserAccess.get();
  }

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

  public static long parseSourceId(String value) {
    return Long.parseLong(value);
  }

  public static boolean parseHasChildren(Object hasChildren) {
    return hasChildren != Boolean.FALSE;
  }
}
