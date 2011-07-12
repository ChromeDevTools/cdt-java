// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.chromium.debug.core.model.StringMappingData;
import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.sourcemap.SourcePositionMap.TranslateDirection;
import org.chromium.debug.core.sourcemap.SourcePositionMapBuilder.CannotAddException;
import org.chromium.debug.core.sourcemap.SourcePositionMapBuilder.MappingHandle;
import org.chromium.debug.core.sourcemap.SourcePositionMapBuilder.ResourceSection;
import org.junit.Test;

public class PositionMapBuilderImplTest {
  /**
   * Creates a sample position map and checks several points.
   */
  @Test
  public void basicTest() throws CannotAddException {
    SourcePositionMapBuilder builder = new PositionMapBuilderImpl();
    builder.addMapping(new ResourceSection(new VmResourceId("source1.js", null), 0, 0, 5, 0),
        new ResourceSection(new VmResourceId("compiled.js", null), 0, 0, 1, 0),
        new TextSectionMappingImpl(
            new StringMappingData(new int [] { 0,0, 1,0,  2,0,  3,0,  4,0 } , 5, 0),
            new StringMappingData(new int [] { 0,0, 0,10, 0,20, 0,30, 0,40 }, 0, 50)));

    builder.addMapping(new ResourceSection(new VmResourceId("source2.js", null), 0, 0, 5, 0),
        new ResourceSection(new VmResourceId("compiled.js", null), 1, 0, 2, 0),
        new TextSectionMappingImpl(
            new StringMappingData(new int [] { 0,0, 1,0,  2,0,  3,0,  4,0}, 5, 0),
            new StringMappingData(new int [] { 1,0, 1,10, 1,20, 1,30, 1,40}, 1, 50)));

    builder.addMapping(new ResourceSection(new VmResourceId("source3.js", null), 0, 0, 5, 0),
        new ResourceSection(new VmResourceId("compiled.js", null), 2, 0, 3, 0),
        new TextSectionMappingImpl(
            new StringMappingData(new int [] {0,0, 1,0,  2,0,  3,0,  4,0}, 5, 0),
            new StringMappingData(new int [] {2,0, 2,10, 2,20, 2,30, 2,40}, 2, 50)));

    SourcePositionMap map = builder.getSourcePositionMap();

    checkTwoWay(map, "other.js", 17, 4, "other.js", 17, 4, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 2, 11, "source3.js", 1, 1, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 0, 0, "source1.js", 0, 0, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 0, 1, "source1.js", 0, 1, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 1, 0, "source2.js", 0, 0, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 1, 3, "source2.js", 0, 3, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 2, 0, "source3.js", 0, 0, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 2, 3, "source3.js", 0, 3, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 3, 0, "compiled.js", 3, 0, TranslateDirection.VM_TO_USER);

    checkTwoWay(map, "compiled.js", 3, 3, "compiled.js", 3, 3, TranslateDirection.VM_TO_USER);
  }

  private static SourcePosition checkOneWay(SourcePositionMap map,
      String fromFile, int fromLine, int fromColumn,
      String toFile, int toLine, int toColumn, TranslateDirection direction) {
    SourcePosition result = map.translatePosition(new VmResourceId(fromFile, null),
        fromLine, fromColumn, direction);
    Assert.assertEquals(new SourcePosition(new VmResourceId(toFile, null), toLine, toColumn),
        result);
    return result;
  }

  private static void checkTwoWay(SourcePositionMap map,
      String fromFile, int fromLine, int fromColumn,
      String toFile, int toLine, int toColumn, TranslateDirection direction) {
    checkOneWay(map, fromFile, fromLine, fromColumn, toFile, toLine, toColumn, direction);
    SourcePosition result = map.translatePosition(new VmResourceId(toFile, null), toLine, toColumn,
        direction.opposite());
    Assert.assertEquals(new SourcePosition(new VmResourceId(fromFile, null), fromLine, fromColumn),
        result);
  }

  /**
   * Checks that {@link SourcePositionMapBuilder#addMapping} correctly handles overlapping regions.
   */
  @Test
  public void testAddMappingOverlaps() throws CannotAddException {
    new OverlappingMapTestFramework().run();
  }

  private static abstract class MultiRangeMapTestFrameworkBase {
    static final VmResourceId COMPILED_JS_ID = new VmResourceId("compiled.js", null);
    static ResourceSection[] GOOD_SECTIONS = {
      new ResourceSection(COMPILED_JS_ID, 1, 0, 2, 0),
      new ResourceSection(COMPILED_JS_ID, 3, 0, 3, 0),
      new ResourceSection(COMPILED_JS_ID, 5, 0, 5, 0),
      new ResourceSection(COMPILED_JS_ID, 5, 0, 5, 0),
      new ResourceSection(COMPILED_JS_ID, 5, 0, 8, 0),
      new ResourceSection(COMPILED_JS_ID, 8, 0, 8, 0),
    };
    private static final int SHUFFLE_TRIES = 10;

