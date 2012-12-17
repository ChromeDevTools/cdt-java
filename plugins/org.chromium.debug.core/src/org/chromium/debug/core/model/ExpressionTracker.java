// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.containsSafe;

import org.chromium.sdk.JsVariable;
import org.chromium.sdk.util.JavaScriptExpressionBuilder;
import org.chromium.sdk.util.JavaScriptExpressionBuilder.ExpressionComponentFormatter;
import org.chromium.sdk.util.JavaScriptExpressionBuilder.VariableAccess;

/**
 * Tracks how one variable is obtained as an inner property of another variable.
 * This allows to build corresponding watch expression for the variable later.
 * Each variable (and therefore value) gets its own {@link ExpressionTracker#Node}. The node
 * keeps reference to its parent node, plus it tracks some additional data about expression
 * (for example that no expression is possible for this particular variable).
 */
class ExpressionTracker {
  /**
   * Corresponds to pair variable/value.
   */
  interface Node {
    String calculateQualifiedName();
    String calculateParentQualifiedName();

    /**
     * Creates node for nested variable (most likely object property).
     */
    Node createVariableNode(JsVariable jsVariable, boolean isInternal);
  }

  /**
   * A factory that knows how to build variable and scope nodes. It helps to track,
   * whether fully qualified names possible for variables from this point or not (such as when
   * we track variable under the closure scope that cannot be accessed by JavaScript expressions).
   */
  interface ScopeAndVariableFactory {
    Node createVariableNode(JsVariable jsVariable, boolean isInternal);
    Node createScopeNode();
  }

  static final ScopeAndVariableFactory STACK_FRAME_FACTORY = new ScopeAndVariableFactory() {
    @Override public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
      return defaultCreateVariable(jsVariable, isInternal, null);
    }
    @Override public Node createScopeNode() {
      return liveScopeNode;
    }
    private final Node liveScopeNode = new Node() {
      @Override public String calculateQualifiedName() {
        return null;
      }
      @Override public String calculateParentQualifiedName() {
        return null;
      }
      @Override
      public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
        return defaultCreateVariable(jsVariable, isInternal, null);
      }
    };
  };

  static final ScopeAndVariableFactory FUNCTION_SCOPE_FACTORY = new ScopeAndVariableFactory() {
    @Override public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
      return NO_EXPRESSION_NODE;
    }
    @Override public Node createScopeNode() {
      return NO_EXPRESSION_NODE;
    }
  };

  static Node createExpressionNode(String expression) {
    return new ExpressionNodeImpl(expression);
  }


  private static class ExpressionNodeImpl implements Node, VariableAccess {
    private final String expression;
    ExpressionNodeImpl(String expression) {
      this.expression = expression;
    }
    @Override public String getShortName() {
      return expression;
    }
    @Override public ExpressionComponentFormatter getVariableFormatter() {
      return JavaScriptExpressionBuilder.SHORT_NAME_PAREN;
    }
    @Override public VariableAccess getParent() {
      return null;
    }
    @Override public String calculateQualifiedName() {
      return JavaScriptExpressionBuilder.buildQualifiedName(this);
    }
    @Override public String calculateParentQualifiedName() {
      return null;
    }
    @Override
    public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
      return defaultCreateVariable(jsVariable, isInternal, this);
    }
  }

  private static class DefaultNodeImpl implements Node, VariableAccess {
    private final JsVariable jsVariable;
    private final VariableAccess parentNode;
    private final ExpressionComponentFormatter qualifiedNameBuilder;

    public DefaultNodeImpl(JsVariable jsVariable, VariableAccess parentNode,
        ExpressionComponentFormatter qualifiedNameBuilder) {
      this.jsVariable = jsVariable;
      this.parentNode = parentNode;
      this.qualifiedNameBuilder = qualifiedNameBuilder;
    }

    @Override public JavaScriptExpressionBuilder.VariableAccess getParent() {
      return parentNode;
    }

    @Override public String getShortName() {
      return jsVariable.getName();
    }

    @Override public ExpressionComponentFormatter getVariableFormatter() {
      return qualifiedNameBuilder;
    }

    @Override
    public String calculateQualifiedName() {
      return JavaScriptExpressionBuilder.buildQualifiedName(this);
    }
    @Override public String calculateParentQualifiedName() {
      if (parentNode == null) {
        return null;
      }
      return JavaScriptExpressionBuilder.buildQualifiedName(parentNode);
    }
    @Override
    public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
      return defaultCreateVariable(jsVariable, isInternal, this);
    }
  }

  private static Node defaultCreateVariable(JsVariable jsVariable,
      boolean isInternal, VariableAccess parentNode) {
    if (isInternal && containsSafe(JavaScriptExpressionBuilder.SEMI_INTERNAL_PROPERTY_NAMES,
        jsVariable.getName())) {
      isInternal = false;
    }
    if (isInternal) {
      return NO_EXPRESSION_NODE;
    }
    ExpressionComponentFormatter nameComponenetBuilder;
    if (parentNode == null) {
      nameComponenetBuilder = JavaScriptExpressionBuilder.SHORT_NAME;
    } else {
      nameComponenetBuilder = JavaScriptExpressionBuilder.OBJECT_PROPERTY_NAME_BUILDER;
    }
    return new DefaultNodeImpl(jsVariable, parentNode, nameComponenetBuilder);
  }

  private static final Node NO_EXPRESSION_NODE = new Node() {
    @Override public String calculateQualifiedName() {
      return null;
    }
    @Override public String calculateParentQualifiedName() {
      return null;
    }
    @Override public Node createVariableNode(JsVariable jsVariable, boolean isInternal) {
      return NO_EXPRESSION_NODE;
    }
  };
}