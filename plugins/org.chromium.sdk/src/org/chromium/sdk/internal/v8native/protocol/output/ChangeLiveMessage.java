// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.output;

import org.chromium.sdk.internal.v8native.DebuggerCommand;

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
