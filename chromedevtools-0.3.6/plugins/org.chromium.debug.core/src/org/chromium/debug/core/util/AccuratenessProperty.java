// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

/**
 * A persistent property of IResource that holds 'file match accurateness value' --
 * a number of file path components that is used in breakpoint target RegExp.
 */
public class AccuratenessProperty {
  public static final QualifiedName KEY =
      new QualifiedName("http://core.debug.chromium.org", "file_match_accurateness");

  public static int read(IFile file) throws CoreException {
    String value = file.getPersistentProperty(KEY);
    return Parser.parse(value);
  }

  public static class Parser {
    public static int parse(String stringValue) {
      if (stringValue == null) {
        return 1;
      } else {
        return Integer.parseInt(stringValue);
      }
    }

    public static String write(int value) {
      if (value == DEFAULT_VALUE) {
        return null;
      } else {
        return Integer.toString(value);
      }
    }

    public static final int DEFAULT_VALUE = BASE_VALUE;
  }

  /**
   * Corresponds to short name only.
   */
  public static final int BASE_VALUE = 1;
}