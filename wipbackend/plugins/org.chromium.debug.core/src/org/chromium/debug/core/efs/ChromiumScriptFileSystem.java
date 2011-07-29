// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.efs;

import java.net.URI;
import java.net.URISyntaxException;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * An in-memory filesystem for remote scripts.
 * The supported URLs are {@code chromiumdebug:///resource_path}.
 */
public class ChromiumScriptFileSystem extends FileSystem {

  /** All file URLs in this filesystem have this scheme. */
  private static final String CHROMIUMDEBUG_SCHEME = "chromiumdebug"; //$NON-NLS-1$

  @Override
  public IFileStore getStore(URI uri) {
    return new ChromiumScriptFileStore(this, toPath(uri));
  }

  /**
   * Constructs a URI by a path.
   *
   * @param path of a filesystem resource
   * @return a URI corresponding to the given {@code path}
   */
  public static URI getFileStoreUri(IPath path) {
    try {
      return new URI(CHROMIUMDEBUG_SCHEME, null, path.toPortableString(), null);
    } catch (URISyntaxException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  public static boolean isChromiumDebugURI(URI uri) {
    return CHROMIUMDEBUG_SCHEME.equals(uri.getScheme());
  }

  /**
   * Converts a chromiumdebug FS FileStore URI into a path relative to the FS root.
   *
   * @param uri to convert
   * @return the path corresponding to the uri
   */
  static IPath toPath(URI uri) {
    return Path.fromPortableString(uri.getPath()).setDevice(null);
  }

  /**
   * Converts a chromiumdebug FS FileStore path into a FS URI.
   *
   * @param path to convert
   * @return the URI corresponding to the given path
   */
  static URI toUri(IPath path) {
    try {
      return new URI(CHROMIUMDEBUG_SCHEME, null, path.toPortableString(), null);
    } catch (URISyntaxException e) {
      return null;
    }
  }

}
