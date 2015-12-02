// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import org.chromium.sdk.ConnectionLogger;

/**
 * The factory provides {@link ConnectionLogger} that can be used to output connection
 * traffic, supposedly to some UI.
 */
public interface NamedConnectionLoggerFactory {
  ConnectionLogger createLogger(String title);
}
