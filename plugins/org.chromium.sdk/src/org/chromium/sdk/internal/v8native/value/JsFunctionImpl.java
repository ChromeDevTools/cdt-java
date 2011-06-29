// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.JsFunction;
import org.chromium.sdk.Script;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.protocol.input.data.FunctionValueHandle;

/**
 * Generic implementation of {@link JsFunction}.
 */
class JsFunctionImpl extends JsObjectImpl implements JsFunction {
  private volatile TextStreamPosition openParenPosition = null;

  JsFunctionImpl(InternalContext context, String parentFqn, ValueMirror valueState) {
    super(context, parentFqn, valueState);
  }

  @Override
  public Script getScript() {
    final FunctionValueHandle functionValueHandle = getAdditionalPropertyData();
    Long scriptId = functionValueHandle.scriptId();
    if (scriptId == null) {
      return null;
    }
    DebugSession debugSession = getInternalContext().getDebugSession();
    return debugSession.getScriptManager().findById(scriptId);
  }

  @Override
  public TextStreamPosition getOpenParenPosition() {
    if (openParenPosition == null) {
      final FunctionValueHandle functionValueHandle = getAdditionalPropertyData();
      openParenPosition = new TextStreamPosition() {
        @Override public int getOffset() {
          return castLongToInt(functionValueHandle.position(), NO_POSITION);
        }
        @Override public int getLine() {
          return castLongToInt(functionValueHandle.line(), NO_POSITION);
        }
        @Override public int getColumn() {
          return castLongToInt(functionValueHandle.column(), NO_POSITION);
        }
        private int castLongToInt(Long objValue, int defaultValue) {
          if (objValue == null) {
            return defaultValue;
          } else {
            return objValue.intValue();
          }
        }
      };
    }
    return openParenPosition;
  }

  private FunctionValueHandle getAdditionalPropertyData() {
    return (FunctionValueHandle) getSubpropertiesMirror().getAdditionalPropertyData();
  }

  @Override
  public JsFunction asFunction() {
    return this;
  }
}
