// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.efs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * A memory-based storage for browser scripts. All resource-related EFS
 * operations are delegated into here.
 */
public class ChromiumScriptStorage {

  /**
   * The filesystem root path.
   */
  // This one should go before INSTANCE.
  private static final IPath ROOT_PATH = new Path(null, ""); //$NON-NLS-1$

  private static final ChromiumScriptStorage INSTANCE = new ChromiumScriptStorage();

  public static ChromiumScriptStorage getInstance() {
    return INSTANCE;
  }

  private static abstract class CommonNode {
    final IPath path;

    final FileInfo info;

    final CommonNode parent;

    CommonNode(IPath path, FolderNode parent, boolean isDirectory) {
      this.path = path;
      this.parent = parent;
      this.info = new FileInfo(path.lastSegment());
      this.info.setDirectory(isDirectory);
      this.info.setExists(true);
      if (parent != null) {
        parent.add(this);
      }
    }

    String getName() {
      return info.getName();
    }

    boolean isFile() {
      return !info.isDirectory();
    }

  }

  private static class RootNode extends FolderNode {
    RootNode() {
      super(ROOT_PATH, null);
      if (parent != null) {
        throw new IllegalArgumentException("Parent must be null, was: " + parent); //$NON-NLS-1$
      }
    }

    @Override
    synchronized void add(CommonNode node) {
      if (node.isFile()) {
        throw new IllegalArgumentException("Cannot add files to the root"); //$NON-NLS-1$
      }
      super.add(node);
    }

  }

  /**
   * Contains other nodes.
   */
  private static class FolderNode extends CommonNode {
    private final Map<String, CommonNode> children =
        Collections.synchronizedMap(new HashMap<String, CommonNode>());

    FolderNode(IPath path, FolderNode parent) {
      super(path, parent, true);
    }

    void add(CommonNode node) {
      children.put(node.getName(), node);
    }

    void remove(String name) {
      // System.out.println(this.hashCode() + " removing " + name);
      CommonNode removedNode = children.remove(name);
      if (removedNode != null) {
        removedNode.info.setExists(false);
      }
    }
  }

  private static class FileNode extends CommonNode {
    private static final byte[] EMPTY_BYTES = new byte[0];

    protected volatile byte[] contents = EMPTY_BYTES;

    FileNode(IPath path, FolderNode parent) {
      super(path, parent, false);
    }

    synchronized InputStream getInputStream() {
      return new ByteArrayInputStream(contents);
    }

    synchronized OutputStream getOutputStream(final int options) {
      return new ByteArrayOutputStream() {
        @Override
        public void close() throws IOException {
          super.close();
          byte[] data;
          if ((options & EFS.APPEND) == 0) {
            data = this.toByteArray();
          } else {
            byte[] outputData = this.toByteArray();
            data = new byte[contents.length + this.size()];
            System.arraycopy(contents, 0, data, 0, contents.length);
            System.arraycopy(outputData, 0, data, contents.length, outputData.length);
          }
          setFileContents(data);
        }
      };

    }

    protected synchronized void setFileContents(byte[] data) {
      contents = data;
      info.setLength(data.length);
      info.setLastModified(System.currentTimeMillis());
      info.setExists(true);
    }

  }

  private static final String[] EMPTY_NAMES = new String[0];

  private final RootNode ROOT = new RootNode();

  private CommonNode find(IPath path) {
    if (path == null) {
      return null;
    }
    CommonNode currentNode = ROOT;
    // invariant: node(path[i]) is a folder
    for (int i = 0, length = path.segmentCount(); i < length; i++) {
      // > 1 segments
      if (currentNode == null || currentNode.isFile()) {
        // currentNode is not an existing folder
        return null;
      }
      // currentNode is a folder
      currentNode = ((FolderNode) currentNode).children.get(path.segment(i));
    }
    return currentNode;
  }

  String[] childNames(IPath path) {
    Map<String, CommonNode> childrenMap = childNodes(path);
    if (childrenMap == null) {
      return EMPTY_NAMES;
    }
    return childrenMap.keySet().toArray(EMPTY_NAMES);
  }

  OutputStream openOutputStream(IPath path, int options) throws CoreException {
    CommonNode node = find(path);
    if (node == null) { // file does not exist
      if (path.segmentCount() > 0) {
        CommonNode parent = find(getParentPath(path));
        if (parent != null && !parent.isFile()) {
          FileNode fileNode = createFile(path, parent);
          return fileNode.getOutputStream(options);
        } else {
          throw newCoreException("Bad store path (no parent folder), child=" + path, null); //$NON-NLS-1$
        }
      } else {
        throw newCoreException("Cannot open OutputStream for the Root", null); //$NON-NLS-1$
      }
    }
    if (node.isFile()) {
      return ((FileNode) node).getOutputStream(options);
    } else {
      throw newCoreException("Cannot open a directory for writing: " + path, null); //$NON-NLS-1$
    }
  }

