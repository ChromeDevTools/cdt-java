// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * This exception is thrown if the SDK protocol version is not compatible with
 * that supported by the browser.
 */
public class UnsupportedVersionException extends Exception {

  private static final long serialVersionUID = 1L;
  private final Version localVersion;
  private final Version remoteVersion;

  public UnsupportedVersionException(Version localVersion, Version remoteVersion) {
    this(localVersion, remoteVersion, "localVersion=" + localVersion
        + "; remoteVersion=" + remoteVersion);
  }

  public UnsupportedVersionException(Version localVersion, Version remoteVersion, String message) {
    super(message);
    this.localVersion = localVersion;
    this.remoteVersion = remoteVersion;
  }

  /**
   * @return the protocol version supported by the SDK
   */
  public Version getLocalVersion() {
    return localVersion;
  }

  /**
   * @return the incompatible protocol version supported by the browser
   */
  public Version getRemoteVersion() {
    return remoteVersion;
  }
}
