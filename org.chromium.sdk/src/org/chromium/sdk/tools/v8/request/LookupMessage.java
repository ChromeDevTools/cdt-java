package org.chromium.debug.core.tools.v8.request;

import java.util.List;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "lookup" request message.
 */
public class LookupRequestMessage extends V8DebugRequestMessage {

  /**
   * @param handles to look up
   */
  public LookupRequestMessage(List<Long> handles) {
    super(V8Command.LOOKUP.value);
    putArgument("handles", handles); //$NON-NLS-1$
  }

}
