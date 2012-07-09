// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.jsdtbridge;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

/**
 * A relatively simple {@link String#indexOf}-based formatter that inserts newlines
 * after semicolons and maintains indentations based on curly brace symbols it meets. The quality
 * of work is quite poor: it doesn't recognize string literals or comments and will format their
 * internals as well.
 * <p>
 * (Technical details: in JavaScript one cannot detect RegExp syntax without doing a full parsing;
 * since RegExp syntax allows quotes among other symbols, one cannot also detect string literals;
 * consequently there's no way one can detect comments. That's why all this syntax elements remain
 * unrecognized.)
 */
public class AdHocFormatter {
  public static TextEdit format(String source, String header) {
    FormatSession session = new FormatSession(source, header);
    session.run();
    return session.getResult();
  }

  private static class FormatSession {
    private int position = 0;
    private final String header;
    private final String source;
    private final SpaceCache spaceCache = new SpaceCache();

    private final MultiTextEdit result = new MultiTextEdit();

    private LastSeenState currentState = LastSeenState.NEW_LINE;

    private enum LastSeenState {
      OPEN_BRACE, CLOSE_BRACE, SEMICOLON, NEW_LINE, NON_SPACE
    }

    private int currentNesting = 0;

    FormatSession(String source, String header) {
      this.source = source;
      this.header = header;
    }

    void run() {
      result.addChild(new ReplaceEdit(0, 0, header));

      while (position < source.length()) {
        {
          char ch = source.charAt(position);
          switch (ch) {
            case ';':
              handleSemicolon();
              break;
            case '{':
              handleOpenBrace();
              break;
            case '}':
              handleCloseBrace();
              break;
            case '\r':
            case '\n':
              handleLineEnd();
              break;
            case ' ':
            case '\t':
              // Ignore.
              break;
            default:
              handleNonSpace();
          }
          position++;
        }
      }
    }

    TextEdit getResult() {
      return result;
    }

    private void handleLineEnd() {
      currentState = LastSeenState.NEW_LINE;
    }

    private void handleSemicolon() {
      currentState = LastSeenState.SEMICOLON;
    }

    private void handleOpenBrace() {
      if (currentState == LastSeenState.SEMICOLON || currentState == LastSeenState.CLOSE_BRACE
          || currentState == LastSeenState.OPEN_BRACE) {
        insertNewLine();
      }
      currentNesting++;
      currentState = LastSeenState.OPEN_BRACE;
    }

    private void handleCloseBrace() {
      if (currentNesting > 0) {
        currentNesting--;
      }
      if (currentState != LastSeenState.NEW_LINE) {
        insertNewLine();
      }
      currentState = LastSeenState.CLOSE_BRACE;
    }

    private void handleNonSpace() {
      if (currentState == LastSeenState.SEMICOLON || currentState == LastSeenState.CLOSE_BRACE
          || currentState == LastSeenState.OPEN_BRACE) {
        insertNewLine();
      }
      currentState = LastSeenState.NON_SPACE;
    }

    private void insertNewLine() {
      result.addChild(new ReplaceEdit(position, 0, spaceCache.getSpace(currentNesting * 2)));
    }

    // Caches instances of strings that starts with a new-line and contains of n spaces.
    private static class SpaceCache {
      private final Map<Integer, String> map = new HashMap<Integer, String>();
      public String getSpace(int len) {
        String result = map.get(len);
        if (result == null) {
          StringBuilder builder = new StringBuilder(len + 1);
          builder.append('\n');
          for (int i = 0; i < len; i++) {
            builder.append(' ');
          }
          result = builder.toString();
          map.put(len, result);
        }
        return result;
      }
    }
  }
}
