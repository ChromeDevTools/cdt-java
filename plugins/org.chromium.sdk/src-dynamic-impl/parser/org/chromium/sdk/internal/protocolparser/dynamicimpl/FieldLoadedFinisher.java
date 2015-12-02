// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

/**
 * Defines object responsible for converting values saved in {@link ObjectData} to types
 * returned to user. It is necessary, because for json type fields we save {@link ObjectData}
 * rather than instance of the type itself.
 */
abstract class FieldLoadedFinisher {
  abstract Object getValueForUser(Object cachedValue);
}
