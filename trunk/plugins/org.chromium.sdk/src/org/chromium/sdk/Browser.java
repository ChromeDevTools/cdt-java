// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.io.IOException;

/**
 * An "entry point" of the SDK. A Browser instance is usually constructed once
 * per a debugged browser instance.
 */
public interface Browser {

  /**
   * Establishes the browser connection and checks for the protocol version
   * supported by the remote.
   * <p>
   * Does nothing if the Browser instance is already connected.
   *
   * @throws IOException if there was a transport layer error
   * @throws UnsupportedVersionException if the SDK protocol version is not
   *         compatible with that supported by the browser
   */
  void connect() throws IOException, UnsupportedVersionException;

  /**
   * Immediately disconnects from the Browser instance and closes the
   * connection. No method invocations relying on the Browser connection will
   * succeed after this call. This method SHOULD be called at the end of a
   * debugging session (once there are no more attached tabs.)
   * <p>
   * Does nothing if the Browser instance is not connected.
   */
  void disconnect();

  /**
   * Retrieves all browser tabs currently opened. Can only be invoked after the
   * browser connection has been successfully established.
   *
   * @return tabs that can be debugged in the associated Browser instance. An
   *         empty array is returned if no tabs are available.
   * @throws IOException if there was a transport layer failure
   * @throws IllegalStateException if this method is called while another
   *         invocation of the same method is in flight, or the Browser instance
   *         is not connected
   */
  BrowserTab[] getTabs() throws IOException, IllegalStateException;
}
