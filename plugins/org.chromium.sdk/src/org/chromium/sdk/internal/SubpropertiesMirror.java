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
 * This class is intended to hold properties either already parsed or to be parsed on demand.
 */
public abstract class SubpropertiesMirror {
  public abstract List<? extends PropertyReference> getProperties();

  public abstract List<? extends PropertyReference> getInternalProperties();

  public abstract Object getAdditionalProperties();

  /**
   * Keeps properties in for of JSON and parses JSON on demand.
   */
  public static class JsonBased extends SubpropertiesMirror {
    private final JSONObject jsonWithProperties;
    private final AdditionalPropertyFactory additionalPropertyFactory;

    private List<? extends PropertyReference> properties = null;
    private List<? extends PropertyReference> internalProperties = null;
    private Object additionalProperties = null;

    public JsonBased(JSONObject jsonWithProperties,
        AdditionalPropertyFactory additionalPropertyFactory) {
      if (additionalPropertyFactory == null) {
        additionalPropertyFactory = NO_OP_FACTORY;
      }
      if (JsonUtil.getAsJSONArray(jsonWithProperties, V8Protocol.REF_PROPERTIES) == null) {
        throw new RuntimeException("Value handle without properties");
      }
      this.jsonWithProperties = jsonWithProperties;
      this.additionalPropertyFactory = additionalPropertyFactory;
    }

    @Override
    public synchronized List<? extends PropertyReference> getProperties() {
      if (properties == null) {
        properties = V8ProtocolUtil.extractObjectProperties(jsonWithProperties);
      }
      return properties;
    }

    @Override
    public synchronized List<? extends PropertyReference> getInternalProperties() {
      if (internalProperties == null) {
        internalProperties = V8ProtocolUtil.extractObjectInternalProperties(jsonWithProperties);
      }
      return internalProperties;
    }

    @Override
    public Object getAdditionalProperties() {
      if (additionalProperties == null) {
        additionalProperties =
            additionalPropertyFactory.createAdditionalProperties(jsonWithProperties);
      }
      return additionalProperties;
    }

    public interface AdditionalPropertyFactory {
      Object createAdditionalProperties(JSONObject jsonWithProperties);
    }

    private static AdditionalPropertyFactory NO_OP_FACTORY = new AdditionalPropertyFactory() {
      public Object createAdditionalProperties(JSONObject jsonWithProperties) {
        return EMPTY_OBJECT;
      }
    };
  }

  static class ListBased extends SubpropertiesMirror {
    private final List<PropertyReference> list;

    ListBased(PropertyReference ... refs) {
      this.list = Collections.unmodifiableList(Arrays.asList(refs));
    }

    @Override
    public List<? extends PropertyReference> getProperties() {
      return list;
    }

    @Override
    public List<? extends PropertyReference> getInternalProperties() {
      return Collections.emptyList();
    }

    @Override
    public Object getAdditionalProperties() {
      return EMPTY_OBJECT;
    }
  }

  static final SubpropertiesMirror EMPTY = new SubpropertiesMirror() {
    @Override
    public List<? extends PropertyReference> getProperties() {
      return Collections.emptyList();
    }

    @Override
    public List<? extends PropertyReference> getInternalProperties() {
      return Collections.emptyList();
    }

    @Override
    public Object getAdditionalProperties() {
      return EMPTY_OBJECT;
    }
  };

  private static final Object EMPTY_OBJECT = new Object();
}
