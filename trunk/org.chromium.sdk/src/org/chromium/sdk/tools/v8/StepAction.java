// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.debug.core.tools.v8;

/**
 * Known V8 VM step actions.
 */
public enum StepAction {
  IN("in"), //$NON-NLS-1$
  NEXT("next"), //$NON-NLS-1$
  OUT("out"), //$NON-NLS-1$
  ;

  public String value;

  StepAction(String value) {
    this.value = value;
  }
}
