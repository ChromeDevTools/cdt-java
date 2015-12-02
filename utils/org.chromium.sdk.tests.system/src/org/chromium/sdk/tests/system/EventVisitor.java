// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.tests.system;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.Script;
import org.chromium.sdk.TabDebugEventListener;

/**
 * A visitor for events from {@link DebugEventListener} and {@link TabDebugEventListener}.
 */
interface EventVisitor<RES> {
  RES visitClosed() throws SmokeException;
  RES visitNavigated(String newUrl) throws SmokeException;
  RES visitDisconnected() throws SmokeException;
  RES visitScriptLoaded(Script newScript) throws SmokeException;
  RES visitSuspended(DebugContext context) throws SmokeException;
  RES visitResumed() throws SmokeException;
  RES visitScriptCollected(Script script) throws SmokeException;
}
