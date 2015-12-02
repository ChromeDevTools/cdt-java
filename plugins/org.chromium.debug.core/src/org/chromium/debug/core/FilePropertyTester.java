// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;

/**
 * Implementation of additional properties for files that are JavaScript sources.
 */
public abstract class FilePropertyTester extends PropertyTester {
  private static final String IS_JS_FILE_PROPERTY = "isJsFile"; //$NON-NLS-1$

  public static class ForFile extends FilePropertyTester {
    @Override
    protected IFile extractFile(Object receiver) {
      return (IFile) receiver;
    }
  }

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    IFile file = extractFile(receiver);
    if (IS_JS_FILE_PROPERTY.equals(property)) {
      return testIsJsFile(file);
    } else {
      throw new RuntimeException("Unrecognized property name"); //$NON-NLS-1$
    }
  }

  protected abstract IFile extractFile(Object receiver);

  private static boolean testIsJsFile(IFile file) {
    return ChromiumDebugPluginUtil.SUPPORTED_EXTENSIONS.contains(file.getFileExtension());
  }
}
