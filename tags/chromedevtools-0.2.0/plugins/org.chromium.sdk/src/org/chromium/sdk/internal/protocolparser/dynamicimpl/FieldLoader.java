// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

/**
 * This classs is responsible for parsing field values and saving them in {@link ObjectData}
 * for future use.
 */
class FieldLoader {
  private final String fieldName;
  private final int fieldPosInArray;
  private final SlowParser<?> slowParser;
  private final boolean isOptional;

  FieldLoader(int fieldPosInArray, String fieldName, SlowParser<?> slowParser, boolean isOptional) {
    this.fieldName = fieldName;
    this.fieldPosInArray = fieldPosInArray;
    this.slowParser = slowParser;
    this.isOptional = isOptional;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void parse(boolean hasValue, Object value, ObjectData objectData)
      throws JsonProtocolParseException {
    if (hasValue) {
      try {
        objectData.getFieldArray()[fieldPosInArray] = slowParser.parseValue(value, objectData);
      } catch (JsonProtocolParseException e) {
        throw new JsonProtocolParseException("Failed to parse field " + getFieldName(), e);
      }
    } else {
      if (!isOptional) {
        throw new JsonProtocolParseException("Field is not optional: " + getFieldName());
      }
    }
  }
}