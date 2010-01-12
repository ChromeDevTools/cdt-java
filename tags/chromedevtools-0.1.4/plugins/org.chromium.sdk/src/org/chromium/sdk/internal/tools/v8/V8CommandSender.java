// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor.V8HandlerCallback;

/**
 * API to asynchronous message sender that supports callbacks.
 * @param <MESSAGE> type of message supported
 * @param <EX> exception that may be thrown synchronously.
 */
public interface V8CommandSender<MESSAGE, EX extends Exception> {
  void sendV8CommandAsync(MESSAGE message, boolean isImmediate,
      V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) throws EX;
}
