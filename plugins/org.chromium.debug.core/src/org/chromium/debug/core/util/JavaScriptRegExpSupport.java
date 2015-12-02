// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.chromium.debug.core.ScriptNameManipulator;

/**
 * Helper class that manipulates with JavaScript RegExp sources.
 */
public class JavaScriptRegExpSupport {
  /**
   * Encodes all special characters to make a RegExp fragment for this string.
   */
  public static String encodeLiteral(String literal) {
    StringBuilder builder = new StringBuilder(literal.length() * 2);
    for (int i = 0; i < literal.length(); i++) {
      char ch = literal.charAt(i);
      if (BAD_CHARS.contains(ch)) {
        builder.append('\\');
      }
      builder.append(ch);
    }
    return builder.toString();
  }

  /**
   * From JavaScript RegExp source creates a Java Pattern.
   */
  public static Pattern convertToJavaPattern(ScriptNameManipulator.ScriptNamePattern pattern) {
    // We assume that Java has the same RegExp syntax as JavaScript.
    return Pattern.compile(pattern.getJavaScriptRegExp());
  }

  // TODO: complete the list.
  private static final Set<Character> BAD_CHARS = new HashSet<Character>(Arrays.asList(
      '/', '[', ']', '(', ')', '?', '.'
      ));
}