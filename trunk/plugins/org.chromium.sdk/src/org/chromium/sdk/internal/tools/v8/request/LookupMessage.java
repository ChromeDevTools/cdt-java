package org.chromium.sdk.internal.tools.v8.request;

import java.util.List;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "lookup" request message.
 */
public class LookupMessage extends DebuggerMessage {

  /**
   * @param handles to look up
   */
  public LookupMessage(List<Long> handles) {
    super(DebuggerCommand.LOOKUP.value);
    putArgument("handles", handles);
  }

}
