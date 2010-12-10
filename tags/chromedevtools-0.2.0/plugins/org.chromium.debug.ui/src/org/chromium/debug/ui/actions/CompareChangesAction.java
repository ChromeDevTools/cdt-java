// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;

/**
 * TODO(peter.rybin): consider removing this class as it only holds a nested class
 * used elsewhere.
 * TODO(peter.rybin): localize text strings properly.
 */
public class CompareChangesAction {
  public static class LiveEditCompareInput extends CompareEditorInput {
    private final IFile file;
    private final VmResource script;
    private final VmResource.ScriptHolder scriptHolder;

    public LiveEditCompareInput(ScriptTargetMapping filePair) {
      this(filePair.getFile(), filePair.getVmResource(), filePair.getScriptHolder());
    }

    public LiveEditCompareInput(IFile file, VmResource vmResource,
        VmResource.ScriptHolder scriptHolder) {
      super(createCompareConfiguration());
      this.file = file;
      this.script = vmResource;
      this.scriptHolder = scriptHolder;
    }

    private static CompareConfiguration createCompareConfiguration() {
      return new CompareConfiguration();
    }

    @Override
    public DiffNode prepareInput(IProgressMonitor monitor) {

      abstract class CompareItem implements ITypedElement, IStreamContentAccessor,
          IModificationDate {
        public Image getImage() {
          return null;
        }
        public String getType() {
          return TEXT_TYPE;
        }
        public long getModificationDate() {
          return 0;
        }
      }
      CompareItem left = new CompareItem() {
        public String getName() {
          return "Local file " + file.getName(); //$NON-NLS-1$
        }
        public InputStream getContents() throws CoreException {
          return file.getContents();
        }
      };
      CompareItem right = new CompareItem() {
        public String getName() {
          String fileInVmName = script.getId().getEclipseSourceName();
          return "File in VM " + fileInVmName; //$NON-NLS-1$
        }
        public InputStream getContents() throws CoreException {
          String source = scriptHolder.getSingleScript().getSource();
          return new ByteArrayInputStream(source.getBytes());
        }
      };

      DiffNode diffNode = new DiffNode(null, Differencer.PSEUDO_CONFLICT, null, left, right);
      return diffNode;
    }
  }
}
