// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser;

/**
 * A base interface for JSON subtype interface. This inheritance serves 2 purposes:
 * it declares base type (visible to human and to interface analyzer) and adds {@link #getSuper()}
 * getter that may be directly used in programs.
 */
public interface JsonSubtype<BASE> {
  BASE getSuper();
}
