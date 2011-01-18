// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Java dynamic-proxy based parser for set of json types. It uses set of type interfaces
 * as model description and provides implementations for them. JsonProtocolParser
 * converts JSONObject into a required Java type instance.
 */
public class JsonProtocolParser {
  private final Map<Class<?>, TypeHandler<?>> type2TypeHandler;

  /**
   * Constructs parser from a set of type interfaces.
   */
  public JsonProtocolParser(Class<?> ... protocolInterfaces)
      throws JsonProtocolModelParseException {
    this(Arrays.asList(protocolInterfaces), Collections.<JsonProtocolParser>emptyList());
  }

  /**
   * Constructs parser from a set of type interfaces and a list of base packages. Type interfaces
   * may reference to type interfaces from base packages.
   * @param basePackages list of base packages in form of list of {@link JsonProtocolParser}'s
   */
  public JsonProtocolParser(List<? extends Class<?>> protocolInterfaces,
      List<? extends JsonProtocolParser> basePackages) throws JsonProtocolModelParseException {
    type2TypeHandler = readTypes(protocolInterfaces, basePackages);
  }

  /**
   * Parses {@link JSONObject} as typeClass type.
   */
  public <T> T parse(JSONObject object, Class<T> typeClass) throws JsonProtocolParseException {
    return parseAnything(object, typeClass);
  }

  /**
   * Parses any object as typeClass type. Non-JSONObject only makes sense for
   * types with {@link JsonType#subtypesChosenManually()} = true annotation.
   */
  public <T> T parseAnything(Object object, Class<T> typeClass) throws JsonProtocolParseException {
    TypeHandler<T> type = type2TypeHandler.get(typeClass).cast(typeClass);
    return type.parseRoot(object);
  }

  private static Map<Class<?>, TypeHandler<?>> readTypes(
      List<? extends Class<?>> protocolInterfaces,
      final List<? extends JsonProtocolParser> basePackages)
      throws JsonProtocolModelParseException {
    ReadInterfacesSession session = new ReadInterfacesSession(protocolInterfaces, basePackages);
    session.go();
    return session.getResult();
  }


  private static class ReadInterfacesSession {
    private final Map<Class<?>, TypeHandler<?>> type2typeHandler;
    private final List<? extends JsonProtocolParser> basePackages;

    final List<RefImpl<?>> refs = new ArrayList<RefImpl<?>>();
    final List<SubtypeCaster> subtypeCasters =
        new ArrayList<SubtypeCaster>();

    ReadInterfacesSession(List<? extends Class<?>> protocolInterfaces,
        List<? extends JsonProtocolParser> basePackages) {
      this.type2typeHandler = new HashMap<Class<?>, TypeHandler<?>>();
      this.basePackages = basePackages;

      for (Class<?> typeClass : protocolInterfaces) {
        type2typeHandler.put(typeClass, null);
      }
    }

    void go() throws JsonProtocolModelParseException {
      // Create TypeHandler's.
      for (Class<?> typeClass : type2typeHandler.keySet()) {
        TypeHandler<?> typeHandler = createTypeHandler(typeClass);
        type2typeHandler.put(typeClass, typeHandler);
      }

      // Resolve cross-references.
      for (RefImpl<?> ref : refs) {
        TypeHandler<?> type = type2typeHandler.get(ref.typeClass);
        if (type == null) {
          throw new RuntimeException();
        }
        ref.set(type);
      }

      // Set subtype casters.
      for (SubtypeCaster subtypeCaster : subtypeCasters) {
        TypeHandler<?> subtypeHandler = subtypeCaster.getSubtypeHandler();
        subtypeHandler.getSubtypeSupport().setSubtypeCaster(subtypeCaster);
      }

      // Check subtype casters consistency.
      for (TypeHandler<?> type : type2typeHandler.values()) {
        type.getSubtypeSupport().checkHasSubtypeCaster();
      }
    }

    Map<Class<?>, TypeHandler<?>> getResult() {
      return type2typeHandler;
    }

