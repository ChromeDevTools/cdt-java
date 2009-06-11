// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.efs;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A script file store. Delegates all operations to the ChromiumScriptStorage
 * instance.
 */
public class ChromiumScriptFileStore extends FileStore {

  /** The filesystem storage. */
  private static final ChromiumScriptStorage STORAGE = ChromiumScriptStorage.getInstance();

  /** The host filesystem of this resource. */
  private final ChromiumScriptFileSystem fileSystem;

  /** The resource path in the filesystem. */
  private final IPath path;

  /**
   * Constructs a proxy for a real resource (which might not exist).
   *
   * @param fileSystem that stores the resource
   * @param path of the resource
   */
  public ChromiumScriptFileStore(ChromiumScriptFileSystem fileSystem, IPath path) {
    this.fileSystem = fileSystem;
    this.path = path;
  }

  @Override
  public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
    return STORAGE.childNames(this.path);
  }

  @Override
  public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
    return STORAGE.fetchInfo(path, options);
  }

  @Override
  public IFileStore getChild(String name) {
    return fileSystem.getStore(path.append(name));
  }

  @Override
  public String getName() {
    if (path.isEmpty()) {
      return "ROOT"; //$NON-NLS-1$
    }
    return path.lastSegment();
  }

  @Override
  public IFileStore getParent() {
    if (path.isEmpty()) {
      return null;
    }
    return new ChromiumScriptFileStore(fileSystem, path.removeLastSegments(1));
  }

  @Override
  public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
    return STORAGE.openInputStream(path, options);
  }

  @Override
  public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
    return STORAGE.openOutputStream(path, options);
  }

  @Override
  public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
    STORAGE.mkdir(path, options);
    return this;
  }

  @Override
  public URI toURI() {
    return ChromiumScriptFileSystem.getFileStoreUri(path);
  }

  @Override
  public void delete(int options, IProgressMonitor monitor) throws CoreException {
    STORAGE.delete(path, options);
  }

  @Override
  public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
    STORAGE.putInfo(path, info, options);
  }

}
