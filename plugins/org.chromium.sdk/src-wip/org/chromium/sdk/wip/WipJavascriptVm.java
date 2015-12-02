// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.wip;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RemoteValueMapping;

/**
 * WIP-specific extension to {@link JavascriptVm}.
 */
public interface WipJavascriptVm extends JavascriptVm {

  /**
   * Creates new {@link PermanentRemoteValueMapping}. If the groups with the
   * same id already exists, another copy of local caches will be created which
   * might be undesired result. It is user's responsibility to choose unique group id.
   */
  PermanentRemoteValueMapping createPermanentValueMapping(String id);

  /**
   * @return extension to evaluate operations that supports {@link RemoteValueMapping}; not null
   */
  EvaluateToMappingExtension getEvaluateWithDestinationMappingExtension();
}
