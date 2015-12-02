// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import java.io.IOException;
import java.util.List;

import org.chromium.sdk.wip.WipBackend;
import org.chromium.sdk.wip.WipBrowser;

/**
 * An internal interface to {@link WipBackend} implementation.
 */
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
      throw new IllegalArgumentException("Incorrect backend argument type", e);
    }
  }

  public abstract List<? extends WipBrowser.WipTabConnector> getTabs(
      WipBrowserImpl browserImpl) throws IOException;
}
