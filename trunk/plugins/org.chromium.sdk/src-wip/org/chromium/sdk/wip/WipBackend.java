// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.wip;

/**
 * Abstract interface to WIP implementation, that is delivered in a separate library.
 * It allows to have several versions of implementation in the system at the same time,
 * which may be needed because WIP is not stable yet and is evolving quite rapidly.
 * <p>
 * A particular set-up should choose it's own way to get backed instances. For example
 * Eclipse may use its extension point mechanism.
 */
public interface WipBackend {

  /**
   * @return a unique name of backend implementation
   */
  String getId();

  /**
   * @return a human-readable implementation description that should help to tell what protocol
   *     version it supports
   */
  String getDescription();
}
