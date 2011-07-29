// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

import java.io.IOException;
import java.util.List;

import org.chromium.sdk.Browser;
import org.chromium.sdk.UnsupportedVersionException;

/**
 * WIP-specific extension to {@link Browser} interface.
 */
public interface WipBrowser extends Browser {
  @Override
  WipTabFetcher createTabFetcher() throws IOException, UnsupportedVersionException;

  interface WipTabFetcher extends TabFetcher {
    @Override
    List<? extends WipTabConnector> getTabs() throws IOException, IllegalStateException;
  }

  interface WipTabConnector extends TabConnector {
    String getTitle();
  }
}
