// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

/**
 * Defines object responsible for converting values saved in {@link ObjectData} to types
 * returned to user. It is necessary, because for json type fields we save {@link ObjectData}
 * rather than instance of the type itself.
 */
abstract class FieldLoadedFinisher {
  abstract Object getValueForUser(Object cachedValue);
}
