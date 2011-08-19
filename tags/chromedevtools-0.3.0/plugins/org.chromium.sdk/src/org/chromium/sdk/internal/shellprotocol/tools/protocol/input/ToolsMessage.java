// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.shellprotocol.tools.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONObject;

@JsonType
public interface ToolsMessage {

  String command();

  long result();

  @JsonOptionalField
  Data data();

  @JsonType(subtypesChosenManually=true)
  interface Data {

    // V8Debugger tool.
    @JsonSubtypeCasting
    JSONObject asDebuggerData() throws JsonProtocolParseException;

    @JsonSubtypeCasting
    String asNavigatedData() throws JsonProtocolParseException;


    // DevToolsService tool.
    /**
     * List of pairs (long id and String url) packed as array.
     */
    @JsonSubtypeCasting
    List<List<Object>> asListTabsData() throws JsonProtocolParseException;

    @JsonSubtypeCasting
    String asVersionData() throws JsonProtocolParseException;
  }
}
