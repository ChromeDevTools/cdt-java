// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionBoolValue;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
import org.chromium.sdk.internal.protocolparser.JsonValueCondition;

/**
 * An interface to field conditions logic. Some conditions are simple and never need parsed
 * values, others are more fine-grained and require quick parser before making actual checks.
 */
abstract class FieldConditionLogic {
  private final boolean logicRequiresQuickParser;

  FieldConditionLogic(boolean logicRequiresQuickParser) {
    this.logicRequiresQuickParser = logicRequiresQuickParser;
  }

  boolean requiresQuickParser() {
    return logicRequiresQuickParser;
  }

  /**
   * @param hasValue whether field exists in JSON object (however its value may be null)
   * @param quickParser parser that may be used if {@link #requiresQuickParser()} is true
   */
  abstract boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> quickParser)
      throws JsonProtocolParseException;

  /**
   * Constructor function that creates field condition logic from method annotations.
   */
  static FieldConditionLogic readLogic(Method m) throws JsonProtocolModelParseException {
    List<FieldConditionLogic> results = new ArrayList<FieldConditionLogic>(1);
    JsonSubtypeConditionBoolValue boolValueAnn =
        m.getAnnotation(JsonSubtypeConditionBoolValue.class);
    if (boolValueAnn != null) {
      final Boolean required = boolValueAnn.value();
      results.add(new FieldConditionLogic(true) {
        @Override
        boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser)
            throws JsonProtocolParseException {
          return hasValue && required == parser.parseValueQuick(unparsedValue);
        }
      });
    }
    JsonSubtypeConditionCustom customAnn = m.getAnnotation(JsonSubtypeConditionCustom.class);
    if (customAnn != null) {
      final CustomConditionWrapper<?> constraint =
          CustomConditionWrapper.create(customAnn.condition());
      results.add(new FieldConditionLogic(true) {
        @Override
        boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser)
            throws JsonProtocolParseException {
          return hasValue && constraint.checkValue(parser.parseValueQuick(unparsedValue));
        }
      });
    }
    JsonSubtypeCondition conditionAnn = m.getAnnotation(JsonSubtypeCondition.class);
    if (conditionAnn != null) {
      int savedResSize = results.size();
      if (conditionAnn.fieldIsAbsent()) {
        results.add(new FieldConditionLogic(false) {
          @Override
          boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser) {
            return !hasValue;
          }
        });
      }
      if (conditionAnn.valueIsNull()) {
        results.add(new FieldConditionLogic(false) {
          @Override
          boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser) {
            return hasValue && unparsedValue != null;
          }
        });
      }
      if (savedResSize == results.size()) {
        results.add(new FieldConditionLogic(false) {
          @Override
          boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser) {
            return hasValue;
          }
        });
      }
    }
    if (results.size() == 0) {
      return null;
    }
    if (results.size() > 1) {
      throw new JsonProtocolModelParseException("Too many constraints for field getter " + m);
    }
    return results.get(0);
  }

  private static class CustomConditionWrapper<T> {
    static <T, CL extends JsonValueCondition<T>> CustomConditionWrapper<T> create(
        Class<CL> constraintClass) {
      return new CustomConditionWrapper<T>(constraintClass);
    }

    private final JsonValueCondition<? super T> constraint;

    private CustomConditionWrapper(Class<? extends JsonValueCondition<? super T>> constraintClass) {
      try {
        constraint = constraintClass.newInstance();
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    boolean checkValue(Object parsedValue) {
      return constraint.conforms((T)parsedValue);
    }
  }

}