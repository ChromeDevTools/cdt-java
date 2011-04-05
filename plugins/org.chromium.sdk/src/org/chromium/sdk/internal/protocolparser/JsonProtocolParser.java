// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser;

import org.json.simple.JSONObject;

/**
 * A typed parser for set of json types. It uses set of type interfaces
 * as model description and provides implementations for them. JsonProtocolParser
 * converts JSONObject into a required Java type instance.
 */
public interface JsonProtocolParser {

  /**
   * Parses {@link JSONObject} as typeClass type.
   */
  <T> T parse(JSONObject object, Class<T> typeClass) throws JsonProtocolParseException;

  /**
   * Parses any object as typeClass type. Non-JSONObject only makes sense for
   * types with {@link JsonType#subtypesChosenManually()} = true annotation.
   */
  <T> T parseAnything(Object object, Class<T> typeClass) throws JsonProtocolParseException;

}
