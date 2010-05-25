// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.chromium.sdk.Script;
import org.eclipse.osgi.util.NLS;

/**
 * Creates from a set of scripts a mock-up of full resource (scripts are positioned according
 * to their line numbers and the whitespace is filled with text pattern).
 */
class MockUpResourceWriter {
  static String writeScriptSource(List<Script> scripts) {
    ArrayList<Script> sortedScriptsArrayList = new ArrayList<Script>(scripts);
    Collections.sort(sortedScriptsArrayList, scriptPositionComparator);
    MockUpResourceWriter writer = new MockUpResourceWriter();
    for (Script script : sortedScriptsArrayList) {
      writer.writeSript(script);
    }
    return writer.getResult();
  }


  private int line = 0;
  private int col = 0;
  private final StringBuilder builder = new StringBuilder();

  private void writeSript(Script script) {
    int scriptLine = script.getStartLine();
    if (scriptLine > line) {
      fillLines(scriptLine - line);
      line = scriptLine;
    } else if (scriptLine < line) {
      writeLineMissMessage(scriptLine);
    } else {
      int scriptCol = script.getStartColumn();
      if (col < scriptCol) {
        fillColumns(scriptCol - col);
      } else if (col > scriptCol) {
        final boolean expectCorrectStartColumn = false;
        if (expectCorrectStartColumn) {
          writeln(""); //$NON-NLS-1$
          writeLineMissMessage(scriptLine);
        } else {
          // Ignore.
        }
      }
    }

    if (script.hasSource()) {
      writeText(script.getSource());
    } else {
      writeln(Messages.MockUpResourceWriter_SCRIPT_WITHOUT_TEXT);
    }
  }

  private void writeLineMissMessage(int scriptLine) {
    writeln(NLS.bind(Messages.MockUpResourceWriter_SCRIPTS_OVERLAPPED,
        line + 1 - scriptLine, scriptLine + 1));
  }

  private void writeText(String text) {
    int pos = 0;
    while (true) {
      int nlPos = text.indexOf('\n', pos);
      if (nlPos == -1) {
        String rest = text.substring(pos);
        builder.append(rest);
        col += rest.length();
        break;
      }
      writeln(text.substring(pos, nlPos));
      pos = nlPos + 1;
    }
  }

  private void writeln(String str) {
    builder.append(str).append('\n');
    line++;
    col = 0;
  }

  private void fillLines(int lines) {
    if (col != 0) {
      builder.append('\n');
      line++;
    }
    for (int i = 0; i < lines; i++) {
      builder.append(NOT_A_JAVASCRIPT_FILLER).append('\n');
    }
    line += lines;
    col = 0;
  }

  private void fillColumns(int number) {
    if (number < NOT_A_JAVASCRIPT_FILLER.length()) {
      if (number < 1) {
        // Nothing.
      } else if (number == 1) {
        builder.append('*');
        col += 1;
      } else {
        builder.append('{');
        for (int i = 2; i < number; i++) {
          builder.append('*');
        }
        builder.append('}');
        col += number;
      }
    }
  }

  private String getResult() {
    return builder.toString();
  }

  private static final String NOT_A_JAVASCRIPT_FILLER =
      Messages.MockUpResourceWriter_NOT_A_JAVASCRIPT;

  private static final Comparator<Script> scriptPositionComparator = new Comparator<Script>() {
    public int compare(Script o1, Script o2) {
      int line1 = o1.getStartLine();
      int line2 = o2.getStartLine();
      if (line1 < line2) {
        return -1;
      } else if (line1 == line2) {
        return 0;
      } else {
        return 1;
      }
    }
  };
}