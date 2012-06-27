// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.value.ValueLoader;
import org.chromium.sdk.internal.v8native.value.ValueLoaderImpl;

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

  CallFrameImpl getTopFrameImpl();

  /**
   * Sends V8 command message provided this context is still valid. There is no
   * way of making sure context will be valid via this API.
   * @throws ContextDismissedCheckedException if context is not valid anymore
   */
  @Override
  RelayOk sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
      V8CommandProcessor.V8HandlerCallback commandCallback, SyncCallback syncCallback)
      throws ContextDismissedCheckedException;

  class ContextDismissedCheckedException extends Exception {
  }

  /**
   * {@link ValueLoader} makes sense only for a particular context.
   * @return {@link ValueLoader} of this context
   */
  ValueLoaderImpl getValueLoader();

  UserContext getUserContext();

  /**
   * An internal extension to {@link DebugContext} interface. This is an object that is
   * passed to user (unlike the {@link InternalContext} which is a separate object).
   */
  interface UserContext extends DebugContext {
    InternalContext getInternalContext();

    ContextBuilder.ExpectingBacktraceStep createReloadBacktraceStep();
  }
}
