// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.sourcemap;

/**
 * Defines a mapping from one text file into another one. Mapping works in both
 * directions: one is called "direct" and another is called "reverse".
 */
public interface TextSectionMapping {
  enum Direction {
    DIRECT, REVERSE
  }

  /**
   * Transforms a point into another {@link TextPoint} according to the map in direct
   * or reverse direction.
   */
  TextPoint transform(TextPoint point, Direction direction);

  /**
   * A structure of line/column. It implements hashCode/equals methods and {@link Comparable}
   * interface (text point is less that other if it normally gets read earlier).
   * Line/column numbers are always 0-based.
   */
  final class TextPoint implements Comparable<TextPoint> {
    private final int line;
    private final int column;

    public TextPoint(int line, int column) {
      this.line = line;
      this.column = column;
    }

    public int getLine() {
      return line;
    }

    public int getColumn() {
      return column;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof TextPoint == false) {
        return false;
      }
      TextPoint other = (TextPoint) obj;
      return this.line == other.line && this.getColumn() == other.getColumn();
    }

    @Override
    public int hashCode() {
      return line + getColumn() * 31;
    }

    public int compareTo(TextPoint o) {
      if (this.line < o.line) {
        return -1;
      } else if (this.line > o.line) {
        return +1;
      } else {
        if (this.getColumn() < o.getColumn()) {
          return -1;
        } else if (this.getColumn() > o.getColumn()) {
          return +1;
        } else {
          return 0;
        }
      }
    }

    @Override
    public String toString() {
      return line + ":" + column;
    }
  }
}