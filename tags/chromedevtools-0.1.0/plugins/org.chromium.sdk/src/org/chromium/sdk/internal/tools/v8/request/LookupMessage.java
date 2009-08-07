package org.chromium.sdk.internal.tools.v8.request;

import java.util.List;

import org.chromium.sdk.internal.ContextToken;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "lookup" request message.
 */
public class LookupMessage extends DebuggerMessage {

  /**
   * @param handles to look up
   * @param inlineRefs whether to inline references
   * @param token the context validity token
   */
  public LookupMessage(List<Long> handles, Boolean inlineRefs, ContextToken token) {
    super(DebuggerCommand.LOOKUP.value, token);
    putArgument("handles", handles);
    putArgument("inlineRefs", inlineRefs);
  }

}
