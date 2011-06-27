// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

/**
 * VM resource reference. It differs from {@link VmResourceId} in that it may be much less
 * precise, e.g. RegExp over script name.
 * {@link VmResourceRef} is an intermediate value derived from user IDE file and used for
 * locating actual resource locally or on remote VM (when setting a breakpoint).
 * This is an algebraic type. See {@link Visitor} and factory methods for details.
 */
public abstract class VmResourceRef {

  /**
   * A GoF Visitor interface for algebraic type {@link VmResourceRef}.
   */
  public interface Visitor<R> {
    R visitResourceId(VmResourceId resourceId);
  }

  /**
   * Creates VmResourceRef that is based on {@link VmResourceId}.
   */
  public static VmResourceRef forVmResourceId(VmResourceId vmResourceId) {
    return new ForVmResourceId(vmResourceId);
  }

  private static final class ForVmResourceId extends VmResourceRef {
    private final VmResourceId vmResourceId;

    ForVmResourceId(VmResourceId vmResourceId) {
      this.vmResourceId = vmResourceId;
    }

    @Override public <R> R accept(Visitor<R> visitor) {
      return visitor.visitResourceId(vmResourceId);
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      ForVmResourceId that = (ForVmResourceId) obj;
      return this.vmResourceId.equals(that.vmResourceId);
    }

    @Override
    public int hashCode() {
      return vmResourceId.hashCode();
    }
  }

  public abstract <R> R accept(Visitor<R> visitor);
}
