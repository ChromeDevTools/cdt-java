// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

/**
 * Handles memento string format. The format is just a sequence of strings that are preceded
 * with their lengths and decorated with parentheses to make it more human-readable.
 */
public class MementoFormat {

  public static void encodeComponent(String component, StringBuilder output) {
    output.append(component.length());
    output.append('(').append(component).append(')');
  }

  /**
   * A simple parser that reads char sequence as a sequence of strings.
   */
  public static class Parser {
    private final CharSequence charSequence;
    private int pos = 0;

    public Parser(CharSequence charSequence) {
      this.charSequence = charSequence;
    }

    public String nextComponent() throws ParserException {
      if (pos >= charSequence.length()) {
        throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
      }
      char ch = charSequence.charAt(pos);
      pos++;
      int num = Character.digit(ch, 10);
      if (num == -1) {
        throw new ParserException("Digit expected"); //$NON-NLS-1$
      }
      int len = num;
      while (true) {
        if (pos >= charSequence.length()) {
          throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
        }
        ch = charSequence.charAt(pos);
        if (!Character.isDigit(ch)) {
          break;
        }
        pos++;
        num = Character.digit(ch, 10);
        if (num == -1) {
          throw new ParserException("Digit expected"); //$NON-NLS-1$
        }
        len = len * 10 + num;
      }
      pos++;
      if (pos + len + 1 > charSequence.length()) {
        throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
      }
      String result = charSequence.subSequence(pos, pos + len).toString();
      pos += len + 1;
      return result;
    }

    public boolean hasMore() {
      return pos < charSequence.length();
    }

    public boolean consumeIfFound(String string) {
      if (string.length() > charSequence.length() - pos) {
        return false;
      }
      for (int i = 0; i < string.length(); i++) {
        if (charSequence.charAt(pos + i) == string.charAt(i)) {
          return false;
        }
      }
      pos += string.length();
      return true;
    }

    public String getDebugSnippet() {
      int end = Math.min(pos + 100, charSequence.length());
      return charSequence.subSequence(pos, end).toString();
    }
  }

  public static class ParserException extends Exception {
    ParserException() {
    }
    ParserException(String message, Throwable cause) {
      super(message, cause);
    }
    ParserException(String message) {
      super(message);
    }
    ParserException(Throwable cause) {
      super(cause);
    }
  }
}