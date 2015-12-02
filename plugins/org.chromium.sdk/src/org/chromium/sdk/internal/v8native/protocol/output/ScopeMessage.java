// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

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
