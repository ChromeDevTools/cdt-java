// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.util;

import java.util.Collection;

/**
 * Binary search algorithm implementation. Unlike {@link Collection#binarySearch}, its input
 * model ({@link Input}) never requires that list elements are presented as Java objects.
 */
public class RangeBinarySearch {
  /**
   * An algorithm input model. There are several (possibly zero) pin-points on an abstract axis.
   * The pin-points are ordered and they divide the axis into several ranges. First and last
   * range are open-ended. An additional point X is strictly less or more than any pin-point.
   */
  public interface Input {
    /**
     * @return number of pin-points
     */
    int pinPointsNumber();

    /**
     * Compares point X with a pin-point. There should exist such number N that this method
     * returns false when index < N and true otherwise.
     * @param index 0-based index of a pin-point
     * @return whether X is less than pin-point number index or more that it
     */
    boolean isPointXLessThanPinPoint(int pinPointIndex);
  }

  /**
   * For the input finds a range that contains the point.
   * @return index 0-base range number.
   */
  public static int find(Input input) {
    // Index of first range in question.
    int begin = 0;
    // Index of last range in question.
    int end = input.pinPointsNumber();

    while (begin != end) {
      // Index of next pin-point.
      int middle = (begin + end) / 2;
      if (input.isPointXLessThanPinPoint(middle)) {
        end = middle;
      } else {
        begin = middle + 1;
      }
    }
    return begin;
  }
}
