// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.Script;

/**
 * Id of resources loaded in V8 VM. We only know that they may have name (typically filename or
 * URL) or numerical id instead. This class reflects this.
 * The class also contains several utility methods that probably should be separated in the future.
 */
public class VmResourceId {

  public static VmResourceId forName(String scriptName) {
    return new VmResourceId(scriptName);
  }

  public static VmResourceId forId(long scriptId) {
    return new VmResourceId(Long.valueOf(scriptId));
  }

  public static VmResourceId forScript(Script script) {
    if (script.getName() != null) {
      return forName(script.getName());
    } else {
      return forId(script.getId());
    }
  }

  private final Object value;

  private VmResourceId(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Null id value"); //$NON-NLS-1$
    }
    this.value = value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VmResourceId == false) {
      return false;
    }
    VmResourceId other = (VmResourceId) obj;
    return this.value.equals(other.value);
  }

  /**
   * @return parameter for {@link JavascriptVm#setBreakpoint} method.
   */
  public Breakpoint.Target getTargetForBreakpoint() {
    if (value instanceof String) {
      return new Breakpoint.Target.ScriptName((String) value);
    } else {
      return new Breakpoint.Target.ScriptId((Long) value);
    }
  }

  String createFileNameTemplate(boolean isEval) {
    if (value instanceof String) {
      return value.toString();
    } else {
      if (isEval) {
        return "<eval #" + value + ">"; //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        return "<no name #" + value + ">"; //$NON-NLS-1$ //$NON-NLS-2$
      }
    }

  }

  @Override
  public String toString() {
    return getEclipseSourceName();
  }

  /**
   * @return source name that is suitable for Eclipse debug source lookup.
   */
  public String getEclipseSourceName() {
    if (value instanceof String) {
      String stringValue = (String) value;
      if (stringValue.startsWith("#")) {
        // Quote it.
        stringValue = "#" + stringValue;
      }
      return stringValue;
    } else {
      return "#" + value;
    }
  }

  public static VmResourceId parseString(String name) {
    if (name.startsWith("##")) {
      return VmResourceId.forName(name.substring(1));
    } else if (name.startsWith("#")) {
      return VmResourceId.forId(Long.parseLong(name.substring(1)));
    } else {
      return VmResourceId.forName(name);
    }
  }

  /**
   * @return source name or null
   */
  public String getSourceName() {
    if (value instanceof String) {
      return (String) value;
    } else {
      return null;
    }
  }
}
