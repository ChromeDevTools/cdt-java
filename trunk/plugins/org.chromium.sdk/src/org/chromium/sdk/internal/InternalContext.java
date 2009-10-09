// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.List;

import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8CommandSender;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.json.simple.JSONObject;

/**
 * Internal API to DebugContext implementation. The actual object might
 * be hidden deep inside, so we need an interface. Do not try to cast
 * DebugContext to this interface -- technically they might be different
 * objects.
 */
public interface InternalContext extends V8CommandSender<DebuggerMessage,
    InternalContext.ContextDismissedCheckedException> {
  /**
   * Context belongs to a particular {@code DebugSession}.
   * @return DebugSession this context belongs to
   */
  DebugSession getDebugSession();

  ContextBuilder getContextBuilder();

  // TODO(peter.rybin): document this
  boolean isValid();

  /**
   * Handle manager makes sense only for a particular context.
   * @return HandleManager of this context
   */
  HandleManager getHandleManager();

  List<PropertyReference> computeLocals(JSONObject frame);

  List<ScopeMirror> computeScopes(JSONObject frame);

  CallFrameImpl getTopFrameImpl();

  /**
   * Sends V8 command message provided this context is still valid. There is no
   * way of making sure context will be valid via this API.
   * @throws ContextDismissedCheckedException if context is not valid anymore
   */
  void sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
      V8CommandProcessor.V8HandlerCallback commandCallback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException;

  class ContextDismissedCheckedException extends Exception {
  }

  ValueLoader getValueLoader();
}
