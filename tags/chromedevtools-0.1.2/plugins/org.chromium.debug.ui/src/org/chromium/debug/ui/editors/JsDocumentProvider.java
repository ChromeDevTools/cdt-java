// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
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

}
