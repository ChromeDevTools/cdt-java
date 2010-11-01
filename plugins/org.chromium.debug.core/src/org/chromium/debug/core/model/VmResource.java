// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;

/**
 * A representation of V8 VM resource in a virtual project. The exact nature of the resource
 * is specified by metadata. However the exact type of metadata is only known in runtime.
 */
public interface VmResource {
  VmResourceId getId();

  Metadata getMetadata();

  IFile getVProjectFile();

  /**
   * @return a name of the file as a user sees it (i.e. without .chromium suffix)
   */
  String getLocalVisibleFileName();

  /**
   * Defines a logic behind a particular resource. Other interfaces should extend this interface
   * and provide more data.
   * As an example there might be imaginary resource that contains formatted sources of a
   * real resource.
   */
  interface Metadata {
  }

  /**
   * A special kind of {@link Metadata} that describes a resource directly linked to a resource
   * in VM. It holds one or several {@link Script}s. Typically resource is .js or .html file.
   */
  interface ScriptHolder extends Metadata {

    /**
     * @return script if this resource entirely consists of 1 script, otherwise throws exception
     * @throws UnsupportedOperationException if this resource does not entirely consists of 1 script
     * TODO(peter.rybin): redesign this method to normally work with html resources.
     */
    Script getSingleScript();
  }
}
