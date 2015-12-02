// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.V8CommandProcessor.V8HandlerCallback;

/**
 * API to asynchronous message sender that supports callbacks.
 * @param <MESSAGE> type of message supported
 * @param <EX> exception that may be thrown synchronously.
 */
public interface V8CommandSender<MESSAGE, EX extends Exception> {
  RelayOk sendV8CommandAsync(MESSAGE message, boolean isImmediate,
      V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) throws EX;
}
