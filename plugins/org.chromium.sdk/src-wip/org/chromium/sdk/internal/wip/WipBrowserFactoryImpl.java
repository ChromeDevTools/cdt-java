// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import java.net.InetSocketAddress;

import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;

public class WipBrowserFactoryImpl implements WipBrowserFactory {
  @Override
  public WipBrowser createBrowser(InetSocketAddress socketAddress,
      LoggerFactory connectionLoggerFactory) {
    return new WipBrowserImpl(socketAddress, connectionLoggerFactory);
  }
}
