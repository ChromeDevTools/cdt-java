// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * An object that describes a Chrome DevTools protocol version.
 */
public class Version {
  private final int major;

  private final int minor;

  private volatile String cachedString;

  /**
   * Constructs an immutable Version instance given the {@code major} and
   * {@code minor} version parts.
   *
   * @param major version part
   * @param minor version part
   */
  public Version(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  /**
   * Checks if this version is compatible with that version (i.e. an SDK that
   * supports {@code this} version can talk to a Browser that supports {@code
   * serverVersion}).
   *
   * @param serverVersion version to check the compatiblity with
   * @return whether {@code this} version SDK can talk to a Browser that
   *         supports the {@code serverVersion} (i.e. their major versions match
   *         and the SDK minor version is not greater than that supported by
   *         the Browser).
   */
  public boolean isCompatibleWithServer(Version serverVersion) {
    return serverVersion.major == this.major && serverVersion.minor >= this.minor;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Version)) {
      return false;
    }
    Version that = (Version) obj;
    return this.major == that.major && this.minor == that.minor;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    if (cachedString == null) {
      StringBuilder sb = new StringBuilder();
      sb.append('[').append(major).append('.').append(minor).append(']');
      cachedString = sb.toString();
    }
    return cachedString;
  }
}
