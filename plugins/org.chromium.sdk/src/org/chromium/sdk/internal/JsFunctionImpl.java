// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsFunction;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;

/**
 * Generic implementation of {@link JsFunction}.
 */
class JsFunctionImpl extends JsObjectImpl implements JsFunction {
  private volatile TextStreamPosition openParenPosition = null;

  JsFunctionImpl(InternalContext context, String parentFqn, ValueMirror valueState) {
    super(context, parentFqn, valueState);
  }

  public Script getScript() {
    FunctionAdditionalProperties additionalProperties =
        (FunctionAdditionalProperties) getSubpropertiesMirror().getAdditionalProperties();

    int scriptId = additionalProperties.getScriptId();
    if (scriptId == FunctionAdditionalProperties.NO_SCRIPT_ID) {
      return null;
    }
    DebugSession debugSession = getInternalContext().getDebugSession();
    return debugSession.getScriptManager().findById(Long.valueOf(scriptId));
  }

  public TextStreamPosition getOpenParenPosition() {
    if (openParenPosition == null) {
      final FunctionAdditionalProperties additionalProperties =
          (FunctionAdditionalProperties) getSubpropertiesMirror().getAdditionalProperties();
      openParenPosition = new TextStreamPosition() {
        public int getOffset() {
          return additionalProperties.getSourcePosition();
        }
        public int getLine() {
          return additionalProperties.getLine();
        }
        public int getColumn() {
          return additionalProperties.getColumn();
        }
      };
    }
    return openParenPosition;
  }

  public int getSourcePosition() {
    FunctionAdditionalProperties additionalProperties =
        (FunctionAdditionalProperties) getSubpropertiesMirror().getAdditionalProperties();

    return additionalProperties.getSourcePosition();
  }

  @Override
  public JsFunction asFunction() {
    return this;
  }
}
