// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Breakpoint.Target;
import org.chromium.sdk.BreakpointTypeExtension;
import org.chromium.sdk.JavascriptVm;

/**
 * Common implementation of script regexp breakpoint target. User cannot access this class,
 * instead {@link JavascriptVm#getBreakpointTypeExtension()} provides the constructor
 * (or null if this target type is not supported).
 */
public class ScriptRegExpBreakpointTarget extends Target {
  private final String regExp;

  public ScriptRegExpBreakpointTarget(String regExp) {
    this.regExp = regExp;
  }

  @Override
  public <R> R accept(Visitor<R> visitor) {
    if (visitor instanceof BreakpointTypeExtension.ScriptRegExpSupport.Visitor) {
      BreakpointTypeExtension.ScriptRegExpSupport.Visitor<R> regExpVisitor =
          (BreakpointTypeExtension.ScriptRegExpSupport.Visitor<R>) visitor;
      return regExpVisitor.visitRegExp(regExp);
    } else {
      return visitor.visitUnknown(this);
    }
  }
}