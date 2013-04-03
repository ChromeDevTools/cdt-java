// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.net.SocketAddress;
import java.util.logging.Logger;

import org.chromium.sdk.internal.JavascriptVmFactoryImpl;
import org.chromium.sdk.wip.WipBrowser;
import org.chromium.sdk.wip.WipBrowserFactory;

/**
 * A factory for {@link JavascriptVm} instances. Note that {@link WipBrowser} instances are
 * created in a specialized class {@link WipBrowserFactory}.
 */
public abstract class JavascriptVmFactory {
  /**
   * Gets a {@link JavascriptVmFactory} instance. This method should be overridden by
   * implementations that want to construct other implementations of
   * {@link Browser}.
   *
   * @return a {@link JavascriptVmFactory} singleton instance
   */
  public static JavascriptVmFactory getInstance() {
    return JavascriptVmFactoryImpl.INSTANCE;
  }

  /**
   * Constructs StandaloneVm instance that talks to a V8 JavaScript VM via
   * DebuggerAgent opened at {@code socketAddress}.
   * @param socketAddress V8 DebuggerAgent is listening on
   * @param connectionLogger provides facility for listening to network
   *        traffic; may be null
   */
  public abstract StandaloneVm createStandalone(SocketAddress socketAddress,
      ConnectionLogger connectionLogger);

  /**
   * @return SDK root logger that can be used to add handlers or to adjust log level
   */
  public static Logger getRootLogger() {
    return LOGGER;
  }

  private static final Logger LOGGER = Logger.getLogger("org.chromium.sdk");
}
