// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JsFunction;
import org.chromium.sdk.Script;

/**
 * Generic implementation of {@link JsFunction}.
 */
class JsFunctionImpl extends JsObjectImpl implements JsFunction {
  JsFunctionImpl(CallFrameImpl callFrame, String parentFqn, ValueMirror valueState) {
    super(callFrame, parentFqn, valueState);
  }

  public Script getScript() {
    FunctionAdditionalProperties additionalProperties =
        (FunctionAdditionalProperties) getSubpropertiesMirror().getAdditionalProperties();

    int scriptId = additionalProperties.getScriptId();
    if (scriptId == FunctionAdditionalProperties.NO_SCRIPT_ID) {
      return null;
    }
    DebugSession debugSession = getCallFrame().getInternalContext().getDebugSession();
    return debugSession.getScriptManager().findById(Long.valueOf(scriptId));
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
