// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.output;

import org.json.simple.JSONObject;

/**
 * A base class for all method parameter classes.
 * It also allows to get the method name it corresponds to.
 */
public abstract class WipParams extends JSONObject {
  protected abstract String getRequestName();
}
