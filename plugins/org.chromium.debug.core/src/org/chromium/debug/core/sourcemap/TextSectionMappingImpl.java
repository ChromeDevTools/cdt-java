// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.sourcemap;

import org.chromium.debug.core.model.StringMappingData;

/**
 * A default implementation of {@link TextSectionMapping}. It is backed by data from
 * 2 {@link StringMappingData} objects.
 */
public class TextSectionMappingImpl implements TextSectionMapping {
  private final StringMappingData directMappingData;
  private final StringMappingData backwardMappingData;

  public TextSectionMappingImpl(StringMappingData directMappingData,
      StringMappingData backwardMappingData) {
    this.directMappingData = directMappingData;
    this.backwardMappingData = backwardMappingData;
  }

  public TextPoint transform(TextPoint original, Direction direction) {
    StringMappingData sourceData =
        direction == Direction.REVERSE ? this.backwardMappingData : this.directMappingData;
    int pos = sourceData.findContainingSegment(original.getLine(), original.getColumn());

    StringMappingData targetData =
        direction == Direction.REVERSE ? this.directMappingData : this.backwardMappingData;

    int resLine;
    int resCol;
    if (sourceData.getSegmentBeginLine(pos) == original.getLine()) {
      resLine = targetData.getSegmentBeginLine(pos);
      resCol = original.getColumn() - sourceData.getSegmentBeginColumn(pos) +
          targetData.getSegmentBeginColumn(pos);
    } else {
      resLine = original.getLine() - sourceData.getSegmentBeginLine(pos) +
          targetData.getSegmentBeginLine(pos);
      resCol = original.getColumn();
    }
    if (pos < targetData.getLastSegmentId()) {
      int nextPos = StringMappingData.getNextSegmentId(pos);
      boolean exceed = resLine > targetData.getSegmentBeginLine(nextPos) ||
          (resLine == targetData.getSegmentBeginLine(nextPos) &&
              resCol >= targetData.getSegmentBeginColumn(nextPos));
      if (exceed) {
        if (targetData.getSegmentBeginLine(pos) == targetData.getSegmentBeginLine(nextPos) &&
            targetData.getSegmentBeginColumn(pos) == targetData.getSegmentBeginColumn(nextPos)) {
          resLine = targetData.getSegmentBeginLine(pos);
          resCol = targetData.getSegmentBeginColumn(pos);
        } else if (targetData.getSegmentBeginColumn(nextPos) > 0) {
          resLine = targetData.getSegmentBeginLine(nextPos);
          resCol = targetData.getSegmentBeginColumn(nextPos) - 1;
        } else {
          resLine = targetData.getSegmentBeginLine(nextPos) - 1;
          resCol = 0;
        }
      }
    }

    return new TextPoint(resLine, resCol);
  }
}