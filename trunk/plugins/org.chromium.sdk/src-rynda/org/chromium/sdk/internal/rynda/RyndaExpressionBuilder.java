// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.util.regex.Pattern;

import org.json.simple.JSONValue;

/**
 * Builder for fully qualified name strings.
 */
public class RyndaExpressionBuilder {

  /**
   * Builds a qualified name of some value.
   */
  public interface ValueNameBuilder {
    void append(StringBuilder output);

    /**
     * @return whether the qualified name should be enclosed before putting inside a bigger
     *   expression
     */
    boolean needsParentheses();

    String getShortName();
  }

  /**
   * Builds a qualified name of a property of some object.
   */
  interface PropertyNameBuilder {
    void build(String name, StringBuilder output);

    /**
     * @return whether the qualified name should be enclosed before putting inside a bigger
     *   expression
     */
    boolean needsParentheses();
  }

  /**
   * Combines property name with a {@link PropertyNameBuilder}.
   */
  static ValueNameBuilder createValueOfPropertyNameBuilder(final String propertyName,
      final PropertyNameBuilder propertyNameBuilder) {
    return new ValueNameBuilder() {
      @Override public boolean needsParentheses() {
        return propertyNameBuilder.needsParentheses();
      }

      @Override public void append(StringBuilder output) {
        propertyNameBuilder.build(propertyName, output);
      }

      @Override public String getShortName() {
        return propertyName;
      }
    };
  }

  static ValueNameBuilder createRootName(final String name, final boolean needsParentheses) {
    return new ValueNameBuilder() {
      @Override
      public void append(StringBuilder output) {
        output.append(name);
      }

      @Override
      public boolean needsParentheses() {
        return needsParentheses;
      }

      @Override public String getShortName() {
        return name;
      }
    };
  }

  /**
   * Builder for a qualified name of some object.
   */
  static class ObjectPropertyNameBuilder implements PropertyNameBuilder {
    private final ValueNameBuilder objectNameBuilder;

    ObjectPropertyNameBuilder(ValueNameBuilder objectNameBuilder) {
      this.objectNameBuilder = objectNameBuilder;
    }

    @Override
    public void build(String name, StringBuilder output) {
      buildParentRef(output);
      if (ALL_DIGITS.matcher(name).matches()) {
        output.append("[").append(name).append("]");
      } else {
        // TODO(peter.rybin): check that name is really a valid identifier.
        boolean isNameSimple = name.indexOf(' ') != -1;
        if (isNameSimple) {
          output.append(".").append(name);
        } else {
          output.append("[\"").append(JSONValue.escape(name)).append("\"]");
        }
      }
    }

    private void buildParentRef(StringBuilder output) {
      if (objectNameBuilder.needsParentheses()) {
        output.append('(').append(output).append(')');
      } else {
        objectNameBuilder.append(output);
      }
    }

    @Override
    public boolean needsParentheses() {
      return false;
    }
  }

  static final Pattern ALL_DIGITS = Pattern.compile("\\d+");
}
