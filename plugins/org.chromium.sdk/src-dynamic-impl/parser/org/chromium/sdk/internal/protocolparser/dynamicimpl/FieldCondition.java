// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.MethodScope;

/**
 * An implementation of JsonSubtypeCondition* annotations. Basically it only holds all parameters
 * and delegates actual condition evaluating to {@link #conditionLogic}.
 */
class FieldCondition {
  private final String propertyName;
  private final QuickParser<?> quickParser;
  private final FieldConditionLogic conditionLogic;

  FieldCondition(String propertyName, QuickParser<?> quickParser,
      FieldConditionLogic conditionLogic) throws JsonProtocolModelParseException {
    if (conditionLogic.requiresQuickParser() && quickParser == null) {
      throw new JsonProtocolModelParseException(
          "The choose condition does not work with the type of " + propertyName);
    }
    this.propertyName = propertyName;
    this.quickParser = quickParser;
    this.conditionLogic = conditionLogic;
  }

  String getPropertyName() {
    return propertyName;
  }

  /**
   * @param hasValue whether field exists in JSON object (however its value may be null)
   * @param unparsedValue value of the field if hasValue is true or undefined otherwise
   */
  boolean checkValue(boolean hasValue, Object unparsedValue) throws JsonProtocolParseException {
    return conditionLogic.checkValue(hasValue, unparsedValue, quickParser);
  }

  public void writeCheckJava(MethodScope methodScope, String valueRef, String hasValueRef,
      String resultRef) {
    conditionLogic.writeCheckJava(methodScope, valueRef, hasValueRef, resultRef, quickParser);
  }
}