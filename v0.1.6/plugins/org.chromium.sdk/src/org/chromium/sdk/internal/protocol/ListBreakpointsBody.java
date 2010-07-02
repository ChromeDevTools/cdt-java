// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import java.util.List;

import org.chromium.sdk.internal.protocol.data.BreakpointInfo;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ListBreakpointsBody extends JsonSubtype<CommandResponseBody> {
  List<BreakpointInfo> breakpoints();
}
