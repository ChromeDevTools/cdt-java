// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import org.chromium.sdk.internal.tools.v8.DebuggerCommand;

/**
 * Represents a "changelive" experimental V8 request message.
 */
public class ChangeLiveMessage extends ContextlessDebuggerMessage {
  public ChangeLiveMessage(long scriptId, String newSource, Boolean previewOnly) {
    super(DebuggerCommand.CHANGELIVE.value);
    putArgument("script_id", scriptId);
    putArgument("new_source", newSource);
    if (previewOnly != null) {
      putArgument("preview_only", previewOnly);
    }
  }
}
