// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * Enables SourceLookupParticipant.
 */
public class SourceLookupDirector extends AbstractSourceLookupDirector {

  public void initializeParticipants() {
    addParticipants(
        new ISourceLookupParticipant[] { new SourceLookupParticipant() });
  }

}
