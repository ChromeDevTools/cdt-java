// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.internal.protocol.data.FunctionValueHandle;
import org.chromium.sdk.internal.protocol.data.ObjectValueHandle;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;

/**
 * This class is intended to hold properties either already parsed or to be parsed on demand.
 */
public abstract class SubpropertiesMirror {
  public abstract List<? extends PropertyReference> getProperties();

  public abstract List<? extends PropertyReference> getInternalProperties();

  public abstract Object getAdditionalProperties();

  public static class ObjectValueBased extends JsonBased<ObjectValueHandle> {
    private final ObjectValueHandle objectValueHandle;
    public ObjectValueBased(ObjectValueHandle valueHandle,
        AdditionalPropertyFactory<ObjectValueHandle> additionalPropertyFactory) {
      super(additionalPropertyFactory);
      this.objectValueHandle = valueHandle;
    }
    @Override
    protected ObjectValueHandle getObjectForFactory() {
      return objectValueHandle;
    }
    @Override
    protected ObjectValueHandle getObjectValue() {
      return objectValueHandle;
    }
  }
  public static class FunctionValueBased extends JsonBased<FunctionValueHandle> {
    private final FunctionValueHandle functionValueHandle;
    public FunctionValueBased(FunctionValueHandle functionValueHandle,
        AdditionalPropertyFactory<FunctionValueHandle> additionalPropertyFactory) {
      super(additionalPropertyFactory);
      this.functionValueHandle = functionValueHandle;
    }
    @Override
    protected FunctionValueHandle getObjectForFactory() {
      return functionValueHandle;
    }
    @Override
    protected ObjectValueHandle getObjectValue() {
      return functionValueHandle.getSuper();
    }
  }

  /**
   * Keeps properties in for of JSON and parses JSON on demand.
   */
  public static abstract class JsonBased<T> extends SubpropertiesMirror {
    private final AdditionalPropertyFactory<T> additionalPropertyFactory;

    private List<? extends PropertyReference> properties = null;
    private List<? extends PropertyReference> internalProperties = null;
    private Object additionalProperties = null;

    public JsonBased(AdditionalPropertyFactory<T> additionalPropertyFactory) {
      if (additionalPropertyFactory == null) {
        additionalPropertyFactory = NO_OP_FACTORY;
      }
      this.additionalPropertyFactory = additionalPropertyFactory;
    }

    @Override
    public synchronized List<? extends PropertyReference> getProperties() {
      if (properties == null) {
        properties = V8ProtocolUtil.extractObjectProperties(getObjectValue());
      }
      return properties;
    }

    @Override
    public synchronized List<? extends PropertyReference> getInternalProperties() {
      if (internalProperties == null) {
        internalProperties = V8ProtocolUtil.extractObjectInternalProperties(getObjectValue());
      }
      return internalProperties;
    }

    protected abstract ObjectValueHandle getObjectValue();

    @Override
    public Object getAdditionalProperties() {
      if (additionalProperties == null) {
        additionalProperties =
            additionalPropertyFactory.createAdditionalProperties(getObjectForFactory());
      }
      return additionalProperties;
    }
    protected abstract T getObjectForFactory();

    public interface AdditionalPropertyFactory<T> {
      Object createAdditionalProperties(T jsonWithProperties);
    }

    private static AdditionalPropertyFactory NO_OP_FACTORY = new AdditionalPropertyFactory<Void>() {
      public Object createAdditionalProperties(Void jsonWithProperties) {
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
