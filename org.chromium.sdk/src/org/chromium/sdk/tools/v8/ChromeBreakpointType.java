// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.debug.core.tools.v8;

/**
 * Known V8 breakpoint types.
 */
public enum BreakpointType {
  FUNCTION("function"), //$NON-NLS-1$
  SCRIPT("script"), //$NON-NLS-1$
  ;

  public String value;

  BreakpointType(String value) {
    this.value = value;
  }
}
