// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import org.json.simple.JSONValue;

/**
 * Helps to build fully qualified name for variable. Fully qualified name should be a JavaScript
 * expression that allows to calculate this variable directly (assuming that all involved
 * operations always return the same values). This is basically about building expression
 * for an inner property of some object.
 * <p>A full path from the variable back to the root object must be provided. The SDK doesn't
 * keep it, it's a user responsibility to save the path.
 */
public class JavaScriptExpressionBuilder {
  /**
   * Builds fully qualified name for a variable. The input is a variable, wrapped
   * as {@link VariableAccess}, that provides a path back to the root object.
   */
  public static String buildQualifiedName(VariableAccess variableAccess) {
    StringBuilder builder = new StringBuilder();
    ExpressionComponentFormatter formatter = variableAccess.getVariableFormatter();
    formatter.formatRecursively(builder, variableAccess);
    return builder.toString();
  }

  /**
   * Defines how the last component of qualified name should be built.
   */
  public interface ExpressionComponentFormatter {
    /**
     * Builds the entire fully qualified name by recursively invoking building of all previous
     * components and finally adds formatting for the this component.
     * @param variable reflects variable and provides access to all parent variables
     */
    void formatRecursively(StringBuilder output, VariableAccess variable);
    boolean needsParentheses();
  }

  /**
   * Makes an input for the qualified name builder. It provides variable name, its associated
   * formatter and a link to the parent variable, thus implicitly providing the entire chain
   * of variables back to the root variable.
   */
  public interface VariableAccess {
    String getShortName();
    ExpressionComponentFormatter getVariableFormatter();
    VariableAccess getParent();
  }

  /**
   * Formats a variable as fully described by its short name. Parent variable isn't used.
   */
  public static final ExpressionComponentFormatter SHORT_NAME =
      new ExpressionComponentFormatter() {
    @Override public void formatRecursively(StringBuilder output, VariableAccess access) {
      output.append(access.getShortName());
    }
    @Override public boolean needsParentheses() {
      return false;
    }
  };

  /**
   * Formats a variable as fully described by its short name. Parent variable isn't used.
   * If the variable itself used as a parent, its name  will be closed in parentheses.
   */
  public static final ExpressionComponentFormatter SHORT_NAME_PAREN =
      new ExpressionComponentFormatter() {
    @Override
    public void formatRecursively(StringBuilder output, VariableAccess access) {
      output.append(access.getShortName());
    }
    @Override public boolean needsParentheses() {
      return true;
    }
  };

  /**
   * Formats a variable as a property of some other object. Property access will be
   * formatter in dot notation if its name allows, otherwise in bracket notation.
   */
  public static final ExpressionComponentFormatter OBJECT_PROPERTY_NAME_BUILDER =
      new ExpressionComponentFormatter() {
    @Override public void formatRecursively(StringBuilder output, VariableAccess wrapper) {
      String name = wrapper.getShortName();
      buildParentRef(output, wrapper.getParent());
      if (KEY_NOTATION_PROPERTY_NAME_PATTERN.matcher(name).matches()) {
        output.append(".").append(name);
      } else {
        output.append("[").append(JSONValue.toJSONString(name)).append("]");
      }
    }

    @Override public boolean needsParentheses() {
      return false;
    }
  };

  public static Long parsePropertyNameAsArrayIndex(String propertyName) {
    // Make cheap checks first.
    if (propertyName.length() > 10) {
      return null;
    }
    if (!ALL_DIGITS_PATTERN.matcher(propertyName).matches()) {
      return null;
    }
    Long index;
    try {
      // Long.valueOf is probably the most expensive check, because it throws exception
      // that is heavy.
      index = Long.valueOf(propertyName);
    } catch (NumberFormatException e) {
      return null;
    }
    if (!checkArrayIndexValue(index)) {
      return null;
    }
    return index;
  }

  public static boolean checkArrayIndexValue(long l) {
    return l >= 0L && l <= 0xfffffffeL;
  }

  private static void buildParentRef(StringBuilder output, VariableAccess variableAccess) {
    ExpressionComponentFormatter objectNameBuilder = variableAccess.getVariableFormatter();
    if (objectNameBuilder.needsParentheses()) {
      output.append('(');
      objectNameBuilder.formatRecursively(output, variableAccess);
      output.append(')');
    } else {
      objectNameBuilder.formatRecursively(output, variableAccess);
    }
  }

  /**
   * A simplified pattern that checks whether dot notation could be used in the property accessor.
   * Allows rare false negatives.
   */
  public static final Pattern KEY_NOTATION_PROPERTY_NAME_PATTERN = Pattern.compile("[_\\p{L}$]*");

  /**
   * A collection of non-normative property names that still could be used as a regular property
   * in expressions.
   */
  public static final Collection<String> SEMI_INTERNAL_PROPERTY_NAMES =
      Collections.singleton("__proto__");

  private static final Pattern ALL_DIGITS_PATTERN = Pattern.compile("\\d+");
}
