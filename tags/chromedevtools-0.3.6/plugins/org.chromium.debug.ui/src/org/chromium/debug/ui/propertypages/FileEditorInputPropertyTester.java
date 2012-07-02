// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
