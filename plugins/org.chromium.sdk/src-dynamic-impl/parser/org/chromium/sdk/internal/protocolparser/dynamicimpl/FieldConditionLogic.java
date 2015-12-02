// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.MethodScope;

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

        @Override
        void writeCheckJava(MethodScope scope, String valueRef, String hasValueRef,
            String resultRef, QuickParser<?> quickParser) {
          scope.startLine("boolean " + resultRef + ";\n");
          scope.startLine("if (" + hasValueRef + ") {\n");
          scope.indentRight();
          quickParser.writeParseQuickCode(scope, valueRef, "parserResult");
          scope.startLine(resultRef + " = " + required + " == parserResult;\n");
          scope.indentLeft();
          scope.startLine("} else {\n");
          scope.startLine("  " + resultRef + " = false;\n");
          scope.startLine("}\n");
        }
      });
    }
    JsonSubtypeConditionCustom customAnn = m.getAnnotation(JsonSubtypeConditionCustom.class);
    if (customAnn != null) {
      Class<? extends JsonValueCondition<?>> condition = customAnn.condition();
      // We do not know exact type of condition. But we also do not care about result type
      // in 'constraint'. Compiler cannot catch the wildcard here, so we use an assumed type.
      Class<? extends JsonValueCondition<Void>> assumedTypeCondition =
          (Class<? extends JsonValueCondition<Void>>) customAnn.condition();
      final CustomConditionWrapper<?> constraint =
          CustomConditionWrapper.create(assumedTypeCondition);
      results.add(new FieldConditionLogic(true) {
        @Override
        boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser)
            throws JsonProtocolParseException {
          return hasValue && constraint.checkValue(parser.parseValueQuick(unparsedValue));
        }

        @Override
        void writeCheckJava(MethodScope scope, String valueRef, String hasValueRef,
            String resultRef, QuickParser<?> quickParser) {
          scope.startLine("boolean " + resultRef + ";\n");
          scope.startLine("if (" + hasValueRef + ") {\n");
          scope.indentRight();
          quickParser.writeParseQuickCode(scope, valueRef, "parserResult");
          constraint.writeParseJava(scope, "parserResult", "constraintResult");
          scope.startLine(resultRef + " = constraintResult;\n");
          scope.indentLeft();
          scope.startLine("} else {\n");
          scope.startLine("  " + resultRef + " = false;\n");
          scope.startLine("}\n");
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

          @Override
          void writeCheckJava(MethodScope scope, String valueRef, String hasValueRef,
              String resultRef, QuickParser<?> quickParser) {
            scope.startLine("boolean " + resultRef + " = !" + hasValueRef + ";\n");
          }
        });
      }
      if (conditionAnn.valueIsNull()) {
        results.add(new FieldConditionLogic(false) {
          @Override
          boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser) {
            return hasValue && unparsedValue != null;
          }

          @Override
          void writeCheckJava(MethodScope scope, String valueRef, String hasValueRef,
              String resultRef, QuickParser<?> quickParser) {
            scope.startLine("boolean " + resultRef + " = " + valueRef + " != null;\n");
          }
        });
      }
      if (savedResSize == results.size()) {
        results.add(new FieldConditionLogic(false) {
          @Override
          boolean checkValue(boolean hasValue, Object unparsedValue, QuickParser<?> parser) {
            return hasValue;
          }

          @Override
          void writeCheckJava(MethodScope scope, String valueRef, String hasValueRef,
              String resultRef, QuickParser<?> quickParser) {
            scope.startLine("boolean " + resultRef + " = " + hasValueRef + ";\n");
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
    static <T> CustomConditionWrapper<T> create(
        Class<? extends JsonValueCondition<T>> constraintClass) {
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
      return constraint.conforms((T) parsedValue);
    }

    public void writeParseJava(ClassScope classScope, String valueRef, String resultRef) {

      abstract class StaticField implements JavaCodeGenerator.ElementData {
        abstract String getFieldName();
      }

      StaticField field = classScope.getRootClassScope().addMember(constraint.getClass(),
          new JavaCodeGenerator.ElementFactory<StaticField>() {
        @Override
        public StaticField create(final int code) {
          return new StaticField() {
            @Override public void generateCode(ClassScope classScope) {
              classScope.startLine("private static final " +
                  constraint.getClass().getCanonicalName() + " " + getFieldName() + " = new " +
                  constraint.getClass().getCanonicalName() + "();\n");
            }
            @Override String getFieldName() {
              return "CUSTOM_CONDITION_" + code;
            }
          };
        }
      });
      classScope.startLine("boolean " + resultRef + " = " + field.getFieldName() + ".conforms(" +
          valueRef + ");\n");
    }
  }

  abstract void writeCheckJava(MethodScope methodScope, String valueRef, String hasValueRef,
      String resultRef, QuickParser<?> quickParser);
}