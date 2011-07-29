// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;

/**
 * A base class for all method parameter classes that implies non-empty responses.
 * @param <R> a type of the corresponding response
 */
public abstract class WipParamsWithResponse<R> extends WipParams {
  public abstract R parseResponse(WipCommandResponse.Data success, JsonProtocolParser parser)
      throws JsonProtocolParseException;
}
