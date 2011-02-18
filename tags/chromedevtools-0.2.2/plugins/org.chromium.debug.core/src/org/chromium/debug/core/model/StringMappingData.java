// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

/**
 * A raw-level data that defines segments inside one multiline substring in terms of lines/columns.
 * A segment is a range within string from (line1, col1) inclusive to (line2, col2) exclusive.
 * These segments could be used for defining a map between to strings.
 */
public class StringMappingData {
  // Contains pairs line/column (0-based).
  private final int[] dataArray;

  private final int endLine;
  private final int endColumn;

  /**
   * @param lineColumnDataArray non-empty array of pairs (line, column); first pair must correspond
   *     to the beginning of the whole substring
   * @param endLine end line of the whole substring
   * @param endColumn end column of the whole substring
   */
  public StringMappingData(int[] lineColumnDataArray, int endLine, int endColumn) {
    this.dataArray = lineColumnDataArray;
    this.endLine = endLine;
    this.endColumn = endColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getEndColumn() {
    return endColumn;
  }

  /**
   * Finds a segment, that includes position (line, col). Each segment has its begin inclusive and
   * its end exclusive.
   * @return segment identifier
   */
  public int findContainingSegment(int line, int col) {
    if (dataArray.length == 0) {
      throw new RuntimeException();
    }
    // A binary search.
    int p1 = 0;
    int p2 = dataArray.length - 2;
    while (p1 < p2) {
      int pMiddle = ((p1/2 + p2/2) / 2 + 1)*2;
      int lineMiddle = dataArray[pMiddle];
      boolean moveP1;
      if (lineMiddle < line) {
        moveP1 = true;
      } else if (lineMiddle > line) {
        moveP1 = false;
      } else {
        int colMiddle = dataArray[pMiddle + 1];
        if (colMiddle <= col) {
          moveP1 = true;
        } else {
          moveP1 = false;
        }
      }
      if (moveP1) {
        p1 = pMiddle;
      } else {
        p2 = pMiddle - 2;
      }
    }
    return p1;
  }

  /**
   * @param segementId internal id of the segment that was returned by other
   *     methods of the class
   */
  public int getSegmentBeginLine(int segementId) {
    return dataArray[segementId];
  }

  /**
   * @param segementId internal id of the segment that was returned by other
   *     methods of the class
   */
  public int getSegmentBeginColumn(int segmentId) {
    return dataArray[segmentId + 1];
  }

  public int getLastSegmentId() {
    return dataArray.length - 2;
  }

  public static int getNextSegmentId(int segmentId) {
    return segmentId + 2;
  }
}
