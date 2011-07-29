// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

/**
 * A symbolic interface that means "callback has been accepted and will be called sooner or later".
 * This interface comes together with {@link SyncCallback} interface. A method typically
 * accepts {@link SyncCallback} parameter or throws an exception. Unless exception was
 * thrown, caller rely on {@link SyncCallback} being called sooner or later. This interface
 * helps correctly writing program in this paradigm:
 * <ul>
 * <li>caller can safely issue waiting operations after getting its value, waiting accepts this
 * value (like {@link CallbackSemaphore#acquireDefault()};
 * <li>implementer must finish the method with something that does a proper relay, otherwise
 * compiler would fail.
 * </ul>
 * The actual value of this type is not used.
 */
public interface RelayOk {
}
