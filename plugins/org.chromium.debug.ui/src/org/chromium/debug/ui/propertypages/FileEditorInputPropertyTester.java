// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.propertypages;

import org.chromium.debug.core.FilePropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IFileEditorInput;

/**
 * Implementation of additional properties for {@link IFileEditorInput} that holds JavaScript
 * sources.
 */
public class FileEditorInputPropertyTester extends FilePropertyTester {
  @Override
  protected IFile extractFile(Object receiver) {
    IFileEditorInput editorInput = (IFileEditorInput) receiver;
    return editorInput.getFile();
  }
}