    private <T> TypeHandler<T> createTypeHandler(Class<T> typeClass)
        throws JsonProtocolModelParseException {
      if (!typeClass.isInterface()) {
        throw new JsonProtocolModelParseException("Json model type should be interface: " +
            typeClass.getName());
      }

      FieldProcessor<T> fields = new FieldProcessor<T>(typeClass);

      fields.go();

      Map<Method, MethodHandler> methodHandlerMap = fields.getMethodHandlerMap();
      methodHandlerMap.putAll(BaseHandlersLibrary.INSTANCE.getAllHandlers());

      TypeHandler.EagerFieldParser eagerFieldParser =
          new EagerFieldParserImpl(fields.getOnDemandHanlers());

      RefToType<?> superclassRef = getSuperclassRef(typeClass);

      return new TypeHandler<T>(typeClass, superclassRef,
          fields.getFieldArraySize(), methodHandlerMap, fields.getFieldLoaders(),
          fields.getFieldConditions(), eagerFieldParser, fields.getAlgCasesData());
    }

    private SlowParser<?> getFieldTypeParser(Type type, boolean declaredNullable,
        boolean isSubtyping) throws JsonProtocolModelParseException {
      if (type instanceof Class) {
        Class<?> typeClass = (Class<?>) type;
        if (type == Long.class) {
          nullableIsNotSupported(declaredNullable);
          return LONG_PARSER.getNullable();
        } else if (type == Long.TYPE) {
          nullableIsNotSupported(declaredNullable);
          return LONG_PARSER.getNotNullable();
        } else if (type == Boolean.class) {
          nullableIsNotSupported(declaredNullable);
          return BOOLEAN_PARSER.getNullable();
        } else if (type == Boolean.TYPE) {
          nullableIsNotSupported(declaredNullable);
          return BOOLEAN_PARSER.getNotNullable();
        } else if (type == Float.class) {
          nullableIsNotSupported(declaredNullable);
          return FLOAT_PARSER.getNullable();
        } else if (type == Float.TYPE) {
          nullableIsNotSupported(declaredNullable);
          return FLOAT_PARSER.getNotNullable();
        } else if (type == Void.class) {
          nullableIsNotSupported(declaredNullable);
          return VOID_PARSER;
        } else if (type == String.class) {
          return STRING_PARSER.get(declaredNullable);
        } else if (type == Object.class) {
          return OBJECT_PARSER.get(declaredNullable);
        } else if (type == JSONObject.class) {
          return JSON_PARSER.get(declaredNullable);
        } else if (typeClass.isEnum()) {
          Class<RetentionPolicy> enumTypeClass = (Class<RetentionPolicy>) typeClass;
          return EnumParser.create(enumTypeClass, declaredNullable);
        } else if (type2typeHandler.containsKey(typeClass)) {
        }
        RefToType<?> ref = getTypeRef(typeClass);
        if (ref != null) {
          return createJsonParser(ref, declaredNullable, isSubtyping);
        }
        throw new JsonProtocolModelParseException("Method return type " + type +
            " (simple class) not supported");
      } else if (type instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (parameterizedType.getRawType() == List.class) {
          Type argumentType = parameterizedType.getActualTypeArguments()[0];
          if (argumentType instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) argumentType;
            if (wildcard.getLowerBounds().length == 0 && wildcard.getUpperBounds().length == 1) {
              argumentType = wildcard.getUpperBounds()[0];
            }
          }
          SlowParser<?> componentParser = getFieldTypeParser(argumentType, false, false);
          return createArrayParser(componentParser, declaredNullable);
        } else {
          throw new JsonProtocolModelParseException("Method return type " + type +
              " (generic) not supported");
        }
      } else {
        throw new JsonProtocolModelParseException("Method return type " + type + " not supported");
      }
    }

    private void nullableIsNotSupported(boolean declaredNullable)
        throws JsonProtocolModelParseException {
      if (declaredNullable) {
        throw new JsonProtocolModelParseException("The type cannot be declared nullable");
      }
    }

    private <T> JsonTypeParser<T> createJsonParser(RefToType<T> type, boolean isNullable,
        boolean isSubtyping) {
      return new JsonTypeParser<T>(type, isNullable, isSubtyping);
    }

    private <T> ArrayParser<T> createArrayParser(SlowParser<T> componentParser,
        boolean isNullable) {
      return new ArrayParser<T>(componentParser, isNullable);
    }

