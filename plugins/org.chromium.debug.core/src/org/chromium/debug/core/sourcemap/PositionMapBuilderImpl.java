// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.sourcemap;

import static org.chromium.debug.core.util.ChromiumDebugPluginUtil.getSafe;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.chromium.debug.core.model.VmResourceId;
import org.chromium.debug.core.sourcemap.TextSectionMapping.TextPoint;

/**
 * Implementation of {@link SourcePositionMapBuilder} and {@link SourcePositionMap}.
 */
public class PositionMapBuilderImpl implements SourcePositionMapBuilder {

  private final Side originalSide = new Side(TextSectionMapping.Direction.DIRECT);
  private final Side vmSide = new Side(TextSectionMapping.Direction.REVERSE);
  private volatile TokenImpl currentToken = new TokenImpl();

  public SourcePositionMap getSourcePositionMap() {
    return mapImpl;
  }

  private final SourcePositionMap mapImpl = new SourcePositionMap() {
    public SourcePosition calculateVmPosition(VmResourceId id, int line, int column) {
      return originalSide.transformImpl(id, line, column);
    }

    public SourcePosition calculateUserPosition(VmResourceId id, int line, int column) {
      return vmSide.transformImpl(id, line, column);
    }

    public Token getCurrentToken() {
      return currentToken;
    }
  };


  public MappingHandle addMapping(ResourceSection originalSection, ResourceSection vmSection,
      TextSectionMapping fromOriginalToVmSectionMapping) throws CannotAddException {
    RangeAdder originalSideAdder = originalSide.checkCanAddRange(originalSection);
    RangeAdder vmSideAdder = vmSide.checkCanAddRange(vmSection);

    final RangeDeleter originalDeleter = originalSideAdder.commit(fromOriginalToVmSectionMapping,
        vmSection.getResourceId());
    final RangeDeleter vmDeleter = vmSideAdder.commit(fromOriginalToVmSectionMapping,
        originalSection.getResourceId());

    updateToken();
    return new MappingHandle() {
      public void delete() {
        originalDeleter.delete();
        vmDeleter.delete();
        updateToken();
      }
    };
  }

  /**
   * A "side" of transformation -- either "original" or "vm".
   */
  private static class Side {
    private final Map<VmResourceId, ResourceData> resourceMap =
        new HashMap<VmResourceId, PositionMapBuilderImpl.ResourceData>();
    private final TextSectionMapping.Direction direction;

    Side(TextSectionMapping.Direction direction) {
      this.direction = direction;
    }

    SourcePosition transformImpl(VmResourceId id, int line, int column) {
      ResourceData resourceData = resourceMap.get(id);
      if (resourceData != null) {
        TextPoint originalPoint = new TextPoint(line, column);
        SourcePosition resultPosition = resourceData.transform(originalPoint, direction);
        if (resultPosition != null) {
          return resultPosition;
        }
      }
      return new SourcePosition(id, line, column);
    }

