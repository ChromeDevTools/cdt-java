package org.chromium.sdk.internal.tools.v8.request;

import java.util.List;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "lookup" request message.
 */
public class LookupMessage extends DebuggerMessage {

  /**
   * @param handles to look up
   * @param inlineRefs whether to inline references
   */
  public LookupMessage(List<Long> handles, Boolean inlineRefs) {
    this(handles, inlineRefs, null);
  }

  public LookupMessage(List<Long> handles, Boolean inlineRefs, Long maxStringLength) {
    super(DebuggerCommand.LOOKUP.value);
    putArgument("handles", handles);
    putArgument("inlineRefs", inlineRefs);
    if (maxStringLength != null) {
      putArgument("maxStringLength", maxStringLength);
    }
  }

}