    private <T> RefToType<T> getTypeRef(final Class<T> typeClass) {
      if (type2typeHandler.containsKey(typeClass)) {
        RefImpl<T> result = new RefImpl<T>(typeClass);
        refs.add(result);
        return result;
      }
      for (JsonProtocolParser baseParser : basePackages) {
        TypeHandler<?> typeHandler = baseParser.type2TypeHandler.get(typeClass);
        if (typeHandler != null) {
          final TypeHandler<T> typeHandlerT = (TypeHandler<T>) typeHandler;
          return new RefToType<T>() {
            @Override
            TypeHandler<T> get() {
              return typeHandlerT;
            }
            @Override
            Class<?> getTypeClass() {
              return typeClass;
            }
          };
        }
      }
      return null;
    }

    private RefToType<?> getSuperclassRef(Class<?> typeClass)
        throws JsonProtocolModelParseException {
      RefToType<?> result = null;
      for (Type interfc : typeClass.getGenericInterfaces()) {
        if (interfc instanceof ParameterizedType == false) {
          continue;
        }
        ParameterizedType parameterizedType = (ParameterizedType) interfc;
        if (parameterizedType.getRawType() != JsonSubtype.class) {
          continue;
        }
        Type param = parameterizedType.getActualTypeArguments()[0];
        if (param instanceof Class == false) {
          throw new JsonProtocolModelParseException("Unexpected type of superclass " + param);
        }
        Class<?> paramClass = (Class<?>) param;
        if (result != null) {
          throw new JsonProtocolModelParseException("Already has superclass " +
              result.getTypeClass().getName());
        }
        result = getTypeRef(paramClass);
        if (result == null) {
          throw new JsonProtocolModelParseException("Unknown base class " + paramClass.getName());
        }
      }
      return result;
    }

    class FieldProcessor<T> {
      private final Class<T> typeClass;

      private final JsonType jsonTypeAnn;
      private final List<FieldLoader> fieldLoaders = new ArrayList<FieldLoader>(2);
      private final List<LazyParseFieldMethodHandler> onDemandHanlers =
          new ArrayList<LazyParseFieldMethodHandler>();
      private final Map<Method, MethodHandler> methodHandlerMap =
          new HashMap<Method, MethodHandler>();
      private final FieldMap fieldMap = new FieldMap();
      private final List<FieldCondition> fieldConditions = new ArrayList<FieldCondition>(2);
      private AlgebraicCasesDataImpl algCasesData = null;
      private int fieldArraySize = 0;

      FieldProcessor(Class<T> typeClass) throws JsonProtocolModelParseException {
        this.typeClass = typeClass;
        jsonTypeAnn = typeClass.getAnnotation(JsonType.class);
        if (jsonTypeAnn == null) {
          throw new JsonProtocolModelParseException("Not a json model type: " + typeClass);
        }
      }

      void go() throws JsonProtocolModelParseException {
        for (Method m : typeClass.getDeclaredMethods()) {
          try {
            processMethod(m);
          } catch (JsonProtocolModelParseException e) {
            throw new JsonProtocolModelParseException("Problem with method " + m, e);
          }
        }
      }

      private void processMethod(Method m) throws JsonProtocolModelParseException {
        if (m.getParameterTypes().length != 0) {
          throw new JsonProtocolModelParseException("No parameters expected in " + m);
        }
        JsonOverrideField overrideFieldAnn = m.getAnnotation(JsonOverrideField.class);
        FieldConditionLogic fieldConditionLogic = FieldConditionLogic.readLogic(m);
        String fieldName = checkAndGetJsonFieldName(m);
        MethodHandler methodHandler;

        JsonSubtypeCasting jsonSubtypeCaseAnn = m.getAnnotation(JsonSubtypeCasting.class);
        if (jsonSubtypeCaseAnn != null) {
          if (fieldConditionLogic != null) {
            throw new JsonProtocolModelParseException(
                "Subtype condition annotation only works with field getter methods");
          }
          if (overrideFieldAnn != null) {
            throw new JsonProtocolModelParseException(
                "Override annotation only works with field getter methods");
          }
          if (algCasesData == null) {
            algCasesData = new AlgebraicCasesDataImpl(jsonTypeAnn.subtypesChosenManually());
          }

          if (jsonTypeAnn.subtypesChosenManually()) {
            methodHandler = processManualSubtypeMethod(m, jsonSubtypeCaseAnn);
          } else {
            if (jsonSubtypeCaseAnn.reinterpret()) {
              throw new JsonProtocolModelParseException(
                  "Option 'reinterpret' is only available with 'subtypes chosen manually'");
            }
            methodHandler = processAutomaticSubtypeMethod(m);
          }

        } else {
          methodHandler = processFieldGetterMethod(m, fieldConditionLogic, overrideFieldAnn,
              fieldName);
        }
        methodHandlerMap.put(m, methodHandler);
      }

