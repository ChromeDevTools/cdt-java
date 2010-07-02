// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "source" V8 request message.
 */
public class SourceMessage extends ContextlessDebuggerMessage {

  /**
   * @param frame number. Nullable, default is the selected frame
   * @param fromLine nullable start line within the source. Default is line 0
   * @param toLine nullable end line within the source (this line is not included in the
   *        result). Default is the number of lines in the script
   */
  public SourceMessage(Integer frame, Integer fromLine, Integer toLine) {
    super(DebuggerCommand.SOURCE.value);
    putArgument("frame", frame);
    putArgument("fromLine", fromLine);
    putArgument("toLine", toLine);
  }
}