    void run() throws CannotAddException {
      // We get all good sections and add them in a random order. Then we check
      // that each of conflict section will not add.

      Random random = new Random(0);
      for (int i = 0; i < SHUFFLE_TRIES; i++) {
        runOneShuffle(random);
      }
    }

    protected abstract void runOneShuffle(Random random) throws CannotAddException;

    protected ArrayList<MappingHandle> addGoodRanges(SourcePositionMapBuilder builder,
        Random random) throws CannotAddException {
      List<ResourceSection> vmSections =
          new ArrayList<SourcePositionMapBuilder.ResourceSection>(Arrays.asList(GOOD_SECTIONS));
      Collections.shuffle(vmSections, random);
      ArrayList<MappingHandle> result =
          new ArrayList<SourcePositionMapBuilder.MappingHandle>(vmSections.size());
      // Add all good sections.
      int index = 0;
      for (ResourceSection section : vmSections) {
        addSection(builder, section, index);
        index++;
      }
      return result;
    }

    protected static MappingHandle addSection(SourcePositionMapBuilder builder,
        ResourceSection vmSection, int index) throws CannotAddException {
      ResourceSection originalSection =
          new ResourceSection(new VmResourceId(("source" + index + ".js"), null), 0, 0, 5, 0);

      TextSectionMappingImpl textMapping = new TextSectionMappingImpl(
          new StringMappingData(
              new int [] { vmSection.getStart().getLine(), vmSection.getStart().getColumn() },
              vmSection.getEnd().getLine(), vmSection.getEnd().getColumn()),
          new StringMappingData((new int [] { 0, 0 }), 5, 0));

      return builder.addMapping(originalSection, vmSection, textMapping);
    }
  }

  private static class OverlappingMapTestFramework extends MultiRangeMapTestFrameworkBase {
    private static ResourceSection[] CONFLICT_SECTIONS = {
      new ResourceSection(COMPILED_JS_ID, 1, 0, 2, 0),
      new ResourceSection(COMPILED_JS_ID, 0, 1, 1, 1),
      new ResourceSection(COMPILED_JS_ID, 1, 1, 2, 1),
      new ResourceSection(COMPILED_JS_ID, 1, 1, 1, 2),
      new ResourceSection(COMPILED_JS_ID, 2, 0, 4, 0),
      new ResourceSection(COMPILED_JS_ID, 4, 0, 6, 0),
      new ResourceSection(COMPILED_JS_ID, 6, 0, 7, 0),
      new ResourceSection(COMPILED_JS_ID, 7, 0, 9, 0),
    };

    protected void runOneShuffle(Random random) throws CannotAddException {

      final SourcePositionMapBuilder builder = new PositionMapBuilderImpl();

      List<MappingHandle> goodRangeHandles = addGoodRanges(builder, random);

      final int conflict_section_index = goodRangeHandles.size();
      // Now try to add conflict sections.
      for (final ResourceSection section : CONFLICT_SECTIONS) {
        assertThrowsAddException(new RunnableWithCannotAddException() {
          public void run() throws CannotAddException {
            addSection(builder, section, conflict_section_index);
          }
        });
      }
    }
  }

  /**
   * Checks that {@link SourcePositionMapBuilder} can add mappings and delete them
   * in different order.
   */
  @Test
  public void testAddAndDeleteMapping() throws CannotAddException {
    new AddAndDeleteMapTestFramework().run();
  }

  private static class AddAndDeleteMapTestFramework extends MultiRangeMapTestFrameworkBase {
    protected void runOneShuffle(Random random) throws CannotAddException {
      final SourcePositionMapBuilder builder = new PositionMapBuilderImpl();

      ArrayList<MappingHandle> goodRangeHandles = addGoodRanges(builder, random);

      // Delete ranges in other order.
      Collections.shuffle(goodRangeHandles, random);

      for (MappingHandle handle : goodRangeHandles) {
        handle.delete();
      }
    }
  }

  private interface RunnableWithCannotAddException {
    void run() throws CannotAddException;
  }

  private static void assertThrowsAddException(RunnableWithCannotAddException runnable) {
    try {
      runnable.run();
      Assert.fail("Exception expected");
    } catch (CannotAddException e) {
      // Expected exception.
    }
  }
}