      private MethodHandler processFieldGetterMethod(Method m,
          FieldConditionLogic fieldConditionLogic, JsonOverrideField overrideFieldAnn,
          String fieldName) throws JsonProtocolModelParseException {
        MethodHandler methodHandler;
        JsonNullable nullableAnn = m.getAnnotation(JsonNullable.class);
        SlowParser<?> fieldTypeParser = getFieldTypeParser(m.getGenericReturnType(),
            nullableAnn != null, false);
        if (fieldConditionLogic != null) {
          fieldConditions.add(new FieldCondition(fieldName, fieldTypeParser.asQuickParser(),
              fieldConditionLogic));
        }
        if (overrideFieldAnn == null) {
          fieldMap.localNames.add(fieldName);
        } else {
          fieldMap.overridenNames.add(fieldName);
        }

        boolean isOptional = isOptionalField(m);

        if (fieldTypeParser.asQuickParser() != null) {
          LazyParseFieldMethodHandler onDemandHandler = new LazyParseFieldMethodHandler(
              fieldTypeParser.asQuickParser(), isOptional, fieldName, typeClass);
          onDemandHanlers.add(onDemandHandler);
          methodHandler = onDemandHandler;
        } else {
          int fieldCode = allocateFieldInArray();
          FieldLoader fieldLoader = new FieldLoader(fieldCode, fieldName, fieldTypeParser,
              isOptional);
          fieldLoaders.add(fieldLoader);
          methodHandler = new PreparsedFieldMethodHandler(fieldCode,
              fieldTypeParser.getValueFinisher());
        }
        return methodHandler;
      }

      private MethodHandler processAutomaticSubtypeMethod(Method m)
          throws JsonProtocolModelParseException {
        MethodHandler methodHandler;
        if (m.getReturnType() == Void.TYPE) {
          if (algCasesData.hasDefaultCase) {
            throw new JsonProtocolModelParseException("Duplicate default case method: " + m);
          }
          algCasesData.hasDefaultCase = true;
          methodHandler = RETURN_NULL_METHOD_HANDLER;
        } else {
          Class<?> methodType = m.getReturnType();
          RefToType<?> ref = getTypeRef(methodType);
          if (ref == null) {
            throw new JsonProtocolModelParseException("Unknown return type in " + m);
          }
          if (algCasesData.variantCodeFieldPos == -1) {
            algCasesData.variantCodeFieldPos = allocateFieldInArray();
            algCasesData.variantValueFieldPos = allocateFieldInArray();
          }
          int algCode = algCasesData.subtypes.size();
          algCasesData.subtypes.add(ref);
          final AutoSubtypeMethodHandler algMethodHandler = new AutoSubtypeMethodHandler(
              algCasesData.variantCodeFieldPos, algCasesData.variantValueFieldPos,
              algCode);
          methodHandler = algMethodHandler;

          SubtypeCaster subtypeCaster = new SubtypeCaster(typeClass, ref) {
            @Override
            ObjectData getSubtypeObjectData(ObjectData objectData) {
              return algMethodHandler.getFieldObjectData(objectData);
            }
          };

          subtypeCasters.add(subtypeCaster);
        }
        return methodHandler;
      }


      private MethodHandler processManualSubtypeMethod(Method m,
          JsonSubtypeCasting jsonSubtypeCaseAnn) throws JsonProtocolModelParseException {
        int fieldCode = allocateFieldInArray();

        SlowParser<?> fieldTypeParser = getFieldTypeParser(m.getGenericReturnType(), false,
            !jsonSubtypeCaseAnn.reinterpret());

        if (!Arrays.asList(m.getExceptionTypes()).contains(JsonProtocolParseException.class)) {
          throw new JsonProtocolModelParseException(
              "Method should declare JsonProtocolParseException exception: " + m);
        }

        final ManualSubtypeMethodHandler handler = new ManualSubtypeMethodHandler(fieldCode,
            fieldTypeParser);
        JsonTypeParser<?> parserAsJsonTypeParser = fieldTypeParser.asJsonTypeParser();
        if (parserAsJsonTypeParser != null && parserAsJsonTypeParser.isSubtyping()) {
          SubtypeCaster subtypeCaster = new SubtypeCaster(typeClass,
              parserAsJsonTypeParser.getType()) {
            @Override
            ObjectData getSubtypeObjectData(ObjectData baseObjectData)
                throws JsonProtocolParseException {
              ObjectData objectData = baseObjectData;
              return handler.getSubtypeData(objectData);
            }
          };
          subtypeCasters.add(subtypeCaster);
        }
        return handler;
      }

