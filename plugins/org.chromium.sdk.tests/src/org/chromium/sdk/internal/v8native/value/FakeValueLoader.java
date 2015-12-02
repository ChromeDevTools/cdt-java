// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.internal.v8native.InternalContext;

/**
 * Test {@link ValueLoader} implementation that works over local data model.
 */
class FakeValueLoader extends ValueLoader {
  private int cacheState = 1;
  private final Map<Long, ValueData> valueDataMap = new HashMap<Long, ValueData>();

  void addValueDataRecursive(ValueData data) {
    Long ref = data.getRef();
    if (ref < 0) {
      return;
    }
    if (!valueDataMap.containsKey(ref)) {
      valueDataMap.put(ref, data);
      ObjectData asObject = data.asObjectData();
      if (asObject != null) {
        for (ValueData inner : asObject.getProperties().values()) {
          addValueDataRecursive(inner);
        }
      }
    }
  }

  @Override
  public void clearCaches() {
    cacheState++;
  }

  @Override
  int getCurrentCacheState() {
    return cacheState;
  }

  @Override
  public SubpropertiesMirror getOrLoadSubproperties(Long ref) {
    ValueData data = getSafe(valueDataMap, ref);
    ObjectData asObject = data.asObjectData();
    if (asObject == null) {
      return SubpropertiesMirror.EMPTY;
    } else {
      return createSubpropertiesMirror(asObject);
    }
  }

  @Override
  public List<ValueMirror> getOrLoadValueFromRefs(List<? extends PropertyReference> propertyRefs) {
    List<ValueMirror> result = new ArrayList<ValueMirror>();
    for (PropertyReference reference : propertyRefs) {
      Long ref = reference.getRef();
      ValueData data = getSafe(valueDataMap, ref);
      ValueMirror mirror = createMirrorFromData(data, false);
      result.add(mirror);
    }
    return result;
  }

  @Override
  public InternalContext getInternalContext() {
    throw new UnsupportedOperationException();
  }

  static SubpropertiesMirror createSubpropertiesMirror(ObjectData objectData) {
    List<PropertyReference> list = new ArrayList<PropertyReference>();
    for (Map.Entry<String, ? extends ValueData> en : objectData.getProperties().entrySet()) {
      list.add(new PropertyReference(en.getKey(), DataWithRef.fromLong(en.getValue().getRef())));
    }
    return new SubpropertiesMirror.ListBased(list);
  }

  static ValueMirror createMirrorFromData(ValueData data, boolean includeProperties) {
    SubpropertiesMirror subpropertiesMirror;
    if (includeProperties) {
      ObjectData asObject = data.asObjectData();
      if (asObject == null) {
        subpropertiesMirror = SubpropertiesMirror.EMPTY;
      } else {
        subpropertiesMirror = FakeValueLoader.createSubpropertiesMirror(asObject);
      }
    } else {
      subpropertiesMirror = null;
    }
    return ValueMirror.create(data.getRef(), data.getType(), data.getClassName(),
        data.getString(), subpropertiesMirror);
  }

  interface ValueData {
    Long getRef();
    Type getType();
    String getClassName();
    LoadableString getString();

    ObjectData asObjectData();
  }

  interface ObjectData {
    Map<String, ? extends ValueData> getProperties();
  }
}