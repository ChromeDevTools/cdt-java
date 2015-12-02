// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.FileScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.MethodScope;

/**
 * A parser that accepts value of JSON field and outputs value in another form (e.g. string
 * is converted to enum constant) to serve field getters in JsonType interfaces.
 * The parser is called "quick" because it is supposed to employ only fast conversions.
 * The quick parser should be suitable for subtype conditions
 * (see {@link JsonSubtypeCondition} etc), because they should not take long to evaluate.
 */
abstract class QuickParser<T> extends SlowParser<T> {
  @Override
  public T parseValue(Object value, ObjectData thisData) throws JsonProtocolParseException {
    return parseValueQuick(value);
  }

  /**
   * Parses input value and returns output that doesn't need any post-processing
   * by {@link FieldLoadedFinisher} (see {@link SlowParser}).
   */
  public abstract T parseValueQuick(Object value) throws JsonProtocolParseException;

  @Override
  public QuickParser<T> asQuickParser() {
    return this;
  }

  @Override
  public FieldLoadedFinisher getValueFinisher() {
    return null;
  }

  @Override
  public JsonTypeParser<?> asJsonTypeParser() {
    return null;
  }

  @Override
  public void appendInternalValueTypeNameJava(FileScope scope) {
    appendFinishedValueTypeNameJava(scope);
  }

  @Override
  void writeParseCode(MethodScope scope, String valueRef,
      String superValueRef, String resultRef) {
    writeParseQuickCode(scope, valueRef, resultRef);
  }

  @Override
  abstract boolean javaCodeThrowsException();

  abstract void writeParseQuickCode(MethodScope scope, String valueRef, String resultRef);
}