      int getFieldArraySize() {
        return fieldArraySize;
      }

      void setFieldArraySize(int fieldArraySize) {
        this.fieldArraySize = fieldArraySize;
      }

      AlgebraicCasesDataImpl getAlgCasesData() {
        return algCasesData;
      }

      void setAlgCasesData(AlgebraicCasesDataImpl algCasesData) {
        this.algCasesData = algCasesData;
      }

      List<FieldLoader> getFieldLoaders() {
        return fieldLoaders;
      }

      List<LazyParseFieldMethodHandler> getOnDemandHanlers() {
        return onDemandHanlers;
      }

      Map<Method, MethodHandler> getMethodHandlerMap() {
        return methodHandlerMap;
      }

      List<FieldCondition> getFieldConditions() {
        return fieldConditions;
      }

      private int allocateFieldInArray() {
        return fieldArraySize++;
      }

      private boolean isOptionalField(Method m) {
        JsonOptionalField jsonOptionalFieldAnn = m.getAnnotation(JsonOptionalField.class);
        return jsonOptionalFieldAnn != null;
      }

      private String checkAndGetJsonFieldName(Method m) throws JsonProtocolModelParseException {
        if (m.getParameterTypes().length != 0) {
          throw new JsonProtocolModelParseException("Must have 0 parameters");
        }
        JsonField fieldAnn = m.getAnnotation(JsonField.class);
        if (fieldAnn != null) {
          return fieldAnn.jsonLiteralName();
        }
        String name = m.getName();
        if (name.startsWith("get") && name.length() > 3) {
          name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return name;
      }
    }
  }

  private static class EagerFieldParserImpl extends TypeHandler.EagerFieldParser {
    private final List<LazyParseFieldMethodHandler> onDemandHandlers;

    private EagerFieldParserImpl(List<LazyParseFieldMethodHandler> onDemandHandlers) {
      this.onDemandHandlers = onDemandHandlers;
    }

    @Override
    void parseAllFields(ObjectData objectData) throws JsonProtocolParseException {
      for (LazyParseFieldMethodHandler handler : onDemandHandlers) {
        handler.parse(objectData);
      }
    }
  }

  private static class LazyParseFieldMethodHandler extends MethodHandler {
    private final QuickParser<?> quickParser;
    private final boolean isOptional;
    private final String fieldName;
    private final Class<?> typeClass;

    LazyParseFieldMethodHandler(QuickParser<?> quickParser, boolean isOptional, String fieldName,
        Class<?> typeClass) {
      this.quickParser = quickParser;
      this.isOptional = isOptional;
      this.fieldName = fieldName;
      this.typeClass = typeClass;
    }

    @Override
    Object handle(Object myself, ObjectData objectData, Object[] args) {
      try {
        return parse(objectData);
      } catch (JsonProtocolParseException e) {
        throw new JsonProtocolParseRuntimeException(
            "On demand parsing failed for " + objectData.getUnderlyingObject(), e);
      }
    }

    public Object parse(ObjectData objectData) throws JsonProtocolParseException {
      Map<?,?> properties = (JSONObject)objectData.getUnderlyingObject();
      Object value = properties.get(fieldName);
      boolean hasValue;
      if (value == null) {
        hasValue = properties.containsKey(fieldName);
      } else {
        hasValue = true;
      }
      return parse(hasValue, value, objectData);
    }

    public Object parse(boolean hasValue, Object value, ObjectData objectData)
        throws JsonProtocolParseException {
      if (hasValue) {
        try {
          return quickParser.parseValueQuick(value);
        } catch (JsonProtocolParseException e) {
          throw new JsonProtocolParseException("Failed to parse field " + fieldName + " in type " +
              typeClass.getName(), e);
        }
      } else {
        if (!isOptional) {
          throw new JsonProtocolParseException("Field is not optional: " + fieldName +
              " (in type " + typeClass.getName() + ")");
        }
        return null;
      }
    }
  }

