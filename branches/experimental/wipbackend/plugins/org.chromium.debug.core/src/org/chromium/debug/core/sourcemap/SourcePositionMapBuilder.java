// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import org.chromium.debug.core.model.VmResourceId;

/**
 * A builder for {@link SourcePositionMap}. The builder pattern is reduced to a "single builder --
 * single product" case. A product can be used and built in turn.
 */
public interface SourcePositionMapBuilder {

  /**
   * Returns an instance of map. The instance is permanent, the builder does not support
   * multiple products.
   */
  SourcePositionMap getSourcePositionMap();

  /**
   * Adds a new mapping between 2 resource sections.
   * @param originalSection a section of "original" resource
   * @param vmSection a section of "vm" resource
   * @param fromOriginalToVmSectionMapping defines internal mapping inside "original" and "vm"
   *     sections
   * @return a handle that could be used to control a created mapping
   */
  MappingHandle addMapping(ResourceSection originalSection, ResourceSection vmSection,
      TextSectionMapping fromOriginalToVmSectionMapping) throws CannotAddException;

  /**
   * A handle that gives control over created mapping.
   */
  interface MappingHandle {
    void delete();
  }

  class CannotAddException extends Exception {
    CannotAddException() {
    }

    CannotAddException(String message, Throwable cause) {
      super(message, cause);
    }

    CannotAddException(String message) {
      super(message);
    }

    CannotAddException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Defines a section of a source file, contains a resource id and start and end point in text.
   * Line/column numbers are always 0-based.
   */
  class ResourceSection {
    private final VmResourceId resourceId;
    private final TextSectionMapping.TextPoint start;
    private final TextSectionMapping.TextPoint end;

    public ResourceSection(VmResourceId resourceId, int startLine, int startColumn,
        int endLine, int endColumn) {
      this.resourceId = resourceId;
      this.start = new TextSectionMapping.TextPoint(startLine, startColumn);
      this.end = new TextSectionMapping.TextPoint(endLine, endColumn);
    }

    public VmResourceId getResourceId() {
      return resourceId;
    }

    TextSectionMapping.TextPoint getStart() {
      return start;
    }

    TextSectionMapping.TextPoint getEnd() {
      return end;
    }
  }
}
