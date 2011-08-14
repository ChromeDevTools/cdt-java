// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.List;

import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.WipBrowser;

public abstract class WipBackendBase implements WipBackend {
  private final String id;
  private final String description;

  public WipBackendBase(String id, String description) {
    this.id = id;
    this.description = description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getDescription() {
    return description;
  }

  static WipBackendBase castArgument(WipBackend wipBackend) {
    try {
      return (WipBackendBase) wipBackend;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Incorrect backend argument", e);
    }
  }

  abstract List<? extends WipBrowser.WipTabConnector> getTabs(WipBrowserImpl browserImpl) throws IOException;
}
