// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.chromium.sdk.Script;
import org.eclipse.osgi.util.NLS;

/**
 * Creates from a set of scripts a mock-up of full resource (scripts are positioned according
 * to their line numbers and the whitespace is filled with text pattern).
 */
class MockUpResourceWriter {
  static String writeScriptSource(Collection<Script> scripts) {
    ArrayList<Script> sortedScriptsArrayList = new ArrayList<Script>();
    for (Script script : scripts) {
      if (script.isCollected()) {
        continue;
      }
      sortedScriptsArrayList.add(script);
    }
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
      int scriptCol = script.getStartColumn();
      fillColumns(scriptCol);
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
    if (number >= NOT_A_JAVASCRIPT_FILLER.length()) {
      builder.append(NOT_A_JAVASCRIPT_FILLER.substring(0, NOT_A_JAVASCRIPT_FILLER.length() - 1));
      int fill = number - NOT_A_JAVASCRIPT_FILLER.length();
      for (int i = 0; i < fill; i++) {
        builder.append('*');
      }
      builder.append(NOT_A_JAVASCRIPT_FILLER.substring(NOT_A_JAVASCRIPT_FILLER.length() - 1));
      col += number;
    } else if (number < 1) {
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
      } else if (line1 > line2) {
        return 1;
      }
      int col1 = o1.getStartColumn();
      int col2 = o2.getStartColumn();
      if (col1 < col2) {
        return -1;
      } else if (col1 > col2) {
        return 1;
      }
      return 0;
    }
  };
}