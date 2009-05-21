// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

// TODO(apavlov): uncomment 2 lines broken without the implementation

// import org.chromium.sdk.internal.BrowserFactoryImpl;

/**
 * A factory for Browser instances. Each instance connects to a single endpoint
 * the debugged browser instance is listening on. There can only be a single
 * Browser instance for any given endpoint (as there can only be a single
 * connection to any Browser instance).
 * <p>
 * The {@code create(...)} methods are not guaranteed to perform hostname
 * resolution, hence hostnames such as "localhost" and "127.0.0.1" can be
 * considered different.
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
//      instance = new BrowserFactoryImpl();
    }
    return instance;
  }

  /**
   * Constructs a Browser implementor instance that talks to a browser instance
   * listening at 127.0.0.1 (localhost) on {@code port}.
   *
   * @param port the browser is listening on at 127.0.0.1 (localhost)
   * @return a Browser instance for the (127.0.0.1, port) endpoint
   */
  public abstract Browser create(int port);

  /**
   * Constructs a Browser implementor instance that talks to a browser instance
   * listening at {@code host} and {@code port}.
   *
   * @param host the browser is running at
   * @param port the browser is listening on
   * @return a Browser instance for the (host, port) endpoint
   */
  public abstract Browser create(String host, int port);

}