  private static class PreparsedFieldMethodHandler extends MethodHandler {
    private final int pos;
    private final FieldLoadedFinisher valueFinisher;

    PreparsedFieldMethodHandler(int pos, FieldLoadedFinisher valueFinisher) {
      this.pos = pos;
      this.valueFinisher = valueFinisher;
    }

    @Override
    Object handle(Object myself, ObjectData objectData, Object[] args) throws Throwable {
      Object val = objectData.getFieldArray()[pos];
      if (valueFinisher != null) {
        val = valueFinisher.getValueForUser(val);
      }
      return val;
    }
  }

  static SlowParser<Void> VOID_PARSER = new QuickParser<Void>() {
    @Override
    public Void parseValueQuick(Object value) {
      return null;
    }
  };

  static class SimpleCastParser<T> extends QuickParser<T> {
    private final boolean nullable;
    private final Class<T> fieldType;

    SimpleCastParser(Class<T> fieldType, boolean nullable) {
      this.fieldType = fieldType;
      this.nullable = nullable;
    }

    @Override
    public T parseValueQuick(Object value) throws JsonProtocolParseException {
      if (value == null) {
        if (nullable) {
          return null;
        } else {
          throw new JsonProtocolParseException("Field must have type " + fieldType.getName());
        }
      }
      try {
        return fieldType.cast(value);
      } catch (ClassCastException e) {
        throw new JsonProtocolParseException("Field must have type " + fieldType.getName(), e);
      }
    }

    @Override
    public FieldLoadedFinisher getValueFinisher() {
      return null;
    }
  }
  static class SimpleParserPair<T> {
    static <T> SimpleParserPair<T> create(Class<T> fieldType) {
      return new SimpleParserPair<T>(fieldType);
    }

    private final SimpleCastParser<T> nullable;
    private final SimpleCastParser<T> notNullable;

    private SimpleParserPair(Class<T> fieldType) {
      nullable = new SimpleCastParser<T>(fieldType, true);
      notNullable = new SimpleCastParser<T>(fieldType, false);
    }

    SimpleCastParser<T> getNullable() {
      return nullable;
    }

    SimpleCastParser<T> getNotNullable() {
      return notNullable;
    }

    SlowParser<?> get(boolean declaredNullable) {
      return declaredNullable ? nullable : notNullable;
    }
  }

  private static SimpleParserPair<Long> LONG_PARSER = SimpleParserPair.create(Long.class);
  private static SimpleParserPair<Boolean> BOOLEAN_PARSER = SimpleParserPair.create(Boolean.class);
  private static SimpleParserPair<Float> FLOAT_PARSER = SimpleParserPair.create(Float.class);
  private static SimpleParserPair<String> STRING_PARSER = SimpleParserPair.create(String.class);
  private static SimpleParserPair<Object> OBJECT_PARSER = SimpleParserPair.create(Object.class);
  private static SimpleParserPair<JSONObject> JSON_PARSER =
      SimpleParserPair.create(JSONObject.class);

  static class ArrayParser<T> extends SlowParser<List<? extends T>> {
    private final SlowParser<T> componentParser;
    private final boolean isNullable;

    ArrayParser(SlowParser<T> componentParser, boolean isNullable) {
      this.componentParser = componentParser;
      this.isNullable = isNullable;
    }

    @Override
    public List<? extends T> parseValue(Object value, ObjectData thisData)
        throws JsonProtocolParseException {
      if (isNullable && value == null) {
        return null;
      }
      if (value instanceof JSONArray == false) {
        throw new JsonProtocolParseException("Array value expected");
      }
      JSONArray arrayValue = (JSONArray) value;
      int size = arrayValue.size();
      List list = new ArrayList<Object>(size);
      FieldLoadedFinisher valueFinisher = componentParser.getValueFinisher();
      for (int i = 0; i < size; i++) {
        // We do not support super object for array component.
        Object val = componentParser.parseValue(arrayValue.get(i), null);
        if (valueFinisher != null) {
          val = valueFinisher.getValueForUser(val);
        }
        list.add(val);
      }
      return Collections.unmodifiableList(list);
    }
    @Override
    public FieldLoadedFinisher getValueFinisher() {
      return null;
    }
    @Override
    public JsonTypeParser<?> asJsonTypeParser() {
      return null;
    }
  }

