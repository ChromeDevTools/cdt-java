// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import org.chromium.sdk.Script;
import org.chromium.sdk.util.BasicUtil;

/**
 * Id of resources loaded in V8 VM. We only know that they may have name (typically filename or
 * URL) and/or numerical id.
 */
public class VmResourceId {
  public static VmResourceId forScript(final Script script) {
    return new VmResourceId(script.getName(), script.getId());
  }

  private final String name;
  private final Object id;

  public VmResourceId(String name, Object id) {
    this.name = name;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public Object getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return BasicUtil.hashCode(getName()) + 31 * BasicUtil.hashCode(getId());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof VmResourceId == false) {
      return false;
    }
    VmResourceId other = (VmResourceId) obj;

    return BasicUtil.eq(this.name, other.name) && BasicUtil.eq(this.id, other.id);
  }

  @Override
  public String toString() {
    return "<" + name + " : " + id + ">";
  }

  public String getVisibleName() {
    String name = getName();
    if (name != null) {
      return name;
    }
    return "<unnamed # " + getId() + ">";
  }
}
