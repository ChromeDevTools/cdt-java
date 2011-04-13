// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.net.InetSocketAddress;

import org.chromium.sdk.Browser;
import org.chromium.sdk.wip.WipBrowserFactory;

public class WipBrowserFactoryImpl implements WipBrowserFactory {
  @Override
  public Browser createBrowser(InetSocketAddress socketAddress,
      LoggerFactory connectionLoggerFactory) {
    return new WipBrowserImpl(socketAddress, connectionLoggerFactory);
  }
}