    /**
     * Checks whether adding the resource section to map is possible.
     * @return {@link RangeAdder} object that can be used to perform "add" operation; not null
     * @throws CannotAddException if resource section overlaps with already registered section
     */
    private RangeAdder checkCanAddRange(ResourceSection section) throws CannotAddException {
      final VmResourceId resourceId = section.getResourceId();
      final ResourceData data = resourceMap.get(resourceId);
      final Range range = Range.create(section);

      if (data != null) {
        data.checkCanAddRange(range);
      }
      return new RangeAdder() {
        /**
         * Commits 'add' operation. No conflicts are expected at this stage.
         */
        public RangeDeleter commit(TextSectionMapping mapTable, VmResourceId destinationResource) {
          ResourceData commitData = getSafe(resourceMap, resourceId);
          if (commitData == null) {
            commitData = new ResourceData();
            resourceMap.put(resourceId, commitData);
          }

          commitData.addRange(range, mapTable, destinationResource);

          return new RangeDeleter() {
            public void delete() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    }
  }

  private void updateToken() {
    currentToken.updated = true;
    currentToken = new TokenImpl();
  }

  private interface RangeAdder {
    RangeDeleter commit(TextSectionMapping mapTable, VmResourceId destinationResource);
  }

  private interface RangeDeleter {
    void delete();
  }

  /**
   * Defines a mapping for a resource and provides methods for modifying this mapping.
   */
  private static class ResourceData {
    private final SortedMap<TextPoint, RangeGroup> rangeMap =
        new TreeMap<TextPoint, RangeGroup>();

    SourcePosition transform(TextPoint point, TextSectionMapping.Direction direction) {
      RangeGroup structure = findRange(point);
      if (structure == null) {
        return null;
      }
      if (structure.nonEmptyRangeMapping == null) {
        return null;
      }
      if (structure.nonEmptyRangeMapping.sourceRange.end.compareTo(point) <= 0) {
        return null;
      }
      TextPoint resPoint = structure.nonEmptyRangeMapping.mapTable.transform(point, direction);
      return new SourcePosition(structure.nonEmptyRangeMapping.targetResourceId,
          resPoint.getLine(), resPoint.getColumn());
    }

    void checkCanAddRange(Range range) throws CannotAddException {
      RangeGroup otherStructure = getSafe(rangeMap, range.start);
      if (otherStructure != null) {
        if (otherStructure.nonEmptyRangeMapping != null && !range.isEmpty()) {
          throw new CannotAddException("Range overlaps");
        }
      }
      SortedMap<TextPoint, RangeGroup> tailMap = rangeMap.tailMap(range.start);
      if (!tailMap.isEmpty()) {
        Range nextRange = tailMap.values().iterator().next().nonEmptyRangeMapping.sourceRange;
        if (nextRange.start.compareTo(range.end) < 0) {
          throw new CannotAddException("Range overlaps");
        }
      }
    }

    void addRange(Range range, TextSectionMapping mapTable, VmResourceId destinationResource) {
      RangeGroup structure = getSafe(rangeMap, range.start);
      if (structure == null) {
        structure = new RangeGroup();
        this.rangeMap.put(range.start, structure);
      }
      if (range.isEmpty()) {
        structure.emptyRangesAtStart++;
      } else {
        if (structure.nonEmptyRangeMapping != null) {
          throw new RuntimeException();
        }
        structure.nonEmptyRangeMapping = new RangeMapping(range, destinationResource, mapTable);
      }
    }

    private RangeGroup findRange(TextPoint point) {
      // Try if we hit any range's start point.
      RangeGroup structure = rangeMap.get(point);
      if (structure == null) {
        // Check the nearest range that starts before the point.
        SortedMap<TextPoint, RangeGroup> headMap = rangeMap.headMap(point);
        if (headMap.isEmpty()) {
          // No range starts before the point.
          return null;
        } else {
          structure = getSafe(headMap, headMap.lastKey());
          if (structure.nonEmptyRangeMapping != null &&
              structure.nonEmptyRangeMapping.sourceRange.end.compareTo(point) > 0) {
            return structure;
          } else {
            return null;
          }
        }
      } else {
        return structure;
      }
    }
  }

  /**
   * A structure that groups all ranges with the same start point. Those are any number of
   * empty (zero-length) ranges and one optional non-empty range. It is important to keep
   * track of empty ranges, because we shouldn't overlap them with other other ranges.
   */
  private static class RangeGroup {
    int emptyRangesAtStart = 0;
    RangeMapping nonEmptyRangeMapping = null;
  }

  /**
   * A mapping details for a particular range in resource.
   */
  private static class RangeMapping {
    final Range sourceRange;
    final VmResourceId targetResourceId;
    final TextSectionMapping mapTable;

    RangeMapping(Range sourceRange, VmResourceId targetResourceId, TextSectionMapping mapTable) {
      this.sourceRange = sourceRange;
      this.targetResourceId = targetResourceId;
      this.mapTable = mapTable;
    }
  }

  private static class Range {
    static Range create(ResourceSection resourceSection) {
      return new Range(resourceSection.getStart(), resourceSection.getEnd());
    }

    final TextPoint start;
    final TextPoint end;

    Range(TextPoint start, TextPoint end) {
      this.start = start;
      this.end = end;
    }
    boolean isEmpty() {
      return start.equals(end);
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Range == false) {
        return false;
      }
      Range other = (Range) obj;
      return this.start.equals(other.start);
    }
    @Override
    public int hashCode() {
      return start.hashCode();
    }
  }

  private static class TokenImpl implements SourcePositionMap.Token {
    private volatile boolean updated = false;

    public boolean isUpdated() {
      return updated;
    }
  }
}
