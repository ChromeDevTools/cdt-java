// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import org.chromium.sdk.internal.BrowserFactoryImpl;

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

  /** The host that will be used when specifying only the browser port. */
  public static final String LOCALHOST = "127.0.0.1";

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
   * 127.0.0.1 (localhost) on {@code port}. Returns a previously created
   * instance if one was created earlier for localhost and {@code port}.
   *
   * @param port the browser is listening on at 127.0.0.1 (localhost)
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   * @return a Browser instance for the (127.0.0.1, port) endpoint
   */
  public abstract Browser create(int port, ConnectionLogger connectionLogger);

  /**
   * Constructs a Browser implementor instance that talks to a browser listening
   * at {@code host} and {@code port}. Returns a previously created
   * instance if one was created earlier for {@code host} and {@code port}.
   *
   * @param host the browser is running at
   * @param port the browser is listening on
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   * @return a Browser instance for the (host, port) endpoint
   */
  public abstract Browser create(String host, int port, ConnectionLogger connectionLogger);

  /**
   * Constructs StandaloneVm instance that talks to a V8 JavaScript VM via
   * DebuggerAgent opened at {@code host} and {@code port}. Does not check whether
   * we already opened connection to this address.
   * @param port V8 DebuggerAgent is listening on
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   */
  public abstract StandaloneVm createStandalone(int port,
      ConnectionLogger connectionLogger);
}
