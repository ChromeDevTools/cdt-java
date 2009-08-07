// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Connection.NetListener;

/**
 * An implementor can provide a way to respond to a certain Message (naturally,
 * instead of Google Chrome).
 */
public interface ChromeStub {

  /**
   * Constructs responses to client requests in place of Google Chrome.
   *
   * @param requestMessage to respond to
   * @return a response message
   */
  Message respondTo(Message requestMessage);

  /**
   * Accepts the NetListener instance set for the host FakeConnection. The
   * listener can be used to report V8 debugger events without explicit requests
   * from the client.
   *
   * @param listener set for the host FakeConnection
   */
  void setNetListener(NetListener listener);
}
