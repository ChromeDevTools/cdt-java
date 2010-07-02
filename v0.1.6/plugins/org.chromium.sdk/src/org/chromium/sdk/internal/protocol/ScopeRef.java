// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ScopeRef {
  long index();
  long type();
}
