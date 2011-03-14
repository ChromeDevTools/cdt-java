// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.rynda.RyndaExpressionBuilder.ObjectPropertyNameBuilder;
import org.chromium.sdk.internal.rynda.RyndaExpressionBuilder.PropertyNameBuilder;
import org.chromium.sdk.internal.rynda.RyndaExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.rynda.RyndaValueLoader.Getter;
import org.chromium.sdk.internal.rynda.RyndaValueLoader.ObjectProperties;
import org.chromium.sdk.internal.rynda.protocol.RyndaProtocol;
import org.chromium.sdk.internal.rynda.protocol.input.ValueData;
import org.chromium.sdk.internal.tools.v8.MethodIsBlockingException;
import org.chromium.sdk.util.AsyncFutureRef;

/**
 * A builder for implementations of {@link JsValue} and {@link JsVariable}.
 * It works in pair with {@link RyndaValueLoader}.
 */
class RyndaValueBuilder {
  private static final Logger LOGGER = Logger.getLogger(RyndaValueBuilder.class.getName());

  private final RyndaValueLoader valueLoader;

  RyndaValueBuilder(RyndaValueLoader valueLoader) {
    this.valueLoader = valueLoader;
  }

  public JsVariable createVariable(ValueData valueData, ValueNameBuilder nameBuilder) {
    JsValue jsValue = wrap(valueData, nameBuilder);
    return createVariable(jsValue, nameBuilder);
  }

  public JsValue wrap(ValueData valueData, ValueNameBuilder nameBuilder) {
    return getValueType(valueData).build(valueData, valueLoader, nameBuilder);
  }

  public static JsVariable createVariable(JsValue jsValue,
      ValueNameBuilder qualifiedNameBuilder) {
    return new VariableImpl(jsValue, qualifiedNameBuilder);
  }

  private static ValueType getValueType(ValueData valueData) {
    String protocolType = valueData.type();
    ValueType result = PROTOCOL_TYPE_TO_VALUE_TYPE.get(protocolType);

    if (result == null) {
      LOGGER.severe("Unexpected value type: " + protocolType);
      result = DEFAULT_VALUE_TYPE;
    }
    return result;
  }

  private static abstract class ValueType {
    abstract JsValue build(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder);
  }

  private static class PrimitiveType extends ValueType {
    private final JsValue.Type jsValueType;

    PrimitiveType(JsValue.Type jsValueType) {
      this.jsValueType = jsValueType;
    }

    @Override
    JsValue build(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder) {
      final String description = valueData.description();
      return new JsValue() {
        @Override public Type getType() {
          return jsValueType;
        }
        @Override public String getValueString() {
          return description;
        }
        @Override public JsObject asObject() {
          return null;
        }
        @Override public boolean isTruncated() {
          // TODO(peter.rybin): implement.
          return false;
        }
        @Override public void reloadHeavyValue(ReloadBiggerCallback callback,
            SyncCallback syncCallback) {
          // TODO(peter.rybin): implement.
          RyndaBrowserImpl.throwUnsupported();
        }
      };
    }
  }

  private static abstract class ObjectTypeBase extends ValueType {
    private final JsValue.Type jsValueType;

    ObjectTypeBase(Type jsValueType) {
      this.jsValueType = jsValueType;
    }

    @Override
    JsValue build(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder) {
      // TODO: Implement caching here.
      return buildNewInstance(valueData, valueLoader, nameBuilder);
    }

    abstract JsValue buildNewInstance(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder);

    abstract class JsObjectBase implements JsObject {
      private final ValueData valueData;
      private final RyndaValueLoader valueLoader;
      private final ValueNameBuilder nameBuilder;
      private final AsyncFutureRef<Getter<ObjectProperties>> loadedPropertiesRef =
          new AsyncFutureRef<Getter<ObjectProperties>>();

