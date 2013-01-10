// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.Map;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

/**
 * Represents a "scope" request message.
 */
public class ScopeMessage extends DebuggerMessage {

  public ScopeMessage(Ref scopeRef) {
    super(DebuggerCommand.SCOPE.value);
    scopeRef.fillJson(getArguments());
    putArgument("inlineRefs", true);
  }

  public static class Ref {
    private final int scopeNumber;
    private final Host host;

    public Ref(int scopeNumber, Host host) {
      this.scopeNumber = scopeNumber;
      this.host = host;
    }
    void fillJson(Map<? super String, ? super Object> object) {
      object.put("number", scopeNumber);
      host.fillJson(object);
    }
  }

  public static abstract class Host {
    public static Host createFrame(final int frameNumber) {
      return new Host() {
        @Override void fillJson(Map<? super String, ? super Object> object) {
          object.put("frameNumber", frameNumber);
        }
      };
    }
    public static Host createFunction(final long functionHandle) {
      return new Host() {
        @Override void fillJson(Map<? super String, ? super Object> object) {
          object.put("functionHandle", functionHandle);
        }
      };
    }

    abstract void fillJson(Map<? super String, ? super Object> object);
  }
}
