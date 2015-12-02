// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import java.util.regex.Pattern;

import org.json.simple.JSONValue;

/**
 * Builder for fully qualified name strings.
 */
public class WipExpressionBuilder {

  /**
   * Holds a value short name and optionally a qualified name builder.
   */
  public interface ValueNameBuilder {
    String getShortName();

    /**
     * @return value qualified name builder or null if there's no qualified name for the value
     */
    QualifiedNameBuilder getQualifiedNameBuilder();
  }

  /**
   * Builds a qualified name of some value.
   */
  public interface QualifiedNameBuilder {
    void append(StringBuilder output);

    /**
     * @return whether the qualified name should be enclosed before putting inside a bigger
     *   expression
     */
    boolean needsParentheses();
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

    final QualifiedNameBuilder qualifiedNameBuilder;
    if (propertyNameBuilder == null) {
      qualifiedNameBuilder = null;
    } else {
      qualifiedNameBuilder = new QualifiedNameBuilder() {
        @Override public boolean needsParentheses() {
          return propertyNameBuilder.needsParentheses();
        }

        @Override public void append(StringBuilder output) {
          propertyNameBuilder.build(propertyName, output);
        }
      };
    }

    return new ValueNameBuilder() {
      @Override public String getShortName() {
        return propertyName;
      }

      @Override public QualifiedNameBuilder getQualifiedNameBuilder() {
        return qualifiedNameBuilder;
      }
    };
  }

  static ValueNameBuilder createRootName(final String name, final boolean needsParentheses) {
    final QualifiedNameBuilder qualifiedNameBuilder = new QualifiedNameBuilder() {
      @Override
      public void append(StringBuilder output) {
        output.append(name);
      }

      @Override
      public boolean needsParentheses() {
        return needsParentheses;
      }
    };
    return new ValueNameBuilder() {
      @Override public String getShortName() {
        return name;
      }

      @Override public QualifiedNameBuilder getQualifiedNameBuilder() {
        return qualifiedNameBuilder;
      }
    };
  }

  public static ValueNameBuilder createRootNameNoDerived(final String name) {
    return new ValueNameBuilder() {
      @Override public String getShortName() {
        return name;
      }

      @Override public QualifiedNameBuilder getQualifiedNameBuilder() {
        return null;
      }
    };
  }

  /**
   * Builder for a qualified name of some object.
   */
  static class ObjectPropertyNameBuilder implements PropertyNameBuilder {
    private final QualifiedNameBuilder objectNameBuilder;

    ObjectPropertyNameBuilder(QualifiedNameBuilder objectNameBuilder) {
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
        output.append('(');
        objectNameBuilder.append(output);
        output.append(')');
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
