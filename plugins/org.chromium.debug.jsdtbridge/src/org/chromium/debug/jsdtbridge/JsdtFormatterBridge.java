// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.jsdtbridge;

import org.chromium.debug.core.model.JavaScriptFormatter;
import org.chromium.debug.core.model.StringMappingData;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.jsdt.core.JavaScriptCore;
import org.eclipse.wst.jsdt.core.ToolFactory;
import org.eclipse.wst.jsdt.core.formatter.CodeFormatter;

/**
 * JSDT-based implementation of {@link JavaScriptFormatter}.
 */
public class JsdtFormatterBridge implements JavaScriptFormatter {
  public Result format(String sourceString) {
    TextEdit textEdit = jsdtFormat(sourceString);

    if (textEdit == null) {
      final boolean useFallbackFormatter = true;
      if (useFallbackFormatter) {
        // While JSDT formatter has chances to fail
        // (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=329716),
        // there is a fall-back implementation, that only insert new-lines in some places
        // thus making a source a bit more readable.
        String header = Messages.JsdtFormatterBridge_FALLBACK_COMMENT;
        textEdit = AdHocFormatter.format(sourceString, header);
      } else {
        throw new RuntimeException("Formatter failed"); //$NON-NLS-1$
      }
    }
    return convertResult(sourceString, textEdit);
  }

  private TextEdit jsdtFormat(String sourceString) {
    CodeFormatter jsdtFormatter = ToolFactory.createCodeFormatter(
        JavaScriptCore.getDefaultOptions());

    TextEdit textEdit = jsdtFormatter.format(CodeFormatter.K_JAVASCRIPT_UNIT,
        sourceString, 0, sourceString.length(), 0, LINE_END_STRING);
    return textEdit;
  }

  private Result convertResult(String sourceString, TextEdit textEdit) {
    IntBuffer intBuffer = new IntBuffer();

    final Position origPos = new Position(0, 0);
    final Position dstPos = new Position(0, 0);

    TextEdit[] editList = textEdit.getChildren();

    int sourceStringPos = 0;
    int editListPos = 0;

    int nextLineEndPos = sourceString.indexOf(LINE_END_CHAR);

    final StringBuilder builder = new StringBuilder();

    // Iterate over all edits and all untouched line ends.
    while (true) {
      ReplaceEdit replaceEdit;
      int nextEditPos = -1;
      { // Find next applicable edit. This is a potential cycle if we skip some changes.
        if (editListPos < editList.length) {
          if (editList[editListPos] instanceof ReplaceEdit == false) {
            throw new RuntimeException();
          }
          replaceEdit = (ReplaceEdit) editList[editListPos];
          nextEditPos = replaceEdit.getOffset();
        } else {
          replaceEdit = null;
        }
      }

      // Choose what comes first: line end or edit.
      boolean processLineEndNotEdit;
      if (nextEditPos == -1) {
        if (nextLineEndPos == -1) {
          break;
        } else {
          processLineEndNotEdit = true;
        }
      } else {
        if (nextLineEndPos == -1) {
          processLineEndNotEdit = false;
        } else {
          processLineEndNotEdit = nextLineEndPos < nextEditPos;
        }
      }
      if (processLineEndNotEdit) {
        // Process next line end.
        builder.append(sourceString.substring(sourceStringPos, nextLineEndPos + 1));
        origPos.line++;
        origPos.col = 0;
        dstPos.line++;
        dstPos.col = 0;
        sourceStringPos = nextLineEndPos + 1;
        nextLineEndPos = sourceString.indexOf(LINE_END_CHAR, sourceStringPos);
      } else {
        // Process next edit.
        builder.append(sourceString.substring(sourceStringPos, nextEditPos));
        origPos.col += nextEditPos - sourceStringPos;
        dstPos.col += nextEditPos - sourceStringPos;

        origPos.writeToArray(intBuffer);
        dstPos.writeToArray(intBuffer);

        // Count removed line ends.
        if (replaceEdit.getLength() > 0) {
          String removedString = sourceString.substring(replaceEdit.getOffset(),
              replaceEdit.getOffset() + replaceEdit.getLength());
          origPos.advanceToString(removedString);
        }
        // Count added line ends.
        builder.append(replaceEdit.getText());
        dstPos.advanceToString(replaceEdit.getText());

        origPos.writeToArray(intBuffer);
        dstPos.writeToArray(intBuffer);

        sourceStringPos = nextEditPos + replaceEdit.getLength();
        editListPos++;

        if (nextLineEndPos != -1 && nextLineEndPos < sourceStringPos) {
          nextLineEndPos = sourceString.indexOf(LINE_END_CHAR, sourceStringPos);
        }
      }
    }
    builder.append(sourceString.substring(sourceStringPos));
    origPos.col += sourceString.length() - sourceStringPos;
    dstPos.col += sourceString.length() - sourceStringPos;

    final int[] inputArray = new int[intBuffer.size() / 2];
    final int[] formattedArray = new int[intBuffer.size() / 2];

    for (int i = 0; i < inputArray.length; i += 2) {
      inputArray[i] = intBuffer.get(i * 2 + 0);
      inputArray[i + 1] = intBuffer.get(i * 2 + 1);
      formattedArray[i] = intBuffer.get(i * 2 + 2);
      formattedArray[i + 1] = intBuffer.get(i * 2 + 3);
    }

    final StringMappingData inputTextData =
        new StringMappingData(inputArray, origPos.line, origPos.col);

    final StringMappingData formattedTextData =
        new StringMappingData(formattedArray, dstPos.line, dstPos.col);


    return new Result() {
      public String getFormattedText() {
        return builder.toString();
      }

      public StringMappingData getInputTextData() {
        return inputTextData;
      }

      public StringMappingData getFormattedTextData() {
        return formattedTextData;
      }
    };
  }

  private static class Position {
    Position(int line, int col) {
      this.line = line;
      this.col = col;
    }
    int line;
    int col;

    void advanceToString(String str) {
      int innerPos = 0;
      while (true) {
        int innerLineEndPos = str.indexOf('\n', innerPos);
        if (innerLineEndPos == -1) {
          break;
        }
        line++;
        col = 0;
        innerPos = innerLineEndPos + 1;
      }
      col += str.length() - innerPos;
    }

    void writeToArray(IntBuffer buffer) {
      buffer.add2(line, col);
    }
  }

  // Exponentially-growing buffer for ints.
  private static class IntBuffer {
    private int[] array = new int[INITIAL_SIZE];
    private int pos = 0;

    public void add2(int i1, int i2) {
      if (pos + 2 > array.length) {
        int[] newArray = new int[array.length * 2];
        System.arraycopy(array, 0, newArray, 0, pos);
        array = newArray;
      }
      array[pos] = i1;
      array[pos + 1] = i2;
      pos += 2;
    }

    public int get(int pos) {
      return array[pos];
    }

    public int size() {
      return pos;
    }

    private static int INITIAL_SIZE = 10;
  }

  private static final char LINE_END_CHAR = '\n';
  private static final String LINE_END_STRING = LINE_END_CHAR + ""; //$NON-NLS-1$
}
