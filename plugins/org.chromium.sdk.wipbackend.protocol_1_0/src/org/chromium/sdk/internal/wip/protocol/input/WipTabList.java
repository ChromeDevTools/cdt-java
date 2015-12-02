// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
