package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.ContextToken;

public class ContextlessDebuggerMessage extends DebuggerMessage {
  public ContextlessDebuggerMessage(String command, ContextToken token) {
    super(command, token);
  }
}