  static MethodHandler RETURN_NULL_METHOD_HANDLER = new MethodHandler() {
    @Override
    Object handle(Object myself, ObjectData objectData, Object[] args) throws Throwable {
      return null;
    }
  };

  static class AutoSubtypeMethodHandler extends MethodHandler {
    private final int variantCodeField;
    private final int variantValueField;
    private final int code;

    AutoSubtypeMethodHandler(int variantCodeField, int variantValueField, int code) {
      this.variantCodeField = variantCodeField;
      this.variantValueField = variantValueField;
      this.code = code;
    }

    ObjectData getFieldObjectData(ObjectData objectData) {
      Object[] array = objectData.getFieldArray();
      Integer actualCode = (Integer) array[variantCodeField];
      if (this.code == actualCode) {
        ObjectData data = (ObjectData) array[variantValueField];
        return data;
      } else {
        return null;
      }
    }

    @Override
    Object handle(Object myself, ObjectData objectData, Object[] args) {
      ObjectData resData = getFieldObjectData(objectData);
      if (resData == null) {
        return null;
      } else {
        return resData.getProxy();
      }
    }
  }

  static class ManualSubtypeMethodHandler extends MethodHandler {
    private final int fieldPos;
    private final SlowParser<?> parser;

    ManualSubtypeMethodHandler(int fieldPos, SlowParser<?> slowParser) {
      this.fieldPos = fieldPos;
      this.parser = slowParser;
    }

    @Override
    Object handle(Object myself, ObjectData objectData, Object[] args)
        throws JsonProtocolParseException {
      return handle(objectData);
    }

    private Object handleRaw(ObjectData objectData) throws JsonProtocolParseException {
      Object cachedValue = objectData.getFieldArray()[fieldPos];
      if (cachedValue == null) {
        cachedValue = parser.parseValue(objectData.getUnderlyingObject(), objectData);
        objectData.getFieldArray()[fieldPos] = cachedValue;
      }
      return cachedValue;
    }

    Object handle(ObjectData objectData) throws JsonProtocolParseException {
      Object res = handleRaw(objectData);
      FieldLoadedFinisher valueFinisher = parser.getValueFinisher();
      if (valueFinisher != null) {
         res = valueFinisher.getValueForUser(res);
      }
      return res;
    }

    ObjectData getSubtypeData(ObjectData objectData) throws JsonProtocolParseException {
      return (ObjectData) handleRaw(objectData);
    }
  }

  static class AlgebraicCasesDataImpl extends TypeHandler.AlgebraicCasesData {
    private int variantCodeFieldPos = -1;
    private int variantValueFieldPos = -1;
    private boolean hasDefaultCase = false;
    private final List<RefToType<?>> subtypes = new ArrayList<RefToType<?>>();
    private final boolean isManualChoose;

    AlgebraicCasesDataImpl(boolean isManualChoose) {
      this.isManualChoose = isManualChoose;
    }

    @Override
    int getVariantCodeFieldPos() {
      return variantCodeFieldPos;
    }

    @Override
    int getVariantValueFieldPos() {
      return variantValueFieldPos;
    }

    @Override
    boolean hasDefaultCase() {
      return hasDefaultCase;
    }

    @Override
    List<RefToType<?>> getSubtypes() {
      return subtypes;
    }

    @Override
    boolean isManualChoose() {
      return isManualChoose;
    }
  }

  private static class RefImpl<T> extends RefToType<T> {
    private final Class<T> typeClass;
    private TypeHandler<T> type = null;

    RefImpl(Class<T> typeClass) {
      this.typeClass = typeClass;
    }

    @Override
    Class<?> getTypeClass() {
      return typeClass;
    }

    @Override
    TypeHandler<T> get() {
      return type;
    }

    void set(TypeHandler<?> type) {
      this.type = (TypeHandler<T>)type;
    }
  }

  // We should use it for static analysis later.
  private static class FieldMap {
    final List<String> localNames = new ArrayList<String>(5);
    final List<String> overridenNames = new ArrayList<String>(1);
  }

  private static class JsonProtocolParseRuntimeException extends RuntimeException {
    JsonProtocolParseRuntimeException() {
    }
    JsonProtocolParseRuntimeException(String message, Throwable cause) {
      super(message, cause);
    }
    JsonProtocolParseRuntimeException(String message) {
      super(message);
    }
    JsonProtocolParseRuntimeException(Throwable cause) {
      super(cause);
    }
  }
}
