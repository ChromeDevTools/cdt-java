// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip.protocol.output;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;

/**
 * A base class for all method parameter classes that implies non-empty responses.
 * @param <R> a type of the corresponding response
 */
public abstract class WipParamsWithResponse<R> extends WipParams {
  public abstract R parseResponse(WipCommandResponse.Data success, WipGeneratedParserRoot parser)
      throws JsonProtocolParseException;
}