      JsObjectBase(ValueData valueData, RyndaValueLoader valueLoader,
          ValueNameBuilder nameBuilder) {
        this.valueData = valueData;
        this.valueLoader = valueLoader;
        this.nameBuilder = nameBuilder;
        if (RyndaProtocol.parseHasChildren(this.valueData.hasChildren())) {
          RyndaValueLoader.setEmptyJsObjectProperties(loadedPropertiesRef);
        }
      }

      @Override
      public Type getType() {
        return jsValueType;
      }

      @Override
      public String getValueString() {
        return valueData.description();
      }

      @Override
      public JsObject asObject() {
        return this;
      }

      @Override
      public boolean isTruncated() {
        return false;
      }

      @Override
      public void reloadHeavyValue(ReloadBiggerCallback callback,
          SyncCallback syncCallback) {
        // TODO(peter.rybin): implement.
        RyndaBrowserImpl.throwUnsupported();
      }

      @Override
      public String getClassName() {
        return RyndaBrowserImpl.throwUnsupported();
      }

      @Override
      public Collection<? extends JsVariable> getProperties()
          throws MethodIsBlockingException {
        return getLoadedProperties().properties();
      }

      @Override
      public Collection<? extends JsVariable> getInternalProperties()
          throws MethodIsBlockingException {
        return getLoadedProperties().internalProperties();
      }

      @Override
      public JsVariable getProperty(String name) {
        return RyndaBrowserImpl.throwUnsupported();
      }

      @Override
      public String getRefId() {
        ValueData.Id objectId = valueData.objectId();
        if (objectId == null) {
          return null;
        }
        return objectId.id() + "-" + objectId.groupName() + "-" + objectId.injectedScriptId();
      }

      protected ObjectProperties getLoadedProperties() {
        if (!loadedPropertiesRef.isInitialized()) {
          doLoadProperties();
        }
        return loadedPropertiesRef.getSync().get();
      }

      private void doLoadProperties() {
        PropertyNameBuilder innerNameBuilder = new ObjectPropertyNameBuilder(nameBuilder);
        valueLoader.loadJsObjectPropertiesAsync(valueData.objectId(), innerNameBuilder,
            loadedPropertiesRef);
      }
    }
  }

  private static class ObjectType extends ObjectTypeBase {
    ObjectType() {
      super(JsValue.Type.TYPE_OBJECT);
    }

    @Override
    JsValue buildNewInstance(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder) {
      return new ObjectTypeBase.JsObjectBase(valueData, valueLoader, nameBuilder) {
        @Override public JsArray asArray() {
          return null;
        }

        @Override public JsFunction asFunction() {
          return null;
        }
      };
    }
  }

  private static class ArrayType extends ObjectTypeBase {
    ArrayType() {
      super(JsValue.Type.TYPE_ARRAY);
    }

    @Override
    JsValue buildNewInstance(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder) {
      return new Array(valueData, valueLoader, nameBuilder);
    }

    private class Array extends JsObjectBase implements JsArray {
      private final AtomicReference<ArrayProperties> arrayPropertiesRef =
          new AtomicReference<ArrayProperties>(null);

      Array(ValueData valueData, RyndaValueLoader valueLoader,
          ValueNameBuilder nameBuilder) {
        super(valueData, valueLoader, nameBuilder);
      }

      @Override
      public JsArray asArray() {
        return this;
      }

      @Override
      public JsFunction asFunction() {
        return null;
      }

      @Override
      public int length() {
        return getArrayProperties().getLength();
      }

      @Override
      public JsVariable get(int index) throws MethodIsBlockingException {
        return getArrayProperties().getSparseArrayMap().get(index);
      }

      @Override
      public SortedMap<Integer, ? extends JsVariable> toSparseArray()
          throws MethodIsBlockingException {
        return getArrayProperties().getSparseArrayMap();
      }

      private ArrayProperties getArrayProperties() {
        ArrayProperties result = arrayPropertiesRef.get();
        if (result == null) {
          ArrayProperties arrayProperties = buildArrayProperties();
          // Only set if concurrent thread hasn't set its version
          arrayPropertiesRef.compareAndSet(null, arrayProperties);
          return arrayPropertiesRef.get();
        } else {
          return result;
        }
      }

