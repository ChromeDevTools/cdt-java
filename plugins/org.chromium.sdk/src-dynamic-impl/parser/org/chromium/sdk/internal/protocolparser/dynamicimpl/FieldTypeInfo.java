// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.FileScope;

/**
 * Information about a field type that is used in Java code generation.
 */
interface FieldTypeInfo {
  void appendValueTypeNameJava(FileScope scope);
}