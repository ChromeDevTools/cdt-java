// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

import org.json.simple.JSONArray;

/**
 * Optional base interface for JSON type interface. Underlying object becomes available
 * to user this way. The JSON type instance may be created from any supported object
 * (e.g. from {@link JSONArray}), but may take advantage of this liberty only if it has no fields.
 */
public interface AnyObjectBased {
  Object getUnderlyingObject();
}
