// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chromium.sdk.JsValue.Type;
import org.junit.Test;

/**
 * Tests ValueLoader-related algorithms.
 */
public class ValueLoaderTest {
  @Test
  public void dropCaches() {
    // Check that after caches dropped new value is available.
    String sizeWithDroppedCaches = rereadSizeProperty(true);
    assertEquals("40", sizeWithDroppedCaches);

    // Check that if you don't drop caches, new value is not available.
    String sizeWithoutDroppedCaches = rereadSizeProperty(false);
    assertEquals("20", sizeWithoutDroppedCaches);
  }

  private String rereadSizeProperty(boolean dropCaches) {
    FakeValueLoader valueLoader = new FakeValueLoader();

    StructObjectData addressData = new StructObjectData(1L, Type.TYPE_OBJECT, "Address", null);
    addressData.properties.put("name", new StructValueData(2L, Type.TYPE_STRING, null,
        new LoadableString.Immutable("Leningrad")));
    addressData.properties.put("size", new StructValueData(3L, Type.TYPE_NUMBER, null,
        new LoadableString.Immutable("20")));
    valueLoader.addValueDataRecursive(addressData);

    JsObjectBase.Impl object = new JsObjectBase.Impl(valueLoader,
        FakeValueLoader.createMirrorFromData(addressData, false));

    String sizeBefore = object.getProperty("size").getValue().getValueString();
    assertEquals("20", sizeBefore);

    if (dropCaches) {
      object.getRemoteValueMapping().clearCaches();
    }

    StructValueData newSizeValue = new StructValueData(4L, Type.TYPE_NUMBER, null,
        new LoadableString.Immutable("40"));
    addressData.properties.put("size", newSizeValue);
    valueLoader.addValueDataRecursive(newSizeValue);

    String sizeAfter = object.getProperty("size").getValue().getValueString();
    return sizeAfter;
  }

  private static class StructValueData implements FakeValueLoader.ValueData {
    Long ref;
    Type type;
    String className;
    LoadableString string;


    StructValueData() {
    }

    StructValueData(Long ref, Type type, String className, LoadableString string) {
      this.ref = ref;
      this.type = type;
      this.className = className;
      this.string = string;
    }

    @Override public Long getRef() {
      return ref;
    }
    @Override public Type getType() {
      return type;
    }
    @Override public String getClassName() {
      return className;
    }
    @Override public LoadableString getString() {
      return string;
    }
    @Override public FakeValueLoader.ObjectData asObjectData() {
      return null;
    }
  }

  private static class StructObjectData extends StructValueData
      implements FakeValueLoader.ObjectData {
    final LinkedHashMap<String, StructValueData> properties =
        new LinkedHashMap<String, ValueLoaderTest.StructValueData>();

    StructObjectData() {
    }

    StructObjectData(Long ref, Type type, String className,
        LoadableString string) {
      super(ref, type, className, string);
    }

    @Override
    public Map<String, StructValueData> getProperties() {
      return properties;
    }

    @Override
    public StructObjectData asObjectData() {
      return this;
    }
  }
}
