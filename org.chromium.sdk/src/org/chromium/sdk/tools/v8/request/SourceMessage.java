// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.request;

import org.chromium.debug.core.tools.v8.V8Command;

/**
 * Represents "source" V8 request message.
 */
public class SourceRequestMessage extends V8DebugRequestMessage {

  /**
   * @param frame
   *          frame number (default is selected frame)
   * @param fromLine
   *          from line within the source, default is line 0
   * @param toLine
   *          to line within the source this (line is not included in the
   *          result), default is the number of lines in the script
   */
  public SourceRequestMessage(Integer frame, Integer fromLine, Integer toLine) {
    super(V8Command.SOURCE.value);
    putArgument("frame", frame); //$NON-NLS-1$
    putArgument("fromLine", fromLine); //$NON-NLS-1$
    putArgument("toLine", toLine); //$NON-NLS-1$
  }
}
