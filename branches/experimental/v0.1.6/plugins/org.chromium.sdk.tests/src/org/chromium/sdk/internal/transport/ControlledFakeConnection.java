// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.transport;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.chromium.sdk.internal.transport.Message;

public class ControlledFakeConnection extends FakeConnection {

  private final Queue<Message> messages = new ConcurrentLinkedQueue<Message>();
  private boolean isContinuousProcessing = false;

  public ControlledFakeConnection(ChromeStub responder) {
    super(responder);
  }

  @Override
  public void send(Message message) {
    if (isContinuousProcessing) {
      super.send(message);
    } else {
      messages.add(message);
    }
  }

  public void setContinuousProcessing(boolean enabled) {
    this.isContinuousProcessing = enabled;
  }

  public void processMessages(int count) {
    for (int i = 0; i < count; i++) {
      Message polled = messages.poll();
      if (polled == null) {
        break;
      }
      super.send(polled);
    }
  }
}