// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType(subtypesChosenManually=true)
public interface WipTabList {
  @JsonSubtypeCasting List<TabDescription> asTabList() throws JsonProtocolParseException;

  @JsonType interface TabDescription {
    String faviconUrl();
    String title();
    String url();

    String thumbnailUrl();

    @JsonOptionalField
    String devtoolsFrontendUrl();

    @JsonOptionalField
    String webSocketDebuggerUrl();
  }
}
