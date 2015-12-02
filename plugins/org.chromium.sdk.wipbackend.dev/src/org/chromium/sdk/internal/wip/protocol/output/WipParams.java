// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol.output;

import org.json.simple.JSONObject;

/**
 * A base class for all method parameter classes.
 * It also allows to get the method name it corresponds to.
 */
public abstract class WipParams extends JSONObject {
  protected abstract String getRequestName();
}