  void mkdir(IPath path, int options) throws CoreException {
    CommonNode node = find(path);
    if (node != null || path.segmentCount() == 0) { // folder exists
      return;
    }
    IPath parentPath = getParentPath(path);
    // parentPath will not be null due to the check above
    CommonNode parentNode = find(parentPath);
    if ((options & EFS.SHALLOW) != 0) {
      IPath chainPath = ROOT_PATH;
      CommonNode childNode = null;
      parentNode = find(ROOT_PATH);
      for (int i = 0, length = path.segmentCount(); i < length; i++) {
        chainPath = chainPath.append(path.segment(i));
        childNode = find(chainPath);
        if (childNode == null) {
          createFolder(chainPath, parentNode);
          parentNode = childNode;
          continue;
        }
        if (childNode.isFile()) {
          throw newCoreException("File encountered in the path: " + chainPath, null); //$NON-NLS-1$
        }
      }
    } else {
      if (parentNode == null) {
        throw newCoreException("Parent does not exist, child=" + path, null); //$NON-NLS-1$
      }
      // not shallow and parent exists
      if (!parentNode.isFile()) {
        createFolder(path, parentNode);
      } else {
        throw newCoreException("Parent is a file: " + parentNode.path, null); //$NON-NLS-1$
      }
    }
  }

  void delete(IPath path, int options) throws CoreException {
    CommonNode parent = find(getParentPath(path));
    if (parent == null) {
      return;
    }
    if (parent.isFile()) {
      throw newCoreException("Parent is not a directory: " + getParentPath(path), null); //$NON-NLS-1$
    }
    FolderNode parentFolder = (FolderNode) parent;
    parentFolder.remove(path.lastSegment());
  }

  InputStream openInputStream(IPath path, int options) throws CoreException {
    CommonNode node = find(path);
    if (node == null) {
      throw newCoreException("File not found: " + path, null); //$NON-NLS-1$
    }
    if (!node.isFile()) {
      throw newCoreException("Cannot open InputStream on directory: " + path, null); //$NON-NLS-1$
    }
    return ((FileNode) node).getInputStream();
  }

  IFileInfo fetchInfo(IPath path, int options) {
    CommonNode node = find(path);
    if (node == null) {
      FileInfo fileInfo = new FileInfo(path.lastSegment());
      fileInfo.setExists(false);
      return fileInfo;
    } else {
      return node.info;
    }
  }

  void putInfo(IPath path, IFileInfo info, int options) throws CoreException {
    CommonNode node = find(path);
    if (node == null) {
      throw newCoreException("The store does not exist: " + path, null); //$NON-NLS-1$
    } else {
      if ((options & EFS.SET_ATTRIBUTES) != 0) {
        copyAttribute(info, node.info, EFS.ATTRIBUTE_ARCHIVE);
        copyAttribute(info, node.info, EFS.ATTRIBUTE_EXECUTABLE);
        copyAttribute(info, node.info, EFS.ATTRIBUTE_HIDDEN);
        copyAttribute(info, node.info, EFS.ATTRIBUTE_LINK_TARGET);
        copyAttribute(info, node.info, EFS.ATTRIBUTE_READ_ONLY);
      }
      if ((options & EFS.SET_LAST_MODIFIED) != 0) {
        node.info.setLastModified(info.getLastModified());
      }
    }
  }

  private static void copyAttribute(IFileInfo from, IFileInfo to, int attribute) {
    to.setAttribute(attribute, from.getAttribute(attribute));
  }

  private static CoreException newCoreException(String message, Throwable cause) {
    return new CoreException(
        new Status(Status.ERROR, ChromiumDebugPlugin.PLUGIN_ID, message, cause));
  }

  private static IPath getParentPath(IPath path) {
    if (path.segmentCount() == 0) {
      return null;
    }
    return path.removeLastSegments(1);
  }

  private static void createFolder(IPath path, CommonNode parentNode) {
    new FolderNode(path, (FolderNode) parentNode);
  }

  private static FileNode createFile(IPath path, CommonNode parent) {
    return new FileNode(path, (FolderNode) parent);
  }

  private Map<String, CommonNode> childNodes(IPath parent) {
    CommonNode node = find(parent);
    if (node == null || node.isFile()) {
      return null;
    }
    return ((FolderNode) node).children;
  }

}
