// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.chromium.debug.core.model.StackFrame;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;

/**
 * Translates a StackFrame into a source filename.
 */
public class SourceLookupParticipant extends AbstractSourceLookupParticipant {

  public String getSourceName(Object object) throws CoreException {
    if (object instanceof StackFrame) {
      return ((StackFrame) object).getExternalFileName();
    }

    return null;
  }
}