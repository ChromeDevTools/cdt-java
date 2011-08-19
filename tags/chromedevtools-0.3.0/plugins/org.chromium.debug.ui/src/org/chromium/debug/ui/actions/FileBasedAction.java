// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * A utility class for creating actions that work with files in selection.
 * User should extend either {@link Single} or {@link Multiple} nested class.
 * Custom file filter is supported.
 */
class FileBasedAction {

  /**
   * A base class for actions that are enabled for a single file. User provides
   * a {@link FileFilter} that may additionally convert low-level {@link IFile}
   * to a user type RES.
   * @param <RES> a user-specified type that corresponds to a file
   */
  static abstract class Single<RES> extends SelectionBasedAction.Single<RES>  {
    private final FileFilter<RES> fileFilter;

    /**
     * @param allowMutipleSelection see javadoc for {@link SelectionBasedAction.Single}
     */
    protected Single(boolean allowMutipleSelection, FileFilter<RES> fileFilter) {
      super(allowMutipleSelection);
      this.fileFilter = fileFilter;
    }

    @Override
    protected RES castElement(Object element) {
      List<RES> files = readFilesFromSelectionObject(element, fileFilter);
      if (files.size() != 1) {
        return null;
      }
      return files.get(0);
    }
  }

  /**
   * A base class for actions that are enabled for several files. User provides
   * a {@link FileFilter} that may additionally convert low-level {@link IFile}
   * to a user type RES.
   * @param <RES> a user-specified type that corresponds to a file
   */
  static abstract class Multiple<RES> extends SelectionBasedAction.Multiple<RES> {
    private final FileFilter<RES> fileFilter;

    protected Multiple(FileFilter<RES> fileFilter) {
      this.fileFilter = fileFilter;
    }

    @Override
    protected List<? extends RES> readSelection(
        IStructuredSelection selection) {
      List<RES> files = new ArrayList<RES>(1);
      for (Iterator<?> it = selection.iterator(); it.hasNext(); ) {
        Object element = it.next();
        files.addAll(readFilesFromSelectionObject(element, fileFilter));
      }
      return files;
    }
  }

  /**
   * A filter that converts input file into a user type T or null.
   * @param <T>
   */
  static abstract class FileFilter<T> {
    /**
     * @return user data or null if the file should be ignored
     */
    abstract T accept(IFile file);
  }

  private static <RES> List<RES> readFilesFromSelectionObject(Object element,
      FileFilter<RES> fileFilter) {
    if (element instanceof ResourceMapping == false) {
      return null;
    }
    ResourceMapping resourceMapping = (ResourceMapping) element;
    return readFiles(resourceMapping, fileFilter);
  }

  private static <RES> List<RES> readFiles(ResourceMapping resourceMapping,
      final FileFilter<RES> fileFilter) {
    final List<RES> fileList = new ArrayList<RES>(1);
    IResourceVisitor visitor = new IResourceVisitor() {
      public boolean visit(IResource resource) throws CoreException {
        if (resource instanceof IFile == false) {
          return true;
        } else {
          IFile file = (IFile) resource;
          RES res = fileFilter.accept(file);
          if (res != null) {
            fileList.add(res);
          }
          return false;
        }
      }
    };
    try {
      resourceMapping.accept(null, visitor, null);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
    return fileList;
  }
}
