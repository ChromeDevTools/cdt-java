// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import static junit.framework.Assert.*;

import org.chromium.debug.core.model.StringMappingData;
import org.chromium.debug.core.sourcemap.TextSectionMapping.Direction;
import org.chromium.debug.core.sourcemap.TextSectionMapping.TextPoint;
import org.junit.Test;

public class TextSectionMappingImplTest {

  @Test
  public void testBasic() {
    TextSectionMapping mapping = new TextSectionMappingImpl(
        new StringMappingData(
            new int[] { 0,0, 0,2, 0,12, 2,5, 2,10, 2,10, 2,10,  3,0,   4,0  },  5, 0),
        new StringMappingData(
            new int[] { 5,3, 5,5, 7,3,  9,5, 9,10, 11,3, 11,17, 11,17, 12,0 }, 13, 0)
        );

    checkTwoWays(mapping, new TextPoint(0, 0), new TextPoint(5, 3));
    checkTwoWays(mapping, new TextPoint(0, 1), new TextPoint(5, 4));
    checkTwoWays(mapping, new TextPoint(0, 2), new TextPoint(5, 5));
    checkTwoWays(mapping, new TextPoint(0, 3), new TextPoint(5, 6));
    checkTwoWays(mapping, new TextPoint(0, 10), new TextPoint(5, 13));
    checkTwoWays(mapping, new TextPoint(0, 11), new TextPoint(5, 14));

    assertEquals(new TextPoint(0, 11),
        mapping.transform(new TextPoint(5, 15), Direction.REVERSE));
    assertEquals(new TextPoint(0, 11),
        mapping.transform(new TextPoint(5, 16), Direction.REVERSE));
    assertEquals(new TextPoint(0, 11),
        mapping.transform(new TextPoint(6, 3), Direction.REVERSE));
    assertEquals(new TextPoint(0, 11),
        mapping.transform(new TextPoint(7, 2), Direction.REVERSE));

    checkTwoWays(mapping, new TextPoint(0, 12), new TextPoint(7, 3));
    checkTwoWays(mapping, new TextPoint(0, 13), new TextPoint(7, 4));
    checkTwoWays(mapping, new TextPoint(1, 13), new TextPoint(8, 13));
    checkTwoWays(mapping, new TextPoint(2, 0), new TextPoint(9, 0));
    checkTwoWays(mapping, new TextPoint(2, 4), new TextPoint(9, 4));
    checkTwoWays(mapping, new TextPoint(2, 5), new TextPoint(9, 5));
    checkTwoWays(mapping, new TextPoint(2, 9), new TextPoint(9, 9));

    assertEquals(new TextPoint(2, 10),
        mapping.transform(new TextPoint(11, 3), Direction.REVERSE));
    assertEquals(new TextPoint(2, 10),
        mapping.transform(new TextPoint(11, 4), Direction.REVERSE));
    assertEquals(new TextPoint(2, 10),
        mapping.transform(new TextPoint(11, 16), Direction.REVERSE));
    assertEquals(new TextPoint(3, 0),
        mapping.transform(new TextPoint(11, 17), Direction.REVERSE));
    assertEquals(new TextPoint(11, 18),
        mapping.transform(new TextPoint(3, 1), Direction.DIRECT));
  }

  private static void checkTwoWays(TextSectionMapping mapping,
      TextPoint source, TextPoint destination) {
    TextPoint resultOne = mapping.transform(source, Direction.DIRECT);
    assertEquals(destination, resultOne);
    TextPoint resultTwo = mapping.transform(resultOne, Direction.REVERSE);
    assertEquals(source, resultTwo);
  }
}