      private ArrayProperties buildArrayProperties() {
        ObjectProperties loadedProperties = getLoadedProperties();
        final TreeMap<Integer, JsVariable> map = new TreeMap<Integer, JsVariable>();
        JsValue lengthValue = null;
        for (JsVariable variable : loadedProperties.properties()) {
          String name = variable.getName();
          if (RyndaExpressionBuilder.ALL_DIGITS.matcher(name).matches()) {
            Integer number = Integer.valueOf(name);
            map.put(number, variable);
          } else if ("length".equals(name)) {
            lengthValue = variable.getValue();
          }
        }
        int length;
        try {
          length = Integer.parseInt(lengthValue.getValueString());
        } catch (NumberFormatException e) {
          length = -1;
        }
        return new ArrayProperties(length, map);
      }
    }

    private static class ArrayProperties {
      final int length;
      final SortedMap<Integer, ? extends JsVariable> sparseArrayMap;

      ArrayProperties(int length,
          SortedMap<Integer, ? extends JsVariable> sparseArrayMap) {
        this.length = length;
        this.sparseArrayMap = sparseArrayMap;
      }
      public int getLength() {
        return length;
      }

      public SortedMap<Integer, ? extends JsVariable> getSparseArrayMap() {
        return sparseArrayMap;
      }
    }
  }

  private static class FunctionType extends ObjectTypeBase {
    FunctionType() {
      super(JsValue.Type.TYPE_FUNCTION);
    }

    @Override
    JsValue buildNewInstance(ValueData valueData, RyndaValueLoader valueLoader,
        ValueNameBuilder nameBuilder) {
      return new ObjectTypeBase.JsObjectBase(valueData, valueLoader, nameBuilder) {
        @Override public JsArray asArray() {
          return null;
        }

        @Override public JsFunction asFunction() {
          // TODO: make it a function, when backend provides data!
          return null;
        }
      };
    }
  }

  private static class VariableImpl implements JsVariable {
    private final JsValue jsValue;
    private final ValueNameBuilder qualifiedNameBuilder;
    private volatile String qualifiedName = null;

    public VariableImpl(JsValue jsValue, ValueNameBuilder qualifiedNameBuilder) {
      this.jsValue = jsValue;
      this.qualifiedNameBuilder = qualifiedNameBuilder;
    }

    @Override
    public boolean isReadable() {
      return true;
    }

    @Override
    public JsValue getValue() {
      return jsValue;
    }

    @Override
    public String getName() {
      return qualifiedNameBuilder.getShortName();
    }

    @Override
    public boolean isMutable() {
      return false;
    }

    @Override
    public void setValue(String newValue, SetValueCallback callback)
        throws UnsupportedOperationException {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getFullyQualifiedName() {
      String result = qualifiedName;
      if (result == null) {
        StringBuilder builder = new StringBuilder();
        qualifiedNameBuilder.append(builder);
        result = builder.toString();
        qualifiedName = result;
      }
      return result;
    }
  }

  private static final Map<String, ValueType> PROTOCOL_TYPE_TO_VALUE_TYPE;
  private static final ValueType DEFAULT_VALUE_TYPE;
  static {
    PROTOCOL_TYPE_TO_VALUE_TYPE = new HashMap<String, ValueType>();
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("string", new PrimitiveType(JsValue.Type.TYPE_STRING));
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("boolean", new PrimitiveType(JsValue.Type.TYPE_BOOLEAN));
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("number", new PrimitiveType(JsValue.Type.TYPE_NUMBER));
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("null", new PrimitiveType(JsValue.Type.TYPE_NULL));
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("undefined", new PrimitiveType(JsValue.Type.TYPE_UNDEFINED));

    ObjectType objectType = new ObjectType();
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("object", objectType);
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("array", new ArrayType());
    PROTOCOL_TYPE_TO_VALUE_TYPE.put("function", new FunctionType());

    PROTOCOL_TYPE_TO_VALUE_TYPE.put("error", objectType);

    DEFAULT_VALUE_TYPE = objectType;
  }
}
