// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.json.simple.JSONObject;

/**
 * Holds properties either already parsed or to be parsed on demand.
 */
public abstract class SubpropertiesMirror {
  public abstract List<? extends PropertyReference> getProperties();

  /**
   * Keeps properties in for of JSON and parses JSON on demand.
   */
  public static class JsonBased extends SubpropertiesMirror {
    private final JSONObject jsonWithProperties;

    private List<? extends PropertyReference> properties = null;

    public JsonBased(JSONObject jsonWithProperties) {
      if (JsonUtil.getAsJSONArray(jsonWithProperties, V8Protocol.REF_PROPERTIES) == null) {
        throw new RuntimeException("Value handle without properties");
      }
      this.jsonWithProperties = jsonWithProperties;
    }

    @Override
    public synchronized List<? extends PropertyReference> getProperties() {
      if (properties == null) {
        properties = V8ProtocolUtil.extractObjectProperties(jsonWithProperties);
      }
      return properties;
    }
  }

  static class Simple extends SubpropertiesMirror {
    private final List<PropertyReference> list;

    Simple(PropertyReference ... refs) {
      this.list = Collections.unmodifiableList(Arrays.asList(refs));
    }

    @Override
    public List<? extends PropertyReference> getProperties() {
      return list;
    }
  }

  static final SubpropertiesMirror EMPTY = new SubpropertiesMirror() {
    @Override
    public List<? extends PropertyReference> getProperties() {
      return Collections.emptyList();
    }
  };
}
