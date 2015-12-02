// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;

/**
 * A symbolic interface that means "callback has been accepted and will be called sooner or later".
 * This interface comes together with {@link SyncCallback} interface. An asynchronous method
 * typically accepts {@link SyncCallback} parameter or throws an exception. By contract either
 * an exception is thrown or the caller rely on {@link SyncCallback} being called sooner or later.
 * This interface as a return type helps correctly write such code:
 * <ul>
 * <li>inside the method you won't mistakenly 'return' without calling some other
 *     {@link RelayOk}-returning method;
 * <li>outside the method you can't call blocking {@link CallbackSemaphore#acquireDefault} without
 *     actually calling someone {@link RelayOk}-returning.
 * </ul>
 * All this checks are done by compiler. The actual value of this type is not used.
 * <p>
 * This helps to prove that nobody will wait forever for a call-back that is never actually going
 * to be called.
 */
public interface RelayOk {
}
