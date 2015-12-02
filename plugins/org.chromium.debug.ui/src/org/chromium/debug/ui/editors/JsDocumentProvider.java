// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.editors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

/**
 * Provides JavaScript content and sets up the document partitioner.
 */
public class JsDocumentProvider extends FileDocumentProvider {

  @Override
  protected IDocument createDocument(Object element) throws CoreException {
    IDocument doc = super.createDocument(element);
    if (doc != null) {
      IDocumentPartitioner partitioner = new FastPartitioner(
          new JsPartitionScanner(), JsPartitionScanner.PARTITION_TYPES);
      partitioner.connect(doc);
      doc.setDocumentPartitioner(partitioner);
    }
    return doc;
  }

  /**
   * Alternative implementation of the method that does not require file to be a physical file.
   */
  @Override
  public boolean isDeleted(Object element) {
    if (element instanceof IFileEditorInput) {
      IFileEditorInput input= (IFileEditorInput) element;

      IProject project = input.getFile().getProject();
      if (project != null && !project.exists()) {
        return true;
      }

      return !input.getFile().exists();
    }
    return super.isDeleted(element);
  }

}
