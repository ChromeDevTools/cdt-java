// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.io.IOException;

/**
 * An "entry point" of the SDK.
 */
public interface Browser {

  /**
   * @return tabs that can be debugged in the associated Browser instance. An
   *         empty array is returned if no tabs are available.
   * @throws IOException
   */
  BrowserTab[] getTabs() throws IOException;

  /**
   * Establishes the browser connection and checks for the protocol version
   * supported by the remote.
   *
   * @throws UnsupportedVersionException if the SDK protocol version is not
   *         compatible with that supported by the browser.
   * @throws IOException
   */
  void connect() throws UnsupportedVersionException, IOException;

  /**
   * Immediately disconnects from the Browser instance closing. No method
   * invocations relying on the Browser connection will succeed after this call.
   * This method SHOULD be called at the end of a debugging session (once there
   * are no more attached tabs.)
   */
  void disconnect();
}
