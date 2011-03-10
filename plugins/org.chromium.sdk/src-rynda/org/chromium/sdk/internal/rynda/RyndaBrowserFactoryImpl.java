// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.net.InetSocketAddress;

import org.chromium.sdk.rynda.RyndaBrowser;
import org.chromium.sdk.rynda.RyndaBrowserFactory;

public class RyndaBrowserFactoryImpl implements RyndaBrowserFactory {
  @Override
  public RyndaBrowser createRyndaConnected(InetSocketAddress socketAddress,
      LoggerFactory connectionLoggerFactory) {
    return new RyndaBrowserImpl(socketAddress, connectionLoggerFactory);
  }
}
