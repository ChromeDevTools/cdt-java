// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.net.SocketAddress;

import org.chromium.sdk.internal.BrowserFactoryImpl;

/**
 * A factory for Browser instances.
 */
public abstract class BrowserFactory {

  private static BrowserFactory instance;

  /**
   * Gets a {@link BrowserFactory} instance. This method should be overridden by
   * implementations that want to construct other implementations of
   * {@link Browser}.
   *
   * @return a BrowserFactory singleton instance
   */
  public static BrowserFactory getInstance() {
    if (instance == null) {
      instance = new BrowserFactoryImpl();
    }
    return instance;
  }

  /**
   * Returns a Browser implementor instance that talks to a browser listening at
   * {@code socketAddress}. Note that you shouldn't try to create several instances
   * of Browser connecting to the same {@code socketAddress}.
   *
   * @param socketAddress the browser is listening on
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   * @return a Browser instance for the {@code socketAddress}
   */
  public abstract Browser create(SocketAddress socketAddress, ConnectionLogger connectionLogger);

  /**
   * Constructs StandaloneVm instance that talks to a V8 JavaScript VM via
   * DebuggerAgent opened at {@code socketAddress}.
   * @param socketAddress V8 DebuggerAgent is listening on
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   */
  public abstract StandaloneVm createStandalone(SocketAddress socketAddress,
      ConnectionLogger connectionLogger);
}
