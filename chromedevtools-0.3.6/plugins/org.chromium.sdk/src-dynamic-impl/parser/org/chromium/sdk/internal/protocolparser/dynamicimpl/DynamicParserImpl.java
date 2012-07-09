// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.chromium.sdk.internal.protocolparser.FieldLoadStrategy;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonObjectBased;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.ClassScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.FileScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.GlobalScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.MethodScope;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.JavaCodeGenerator.Util;
import org.chromium.sdk.internal.protocolparser.implutil.CommonImpl.ParseRuntimeException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Java dynamic-proxy based implementation of {@link JsonProtocolParser}. It analyses
 * interfaces with reflection and provides their implementation by {@link Proxy} factory.
 * User-friendly 'root' interface is available by {@link #getParserRoot()} method.
 * @param <ROOT> root user-provided type (see {@link JsonParserRoot})
 */
public class DynamicParserImpl<ROOT> {
  private final Map<Class<?>, TypeHandler<?>> type2TypeHandler;
  private final ParserRootImpl<ROOT> rootImpl;

  /**
   * Constructs parser from a set of type interfaces.
   */
  public DynamicParserImpl(Class<ROOT> parserRootClass, List<Class<?>> protocolInterfaces)
      throws JsonProtocolModelParseException {
    this(parserRootClass, protocolInterfaces, Collections.<DynamicParserImpl<?>>emptyList());
  }

  /**
   * Constructs parser from a set of type interfaces and a list of base packages. Type interfaces
   * may reference to type interfaces from base packages.
   * @param basePackages list of base packages in form of list of {@link DynamicParserImpl}'s
   */
  public DynamicParserImpl(Class<ROOT> parserRootClass,
      List<? extends Class<?>> protocolInterfaces,
      List<? extends DynamicParserImpl<?>> basePackages) throws JsonProtocolModelParseException {
    this(parserRootClass, protocolInterfaces, basePackages, false);
  }

  public DynamicParserImpl(Class<ROOT> parserRootClass,
      List<? extends Class<?>> protocolInterfaces,
      List<? extends DynamicParserImpl<?>> basePackages, boolean strictMode)
      throws JsonProtocolModelParseException {
    type2TypeHandler = readTypes(protocolInterfaces, basePackages, strictMode);
    rootImpl = new ParserRootImpl<ROOT>(parserRootClass, type2TypeHandler);
  }

  public ROOT getParserRoot() {
    return rootImpl.getInstance();
  }

  private static Map<Class<?>, TypeHandler<?>> readTypes(
      List<? extends Class<?>> protocolInterfaces,
      final List<? extends DynamicParserImpl<?>> basePackages, boolean strictMode)
      throws JsonProtocolModelParseException {
    ReadInterfacesSession session =
        new ReadInterfacesSession(protocolInterfaces, basePackages, strictMode);
    session.go();
    return session.getResult();
  }


  private static class ReadInterfacesSession {
    private final Map<Class<?>, TypeHandler<?>> type2typeHandler;
    private final List<? extends DynamicParserImpl<?>> basePackages;
    private final boolean strictMode;

    final List<RefImpl<?>> refs = new ArrayList<RefImpl<?>>();
    final List<SubtypeCaster> subtypeCasters =
        new ArrayList<SubtypeCaster>();

    ReadInterfacesSession(List<? extends Class<?>> protocolInterfaces,
        List<? extends DynamicParserImpl<?>> basePackages, boolean strictMode) {
      // Keep interfaces ordered to keep generated parser less random.
      this.type2typeHandler = new LinkedHashMap<Class<?>, TypeHandler<?>>();
      this.basePackages = basePackages;
      this.strictMode = strictMode;

      for (Class<?> typeClass : protocolInterfaces) {
        if (type2typeHandler.containsKey(typeClass)) {
          throw new IllegalArgumentException(
              "Protocol interface duplicated " + typeClass.getName());
        }
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

      if (strictMode) {
        for (TypeHandler<?> type : type2typeHandler.values()) {
          type.buildClosedNameSet();
        }
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

      boolean requiresJsonObject = fields.requiresJsonObject() ||
          JsonObjectBased.class.isAssignableFrom(typeClass);

      return new TypeHandler<T>(typeClass, superclassRef,
          fields.getFieldArraySize(), fields.getVolatileFields(), methodHandlerMap,
          fields.getFieldLoaders(),
          fields.getFieldConditions(), eagerFieldParser, fields.getAlgCasesData(),
          requiresJsonObject, strictMode);
    }

    private SlowParser<?> getFieldTypeParser(Type type, boolean declaredNullable,
        boolean isSubtyping, FieldLoadStrategy loadStrategy)
        throws JsonProtocolModelParseException {
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
        } else if (type == Number.class) {
          return NUMBER_PARSER.get(declaredNullable);
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
          SlowParser<?> componentParser =
              getFieldTypeParser(argumentType, false, false, loadStrategy);
          return createArrayParser(componentParser, declaredNullable, loadStrategy);
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
        boolean isNullable, FieldLoadStrategy loadStrategy) {
      if (loadStrategy == FieldLoadStrategy.LAZY) {
        return new ArrayParser<T>(componentParser, isNullable, ArrayParser.LAZY);
      } else {
        return new ArrayParser<T>(componentParser, isNullable, ArrayParser.EAGER);
      }
    }

    private <T> RefToType<T> getTypeRef(final Class<T> typeClass) {
      if (type2typeHandler.containsKey(typeClass)) {
        RefImpl<T> result = new RefImpl<T>(typeClass);
        refs.add(result);
        return result;
      }
      for (DynamicParserImpl<?> baseParser : basePackages) {
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
      private final List<LazyHandler> onDemandHanlers = new ArrayList<LazyHandler>();
      private final Map<Method, MethodHandler> methodHandlerMap =
          new HashMap<Method, MethodHandler>();
      private final FieldMap fieldMap = new FieldMap();
      private final List<FieldCondition> fieldConditions = new ArrayList<FieldCondition>(2);
      private ManualAlgebraicCasesDataImpl manualAlgCasesData = null;
      private AutoAlgebraicCasesDataImpl autoAlgCasesData = null;
      private int fieldArraySize = 0;
      private List<VolatileFieldBinding> volatileFields = new ArrayList<VolatileFieldBinding>(2);
      private boolean requiresJsonObject = false;

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

          if (jsonTypeAnn.subtypesChosenManually()) {
            if (manualAlgCasesData == null) {
              manualAlgCasesData = new ManualAlgebraicCasesDataImpl();
            }
            methodHandler = processManualSubtypeMethod(m, jsonSubtypeCaseAnn);
          } else {
            if (autoAlgCasesData == null) {
              autoAlgCasesData = new AutoAlgebraicCasesDataImpl();
            }
            if (jsonSubtypeCaseAnn.reinterpret()) {
              throw new JsonProtocolModelParseException(
                  "Option 'reinterpret' is only available with 'subtypes chosen manually'");
            }
            requiresJsonObject = true;
            methodHandler = processAutomaticSubtypeMethod(m);
          }

        } else {
          requiresJsonObject = true;
          methodHandler = processFieldGetterMethod(m, fieldConditionLogic, overrideFieldAnn,
              fieldName);
        }
        methodHandlerMap.put(m, methodHandler);
      }

      private MethodHandler processFieldGetterMethod(Method m,
          FieldConditionLogic fieldConditionLogic, JsonOverrideField overrideFieldAnn,
          String fieldName) throws JsonProtocolModelParseException {
        MethodHandler methodHandler;

        FieldLoadStrategy loadStrategy;
        if (m.getAnnotation(JsonField.class) == null) {
          loadStrategy = FieldLoadStrategy.AUTO;
        } else {
          loadStrategy = m.getAnnotation(JsonField.class).loadStrategy();
        }

        JsonNullable nullableAnn = m.getAnnotation(JsonNullable.class);
        SlowParser<?> fieldTypeParser = getFieldTypeParser(m.getGenericReturnType(),
            nullableAnn != null, false, loadStrategy);
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
          QuickParser<?> quickParser = fieldTypeParser.asQuickParser();
          if (loadStrategy == FieldLoadStrategy.EAGER) {
            methodHandler = createEagerLoadGetterHandler(fieldName, fieldTypeParser, isOptional);
          } else {
            methodHandler = createLazyQuickGetterHandler(quickParser, isOptional, fieldName);
          }
        } else {
          if (loadStrategy == FieldLoadStrategy.LAZY) {
            methodHandler = createLazyCachedGetterHandler(fieldName, fieldTypeParser, isOptional);
          } else {
            methodHandler = createEagerLoadGetterHandler(fieldName, fieldTypeParser, isOptional);
          }
        }
        return methodHandler;
      }

      private MethodHandler createLazyQuickGetterHandler(QuickParser<?> quickParser,
          boolean isOptional, String fieldName) {
        LazyParseFieldMethodHandler onDemandHandler = new LazyParseFieldMethodHandler(quickParser,
            isOptional, fieldName, typeClass);
        onDemandHanlers.add(onDemandHandler);
        return onDemandHandler;
      }

      private MethodHandler createEagerLoadGetterHandler(String fieldName,
          SlowParser<?> fieldTypeParser, boolean isOptional) {
        int fieldCode = allocateFieldInArray();
        FieldLoader fieldLoader = new FieldLoader(fieldCode, fieldName, fieldTypeParser,
            isOptional);
        fieldLoaders.add(fieldLoader);
        return new PreparsedFieldMethodHandler(fieldCode,
            fieldTypeParser.getValueFinisher(), fieldName);
      }

      private MethodHandler createLazyCachedGetterHandler(String fieldName,
          SlowParser<?> fieldTypeParser, boolean isOptional) {
        VolatileFieldBinding fieldBinding = allocateVolatileField(fieldTypeParser, false);
        LazyCachedFieldMethodHandler lazyCachedHandler =
            new LazyCachedFieldMethodHandler(fieldBinding, fieldTypeParser, isOptional,
                fieldName, typeClass);
        onDemandHanlers.add(lazyCachedHandler);
        return lazyCachedHandler;
      }

      private MethodHandler processAutomaticSubtypeMethod(Method m)
          throws JsonProtocolModelParseException {
        MethodHandler methodHandler;
        if (m.getReturnType() == Void.TYPE) {
          if (autoAlgCasesData.hasDefaultCase) {
            throw new JsonProtocolModelParseException("Duplicate default case method: " + m);
          }
          autoAlgCasesData.hasDefaultCase = true;
          methodHandler = RETURN_NULL_METHOD_HANDLER;
        } else {
          Class<?> methodType = m.getReturnType();
          RefToType<?> ref = getTypeRef(methodType);
          if (ref == null) {
            throw new JsonProtocolModelParseException("Unknown return type in " + m);
          }
          if (autoAlgCasesData.variantCodeFieldPos == -1) {
            autoAlgCasesData.variantCodeFieldPos = allocateFieldInArray();
            autoAlgCasesData.variantValueFieldPos = allocateFieldInArray();
          }
          final int algCode = autoAlgCasesData.subtypes.size();
          autoAlgCasesData.subtypes.add(ref);
          final AutoSubtypeMethodHandler algMethodHandler = new AutoSubtypeMethodHandler(
              autoAlgCasesData.variantCodeFieldPos, autoAlgCasesData.variantValueFieldPos,
              algCode);
          methodHandler = algMethodHandler;

          SubtypeCaster subtypeCaster = new SubtypeCaster(typeClass, ref) {
            @Override
            ObjectData getSubtypeObjectData(ObjectData objectData) {
              return algMethodHandler.getFieldObjectData(objectData);
            }
            @Override
            void writeJava(ClassScope scope, String expectedTypeName, String superTypeValueRef,
                String resultRef) {
              scope.startLine(expectedTypeName + " " + resultRef + " = " + superTypeValueRef +
                  "." + AutoAlgebraicCasesDataImpl.getAutoAlgFieldNameJava(algCode) + ";\n");
            }
          };

          subtypeCasters.add(subtypeCaster);
        }
        return methodHandler;
      }


      private MethodHandler processManualSubtypeMethod(final Method m,
          JsonSubtypeCasting jsonSubtypeCaseAnn) throws JsonProtocolModelParseException {

        SlowParser<?> fieldTypeParser = getFieldTypeParser(m.getGenericReturnType(), false,
            !jsonSubtypeCaseAnn.reinterpret(), FieldLoadStrategy.AUTO);

        VolatileFieldBinding fieldInfo = allocateVolatileField(fieldTypeParser, true);

        if (!Arrays.asList(m.getExceptionTypes()).contains(JsonProtocolParseException.class)) {
          throw new JsonProtocolModelParseException(
              "Method should declare JsonProtocolParseException exception: " + m);
        }

        final ManualSubtypeMethodHandler handler = new ManualSubtypeMethodHandler(fieldInfo,
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
            @Override
            void writeJava(ClassScope scope, String expectedTypeName, String superTypeValueRef,
                String resultRef) {
              scope.startLine(expectedTypeName + " " + resultRef + " = " + superTypeValueRef +
                  "." + m.getName() + "();\n");
            }
          };
          manualAlgCasesData.subtypes.add(parserAsJsonTypeParser.getType());
          subtypeCasters.add(subtypeCaster);
        }

        return handler;
      }

      int getFieldArraySize() {
        return fieldArraySize;
      }

      List<VolatileFieldBinding> getVolatileFields() {
        return volatileFields;
      }

      TypeHandler.AlgebraicCasesData getAlgCasesData() {
        if (jsonTypeAnn.subtypesChosenManually()) {
          return manualAlgCasesData;
        } else {
          return autoAlgCasesData;
        }
      }

      List<FieldLoader> getFieldLoaders() {
        return fieldLoaders;
      }

      List<LazyHandler> getOnDemandHanlers() {
        return onDemandHanlers;
      }

      Map<Method, MethodHandler> getMethodHandlerMap() {
        return methodHandlerMap;
      }

      List<FieldCondition> getFieldConditions() {
        return fieldConditions;
      }

      boolean requiresJsonObject() {
        return requiresJsonObject;
      }

      private int allocateFieldInArray() {
        return fieldArraySize++;
      }

      private VolatileFieldBinding allocateVolatileField(final SlowParser<?> fieldTypeParser,
          boolean internalType) {
        int position = volatileFields.size();
        FieldTypeInfo fieldTypeInfo;
        if (internalType) {
          fieldTypeInfo = new FieldTypeInfo() {
            @Override public void appendValueTypeNameJava(FileScope scope) {
              fieldTypeParser.appendInternalValueTypeNameJava(scope);
            }
          };
        } else {
          fieldTypeInfo = new FieldTypeInfo() {
            @Override public void appendValueTypeNameJava(FileScope scope) {
              fieldTypeParser.appendFinishedValueTypeNameJava(scope);
            }
          };
        }
        VolatileFieldBinding binding = new VolatileFieldBinding(position, fieldTypeInfo);
        volatileFields.add(binding);
        return binding;
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
          String jsonLiteralName = fieldAnn.jsonLiteralName();
          if (!jsonLiteralName.isEmpty()) {
            return jsonLiteralName;
          }
        }
        return m.getName();
      }
    }
  }

  private static class EagerFieldParserImpl extends TypeHandler.EagerFieldParser {
    private final List<LazyHandler> onDemandHandlers;

    private EagerFieldParserImpl(List<LazyHandler> onDemandHandlers) {
      this.onDemandHandlers = onDemandHandlers;
    }

    @Override
    void parseAllFields(ObjectData objectData) throws JsonProtocolParseException {
      for (LazyHandler handler : onDemandHandlers) {
        handler.parseEager(objectData);
      }
    }
    @Override
    void addAllFieldNames(Set<? super String> output) {
      for (LazyHandler handler : onDemandHandlers) {
        output.add(handler.getFieldName());
      }
    }
  }

  private interface LazyHandler {
    void parseEager(ObjectData objectData) throws JsonProtocolParseException;
    String getFieldName();
  }

  private static class LazyParseFieldMethodHandler extends MethodHandler implements LazyHandler {
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
    Object handle(ObjectData objectData, Object[] args) {
      try {
        return parse(objectData);
      } catch (JsonProtocolParseException e) {
        throw new ParseRuntimeException(
            "On demand parsing failed for " + objectData.getUnderlyingObject(), e);
      }
    }

    @Override
    public void parseEager(ObjectData objectData) throws JsonProtocolParseException {
      parse(objectData);
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
          throw new JsonProtocolParseException("Failed to parse field '" + fieldName +
              "' in type " + typeClass.getName(), e);
        }
      } else {
        if (!isOptional) {
          throw new JsonProtocolParseException("Field is not optional: " + fieldName +
              " (in type " + typeClass.getName() + ")");
        }
        return null;
      }
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
      writeMethodDeclarationJava(classScope, m, Collections.<String>emptyList());
      classScope.startLine("{\n");

      MethodScope scope = classScope.newMethodScope();

      scope.indentRight();

      scope.startLine("");
      quickParser.appendFinishedValueTypeNameJava(scope);
      scope.append(" result;\n");

      boolean wrap = quickParser.javaCodeThrowsException() || !isOptional;
      if (wrap) {
        scope.startLine("try {\n");
        scope.indentRight();
      }

      String valueRef = scope.newMethodScopedName("value");
      String hasValueRef = scope.newMethodScopedName("hasValue");
      Util.writeReadValueAndHasValue(scope, fieldName, "underlying", valueRef, hasValueRef);
      scope.startLine("if (" + hasValueRef + ") {\n");
      scope.indentRight();
      if (quickParser.javaCodeThrowsException()) {
        scope.startLine("try {\n");
        scope.indentRight();
        quickParser.writeParseQuickCode(scope, valueRef, "r1");
        scope.startLine("result = r1;\n");
        scope.indentLeft();
        scope.startLine("} catch (" + Util.BASE_PACKAGE + ".JsonProtocolParseException e) {\n");
        scope.startLine("  throw new " + Util.BASE_PACKAGE + ".JsonProtocolParseException(" +
            "\"Failed to parse field " + fieldName + " in type ");
        scope.append(typeClass.getName() + "\", e);\n");
        scope.startLine("}\n");
      } else {
        quickParser.writeParseQuickCode(scope, valueRef, "r1");
        scope.startLine("result = r1;\n");
      }
      scope.indentLeft();
      scope.startLine("} else {\n");
      scope.indentRight();
      if (isOptional) {
        scope.startLine("result = null;\n");
      } else {
        scope.startLine("throw new " + Util.BASE_PACKAGE + ".JsonProtocolParseException(" +
            "\"Field is not optional: " + fieldName + "\");\n");
      }
      scope.indentLeft();
      scope.startLine("}\n");

      if (wrap) {
        scope.indentLeft();
        scope.startLine("} catch (" + Util.BASE_PACKAGE + ".JsonProtocolParseException e) {\n");
        scope.startLine("  throw new " + Util.BASE_PACKAGE +
            ".implutil.CommonImpl.ParseRuntimeException(" +
            "\"On demand parsing failed for \" + underlying, e);\n");
        scope.startLine("}\n");
      }
      scope.startLine("return result;\n");
      scope.indentLeft();
      scope.startLine("}\n");
    }
  }

  /**
   * Basic implementation of the method that parses value on demand and store it for
   * a future use.
   */
  private static abstract class LazyCachedMethodHandlerBase extends MethodHandler {
    private final VolatileFieldBinding fieldBinding;

    LazyCachedMethodHandlerBase(VolatileFieldBinding fieldBinding) {
      this.fieldBinding = fieldBinding;
    }

    @Override
    Object handle(ObjectData objectData, Object[] args) {
      try {
        return handle(objectData);
      } catch (JsonProtocolParseException e) {
        throw new ParseRuntimeException(
            "On demand parsing failed for " + objectData.getUnderlyingObject(), e);
      }
    }

    Object handle(ObjectData objectData) throws JsonProtocolParseException {
      Object raw = handleRaw(objectData);
      return finishRawValue(raw);
    }

    protected abstract Object finishRawValue(Object raw);

    Object handleRaw(ObjectData objectData) throws JsonProtocolParseException {
      AtomicReferenceArray<Object> atomicReferenceArray = objectData.getAtomicReferenceArray();

      Object cachedValue = fieldBinding.get(atomicReferenceArray);
      if (cachedValue != null) {
        return cachedValue;
      }

      Object parsedValue = parse(objectData);

      if (parsedValue != null) {
        parsedValue = fieldBinding.setAndGet(atomicReferenceArray, parsedValue);
      }
      return parsedValue;
    }

    protected abstract Object parse(ObjectData objectData) throws JsonProtocolParseException;

    protected VolatileFieldBinding getFieldBinding() {
      return fieldBinding;
    }

    protected abstract void writeReturnTypeJava(ClassScope scope, Method m);

    @Override
    void writeMethodImplementationJava(ClassScope classScope, Method m) {
      classScope.startLine("@Override public ");
      writeReturnTypeJava(classScope, m);
      classScope.append(" ");
      appendMethodSignatureJava(classScope, m, Collections.<String>emptyList());
      {
        Type[] exceptions = m.getGenericExceptionTypes();
        if (exceptions.length > 0) {
          classScope.append(" throws ");
          for (int i = 0; i < exceptions.length; i++) {
            if (i != 0) {
              classScope.append(", ");
            }
            Util.writeJavaTypeName(exceptions[i], classScope.getStringBuilder());
          }
        }
      }

      MethodScope scope = classScope.newMethodScope();
      scope.append(" {\n");
      scope.indentRight();

      classScope.startLine("");
      writeReturnTypeJava(classScope, m);
      scope.append(" result = ");
      getFieldBinding().writeGetExpressionJava(scope.getStringBuilder());
      scope.append(";\n");

      scope.startLine("if (result != null) {\n");
      scope.startLine("  return result;\n");
      scope.startLine("}\n");

      String parseResultRef = scope.newMethodScopedName("parseResult");
      writeParseJava(scope, parseResultRef);

      scope.startLine("if (" + parseResultRef + " != null) {\n");
      scope.indentRight();
      getFieldBinding().writeSetAndGetJava(scope, parseResultRef, "cachedResult");
      scope.startLine(parseResultRef + " = cachedResult;\n");
      scope.indentLeft();
      scope.startLine("}\n");

      scope.startLine("return " + parseResultRef + ";\n");

      scope.indentLeft();
      scope.startLine("}\n");
    }

    protected abstract void writeParseJava(MethodScope scope, String parseResultRef);
  }

  private static class LazyCachedFieldMethodHandler extends LazyCachedMethodHandlerBase
      implements LazyHandler {
    private final SlowParser<?> slowParser;
    private final boolean isOptional;
    private final String fieldName;
    private final Class<?> typeClass;

    LazyCachedFieldMethodHandler(VolatileFieldBinding fieldBinding, SlowParser<?> slowParser,
        boolean isOptional, String fieldName, Class<?> typeClass) {
      super(fieldBinding);
      this.slowParser = slowParser;
      this.isOptional = isOptional;
      this.fieldName = fieldName;
      this.typeClass = typeClass;
    }

    @Override
    public void parseEager(ObjectData objectData) throws JsonProtocolParseException {
      parse(objectData);
    }

    @Override
    protected Object parse(ObjectData objectData) throws JsonProtocolParseException {
      Map<?,?> properties = (JSONObject)objectData.getUnderlyingObject();
      Object value = properties.get(fieldName);
      boolean hasValue;
      if (value == null) {
        hasValue = properties.containsKey(fieldName);
      } else {
        hasValue = true;
      }
      Object parsedValue = parse(hasValue, value, objectData);
      // Cache already finished value, because we don't use unfinished value anywhere.
      FieldLoadedFinisher valueFinisher = slowParser.getValueFinisher();
      if (valueFinisher != null) {
        parsedValue = valueFinisher.getValueForUser(parsedValue);
      }
      return parsedValue;
    }

    @Override
    protected Object finishRawValue(Object raw) {
      return raw;
    }

    private Object parse(boolean hasValue, Object value, ObjectData objectData)
        throws JsonProtocolParseException {
      if (hasValue) {
        try {
          return slowParser.parseValue(value, objectData);
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


    @Override
    protected void writeReturnTypeJava(ClassScope scope, Method m) {
      getFieldBinding().getTypeInfo().appendValueTypeNameJava(scope);
    }

    @Override
    protected void writeParseJava(MethodScope scope, String parseResultRef) {
      scope.startLine("");
      getFieldBinding().getTypeInfo().appendValueTypeNameJava(scope);
      scope.append(" " + parseResultRef + ";\n");

      boolean wrap = slowParser.javaCodeThrowsException() || !isOptional;
      if (wrap) {
        scope.startLine("try {\n");
        scope.indentRight();
      }

      String valueRef = scope.newMethodScopedName("value");
      String hasValueRef = scope.newMethodScopedName("hasValue");
      Util.writeReadValueAndHasValue(scope, fieldName, "underlying", valueRef, hasValueRef);

      scope.startLine("if (" + hasValueRef + ") {\n");
      scope.indentRight();
      if (slowParser.javaCodeThrowsException()) {
        scope.startLine("try {\n");
        scope.indentRight();
        slowParser.writeParseCode(scope, valueRef, "null", "r1");
        scope.startLine(parseResultRef + " = r1;\n");
        scope.indentLeft();
        scope.startLine("} catch (" + Util.BASE_PACKAGE + ".JsonProtocolParseException e) {\n");
        scope.startLine("  throw new " + Util.BASE_PACKAGE + ".JsonProtocolParseException(" +
            "\"Failed to parse field " + fieldName + " in type ");
        scope.append(typeClass.getName() + "\", e);\n");
        scope.startLine("}\n");
      } else {
        slowParser.writeParseCode(scope, valueRef, "null", "r1");
        scope.startLine(parseResultRef + " = r1;\n");
      }
      scope.indentLeft();
      scope.startLine("} else {\n");
      scope.indentRight();
      if (isOptional) {
        scope.startLine(parseResultRef + " = null;\n");
      } else {
        scope.startLine("throw new " + Util.BASE_PACKAGE + ".JsonProtocolParseException(" +
            "\"Field is not optional: " + fieldName + "\");\n");
      }
      scope.indentLeft();
      scope.startLine("}\n");

      if (wrap) {
        scope.indentLeft();
        scope.startLine("} catch (" + Util.BASE_PACKAGE + ".JsonProtocolParseException e) {\n");
        scope.startLine("  throw new " + Util.BASE_PACKAGE +
            ".implutil.CommonImpl.ParseRuntimeException(" +
            "\"On demand parsing failed for \" + underlying, e);\n");
        scope.startLine("}\n");
      }
    }

    @Override
    public String getFieldName() {
      return fieldName;
    }
  }

  private static class PreparsedFieldMethodHandler extends MethodHandler {
    private final int pos;
    private final FieldLoadedFinisher valueFinisher;
    private final String fieldName;

    PreparsedFieldMethodHandler(int pos, FieldLoadedFinisher valueFinisher, String fieldName) {
      this.pos = pos;
      this.valueFinisher = valueFinisher;
      this.fieldName = fieldName;
    }

    @Override
    Object handle(ObjectData objectData, Object[] args) throws Throwable {
      Object val = objectData.getFieldArray()[pos];
      if (valueFinisher != null) {
        val = valueFinisher.getValueForUser(val);
      }
      return val;
    }

    @Override
    void writeMethodImplementationJava(ClassScope scope, Method m) {
      writeMethodDeclarationJava(scope, m, Collections.<String>emptyList());
      scope.append(" {\n");
      scope.startLine("  return field_" + fieldName + ";\n");
      scope.startLine("}\n");
    }
  }

  static SlowParser<Void> VOID_PARSER = new QuickParser<Void>() {
    @Override
    public Void parseValueQuick(Object value) {
      return null;
    }

    @Override
    public void appendFinishedValueTypeNameJava(FileScope scope) {
      scope.append("Void");
    }

    @Override
    void writeParseQuickCode(MethodScope scope, String valueRef, String resultRef) {
      scope.startLine("Void " + resultRef + " = null;\n");
    }

    @Override
    boolean javaCodeThrowsException() {
      return false;
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

    @Override
    public void appendFinishedValueTypeNameJava(FileScope scope) {
      scope.append(fieldType.getCanonicalName());
    }

    @Override
    public void writeParseQuickCode(MethodScope scope, String valueRef,
        String resultRef) {
      scope.startLine(fieldType.getCanonicalName() + " " + resultRef + " = (" +
          fieldType.getCanonicalName() + ") " + valueRef + ";\n");
    }

    @Override
    boolean javaCodeThrowsException() {
      return false;
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

  private static final SimpleParserPair<Long> LONG_PARSER =
      SimpleParserPair.create(Long.class);
  private static final SimpleParserPair<Boolean> BOOLEAN_PARSER =
      SimpleParserPair.create(Boolean.class);
  private static final SimpleParserPair<Float> FLOAT_PARSER =
      SimpleParserPair.create(Float.class);
  private static final SimpleParserPair<Number> NUMBER_PARSER =
      SimpleParserPair.create(Number.class);
  private static final SimpleParserPair<String> STRING_PARSER =
      SimpleParserPair.create(String.class);
  private static final SimpleParserPair<Object> OBJECT_PARSER =
      SimpleParserPair.create(Object.class);
  private static final SimpleParserPair<JSONObject> JSON_PARSER =
      SimpleParserPair.create(JSONObject.class);

  static class ArrayParser<T> extends SlowParser<List<? extends T>> {

    static abstract class ListFactory {
      abstract <T> List<T> create(JSONArray array, SlowParser<T> componentParser)
          throws JsonProtocolParseException;

      abstract void writeCreateListCode(SlowParser<?> componentParser, MethodScope scope,
          String inputRef, String resultRef);
    }

    static final ListFactory EAGER = new ListFactory() {
      @Override
      <T> List<T> create(JSONArray array, SlowParser<T> componentParser)
          throws JsonProtocolParseException {
        int size = array.size();
        List list = new ArrayList<Object>(size);
        FieldLoadedFinisher valueFinisher = componentParser.getValueFinisher();
        for (int i = 0; i < size; i++) {
          // We do not support super object for array component.
          Object val = componentParser.parseValue(array.get(i), null);
          if (valueFinisher != null) {
            val = valueFinisher.getValueForUser(val);
          }
          list.add(val);
        }
        return Collections.unmodifiableList(list);
      }

      @Override
      void writeCreateListCode(SlowParser<?> componentParser, MethodScope scope, String inputRef,
          String resultRef) {
        String sizeRef = scope.newMethodScopedName("size");
        String listRef = scope.newMethodScopedName("list");
        String indexRef = scope.newMethodScopedName("index");
        String componentRef = scope.newMethodScopedName("arrayComponent");
        scope.startLine("int " + sizeRef + " = " + inputRef + ".size();\n");
        scope.startLine("java.util.List<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append("> " + listRef + " = new java.util.ArrayList<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append(">(" + sizeRef + ");\n");
        scope.startLine("for (int " + indexRef + " = 0; " + indexRef + " < " + sizeRef + "; " +
            indexRef + "++) {\n");
        scope.indentRight();
        componentParser.writeParseCode(scope, inputRef + ".get(" + indexRef + ")", "null",
            componentRef);
        scope.startLine(listRef + ".add(" + componentRef + ");\n");
        scope.indentLeft();
        scope.startLine("}\n");
        scope.startLine("java.util.List<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append("> " + resultRef + " = java.util.Collections.unmodifiableList(" +
            listRef + ");\n");
      }
    };

    static final ListFactory LAZY = new ListFactory() {
      @Override
      <T> List<T> create(final JSONArray array, final SlowParser<T> componentParser) {
        final int size = array.size();
        List<T> list = new AbstractList<T>() {
          private final AtomicReferenceArray<T> values = new AtomicReferenceArray<T>(size);

          @Override
          public synchronized T get(int index) {
            T parsedValue = values.get(index);
            if (parsedValue == null) {
              Object rawObject = array.get(index);
              if (rawObject != null) {
                Object parsedObject;
                try {
                  parsedObject = componentParser.parseValue(array.get(index), null);
                } catch (JsonProtocolParseException e) {
                  throw new ParseRuntimeException(e);
                }
                FieldLoadedFinisher valueFinisher = componentParser.getValueFinisher();
                if (valueFinisher != null) {
                  parsedObject = valueFinisher.getValueForUser(parsedObject);
                }
                parsedValue = (T) parsedObject;
                values.compareAndSet(index, null, parsedValue);
                parsedValue = values.get(index);
              }
            }
            return parsedValue;
          }

          @Override
          public int size() {
            return size;
          }
        };
        return list;
      }

      @Override
      void writeCreateListCode(SlowParser<?> componentParser, MethodScope scope, String inputRef,
          String resultRef) {
        String sizeRef = scope.newMethodScopedName("size");
        scope.startLine("final int " + sizeRef + " = " + inputRef + ".size();\n");
        scope.startLine("java.util.List<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append("> " + resultRef + " = new java.util.AbstractList<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append(">() {\n");
        scope.indentRight();
        scope.startLine("private final java.util.concurrent.atomic.AtomicReferenceArray<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append("> cachedValues = new java.util.concurrent.atomic.AtomicReferenceArray<");
        componentParser.appendFinishedValueTypeNameJava(scope);
        scope.append(">(" + sizeRef + ");\n");
        scope.append("\n");
        scope.startLine("@Override public int size() { return " + sizeRef + "; }\n");
        scope.append("\n");
        writeGetMethodCode(componentParser, scope, inputRef);
        scope.indentLeft();
        scope.startLine("};\n");
      }

      private void writeGetMethodCode(SlowParser<?> componentParser, MethodScope outerMethodScope,
          String arrayRef) {
        outerMethodScope.startLine("@Override public ");
        componentParser.appendFinishedValueTypeNameJava(outerMethodScope);
        outerMethodScope.append(" get(int index) {\n");
        {
          MethodScope scope = outerMethodScope.newMethodScope();
          scope.indentRight();

          String resultRef = scope.newMethodScopedName("result");

          scope.startLine("");
          componentParser.appendFinishedValueTypeNameJava(scope);
          scope.append(" " + resultRef + " = cachedValues.get(index);\n");
          scope.startLine("if (" + resultRef + " == null) {\n");
          scope.indentRight();

          boolean wrap = componentParser.javaCodeThrowsException();
          if (wrap) {
            scope.startLine("try {\n");
            scope.indentRight();
          }

          scope.startLine("Object unparsed = " + arrayRef + ".get(index);\n");
          componentParser.writeParseCode(scope, "unparsed", "null", "parsed");
          scope.startLine(resultRef + " = parsed;\n");

          if (wrap) {
            scope.indentLeft();
            scope.startLine("} catch (" + Util.BASE_PACKAGE +
                ".JsonProtocolParseException e) {\n");
            scope.startLine("  throw new " + Util.BASE_PACKAGE +
                ".implutil.CommonImpl.ParseRuntimeException(e);\n");
            scope.startLine("}\n");
          }
          scope.startLine("cachedValues.compareAndSet(index, null, " + resultRef + ");\n");
          scope.startLine(resultRef + " = cachedValues.get(index);\n");
          scope.indentLeft();
          scope.startLine("}\n");
          scope.startLine("return " + resultRef + ";\n");
          scope.indentLeft();
        }
        outerMethodScope.startLine("}\n");
      }
    };

    private final SlowParser<T> componentParser;
    private final boolean isNullable;
    private final ListFactory listFactory;

    ArrayParser(SlowParser<T> componentParser, boolean isNullable, ListFactory listFactory) {
      this.componentParser = componentParser;
      this.isNullable = isNullable;
      this.listFactory = listFactory;
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
      return listFactory.create(arrayValue, componentParser);
    }

    @Override
    public FieldLoadedFinisher getValueFinisher() {
      return null;
    }

    @Override
    public JsonTypeParser<?> asJsonTypeParser() {
      return null;
    }

    @Override
    public void appendFinishedValueTypeNameJava(FileScope scope) {
      scope.append("java.util.List<");
      componentParser.appendFinishedValueTypeNameJava(scope);
      scope.append(">");
    }

    @Override
    public void appendInternalValueTypeNameJava(FileScope scope) {
      appendFinishedValueTypeNameJava(scope);
    }

    @Override
    void writeParseCode(MethodScope scope, String valueRef,
        String superValueRef, String resultRef) {
      if (isNullable) {
        scope.startLine("if (" + valueRef +  " == null) {\n");
        scope.startLine("  return null;\n");
        scope.startLine("}\n");
      }
      scope.startLine("if (" + valueRef +  " instanceof org.json.simple.JSONArray == false) {\n");
      scope.startLine("  throw new " + Util.BASE_PACKAGE +
          ".JsonProtocolParseException(\"Array value expected\");\n");
      scope.startLine("}\n");

      String arrayValueRef = scope.newMethodScopedName("arrayValue");

      scope.startLine("final org.json.simple.JSONArray " + arrayValueRef +
          " = (org.json.simple.JSONArray) " + valueRef +  ";\n");
      listFactory.writeCreateListCode(componentParser, scope, arrayValueRef, resultRef);
    }

    @Override
    boolean javaCodeThrowsException() {
      return true;
    }
  }

  static MethodHandler RETURN_NULL_METHOD_HANDLER = new MethodHandler() {
    @Override
    Object handle(ObjectData objectData, Object[] args) throws Throwable {
      return null;
    }

    @Override
    void writeMethodImplementationJava(ClassScope scope, Method m) {
      writeMethodDeclarationJava(scope, m, Collections.<String>emptyList());
      scope.append(" {\n");
      scope.startLine("}\n");
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
    Object handle(ObjectData objectData, Object[] args) {
      ObjectData resData = getFieldObjectData(objectData);
      if (resData == null) {
        return null;
      } else {
        return resData.getProxy();
      }
    }

    @Override
    void writeMethodImplementationJava(ClassScope scope, Method m) {
      writeMethodDeclarationJava(scope, m, Collections.<String>emptyList());
      scope.append(" {\n");
      scope.startLine("  return " + AutoAlgebraicCasesDataImpl.getAutoAlgFieldNameJava(code) +
          ";\n");
      scope.startLine("}\n");
    }
  }

  static class ManualSubtypeMethodHandler extends LazyCachedMethodHandlerBase {
    private final SlowParser<?> parser;

    ManualSubtypeMethodHandler(VolatileFieldBinding fieldInf, SlowParser<?> parser) {
      super(fieldInf);
      this.parser = parser;
    }

    @Override
    protected Object parse(ObjectData objectData) throws JsonProtocolParseException {
      return parser.parseValue(objectData.getUnderlyingObject(), objectData);
    }

    @Override
    protected Object finishRawValue(Object raw) {
      FieldLoadedFinisher valueFinisher = parser.getValueFinisher();
      Object res = raw;
      if (valueFinisher != null) {
         res = valueFinisher.getValueForUser(res);
      }
      return res;
    }

    ObjectData getSubtypeData(ObjectData objectData) throws JsonProtocolParseException {
      return (ObjectData) handleRaw(objectData);
    }

    @Override
    protected void writeReturnTypeJava(ClassScope scope, Method m) {
      JsonTypeParser<?> jsonTypeParser = parser.asJsonTypeParser();
      if (jsonTypeParser == null) {
        JavaCodeGenerator.Util.writeJavaTypeName(m.getGenericReturnType(),
            scope.getStringBuilder());
      } else {
        String valueTypeName = scope.getTypeImplReference(jsonTypeParser.getType().get());
        scope.append(valueTypeName);
      }
    }

    @Override
    protected void writeParseJava(MethodScope scope, String parseResultRef) {
      parser.writeParseCode(scope, "underlying", "this", parseResultRef);
    }
  }

  static class AutoAlgebraicCasesDataImpl extends TypeHandler.AlgebraicCasesData {
    private int variantCodeFieldPos = -1;
    private int variantValueFieldPos = -1;
    private boolean hasDefaultCase = false;
    private final List<RefToType<?>> subtypes = new ArrayList<RefToType<?>>();


    @Override
    List<RefToType<?>> getSubtypes() {
      return subtypes;
    }

    @Override
    void parseObjectSubtype(ObjectData objectData, Map<?, ?> jsonProperties,
        Object input) throws JsonProtocolParseException {
      if (jsonProperties == null) {
        throw new JsonProtocolParseException(
            "JSON object input expected for non-manual subtyping");
      }
      int code = -1;
      for (int i = 0; i < this.getSubtypes().size(); i++) {
        TypeHandler<?> nextSubtype = this.getSubtypes().get(i).get();
        boolean ok = nextSubtype.getSubtypeSupport().checkConditions(jsonProperties);
        if (ok) {
          if (code == -1) {
            code = i;
          } else {
            throw new JsonProtocolParseException("More than one case match");
          }
        }
      }
      if (code == -1) {
        if (!this.hasDefaultCase) {
          throw new JsonProtocolParseException("Not a singe case matches");
        }
      } else {
        ObjectData fieldData =
            this.getSubtypes().get(code).get().parse(input, objectData);
        objectData.getFieldArray()[this.variantValueFieldPos] = fieldData;
      }
      objectData.getFieldArray()[this.variantCodeFieldPos] =
          Integer.valueOf(code);
    }

    @Override
    void writeConstructorCodeJava(MethodScope methodScope) {
      methodScope.startLine("int code = -1;\n");
      for (int i = 0; i < getSubtypes().size(); i++) {
        TypeHandler<?> nextSubtype = getSubtypes().get(i).get();
        methodScope.startLine("if (" + methodScope.getTypeImplReference(nextSubtype) +
            ".checkSubtypeConditions(underlying)) {\n");
        methodScope.startLine("  if (code == -1) {\n");
        methodScope.startLine("    code = " + i + ";\n");
        methodScope.startLine("  } else {\n");
        methodScope.startLine("    throw new " + Util.BASE_PACKAGE +
            ".JsonProtocolParseException(\"More than one case match\");\n");
        methodScope.startLine("  }\n");
        methodScope.startLine("}\n");
      }
      if (!hasDefaultCase) {
        methodScope.startLine("if (code == -1) {\n");
        methodScope.startLine("  throw new " + Util.BASE_PACKAGE +
            ".JsonProtocolParseException(\"Not a singe case matches\");\n");
        methodScope.startLine("}\n");
      }
      for (int i = 0; i < getSubtypes().size(); i++) {
        TypeHandler<?> nextSubtype = getSubtypes().get(i).get();
        methodScope.startLine(getAutoAlgFieldNameJava(i) + " = (code == " + i + ") ? new " +
            methodScope.getTypeImplReference(nextSubtype) + "(underlying, this) : null;\n");
      }
    }

    @Override
    void writeFiledsJava(ClassScope classScope) {
      for (int i = 0; i < getSubtypes().size(); i++) {
        TypeHandler<?> nextSubtype = getSubtypes().get(i).get();
        classScope.startLine("private final " + classScope.getTypeImplReference(nextSubtype) +
            " " + getAutoAlgFieldNameJava(i) + ";\n");
      }
    }

    private static String getAutoAlgFieldNameJava(int code) {
      return "auto_alg_field_" + code;
    }
  }


  static class ManualAlgebraicCasesDataImpl extends TypeHandler.AlgebraicCasesData {
    private final List<RefToType<?>> subtypes = new ArrayList<RefToType<?>>();

    @Override
    List<RefToType<?>> getSubtypes() {
      return subtypes;
    }

    @Override
    void parseObjectSubtype(ObjectData objectData, Map<?, ?> jsonProperties, Object input) {
    }

    @Override
    void writeConstructorCodeJava(MethodScope methodScope) {
    }

    @Override
    void writeFiledsJava(ClassScope classScope) {
    }
  }

  static class VolatileFieldBinding {
    private final int position;
    private final FieldTypeInfo fieldTypeInfo;

    public VolatileFieldBinding(int position, FieldTypeInfo fieldTypeInfo) {
      this.position = position;
      this.fieldTypeInfo = fieldTypeInfo;
    }

    public Object setAndGet(AtomicReferenceArray<Object> atomicReferenceArray,
        Object value) {
      atomicReferenceArray.compareAndSet(position, null, value);
      return atomicReferenceArray.get(position);
    }

    public Object get(AtomicReferenceArray<Object> atomicReferenceArray) {
      return atomicReferenceArray.get(position);
    }

    void writeGetExpressionJava(StringBuilder output) {
      output.append(getCodeFieldName() + ".get()");
    }

    void writeSetAndGetJava(MethodScope scope,
        String valueRef, String resultRef) {
      scope.startLine(getCodeFieldName() + ".compareAndSet(null, " +  valueRef + ");\n");
      scope.startLine("");
      fieldTypeInfo.appendValueTypeNameJava(scope);
      scope.append(" " + resultRef + " = ");
      writeGetExpressionJava(scope.getStringBuilder());
      scope.append(";\n");
    }


    void writeFieldDeclarationJava(ClassScope scope) {
      scope.startLine("private final java.util.concurrent.atomic.AtomicReference<");
      fieldTypeInfo.appendValueTypeNameJava(scope);
      scope.append("  > " + getCodeFieldName() +
          " = new java.util.concurrent.atomic.AtomicReference<");
      fieldTypeInfo.appendValueTypeNameJava(scope);
      scope.append(">(null);\n");
    }

    FieldTypeInfo getTypeInfo() {
      return fieldTypeInfo;
    }

    private String getCodeFieldName() {
      return FIELD_NAME_PREFIX + position;
    }

    private static final String FIELD_NAME_PREFIX = "lazyCachedField_";
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

  public GeneratedCodeMap generateStaticParser(StringBuilder stringBuilder,
      String packageName, String className) {
    return generateStaticParser(stringBuilder, packageName, className,
        Collections.<GeneratedCodeMap>emptyList());
  }

  public GeneratedCodeMap generateStaticParser(StringBuilder stringBuilder, String packageName,
      String className, Collection<GeneratedCodeMap> basePackages) {
    JavaCodeGenerator generator = new JavaCodeGenerator.Impl();

    GlobalScope globalScope = generator.newGlobalScope(type2TypeHandler.values(), basePackages);

    FileScope fileScope = globalScope.newFileScope(stringBuilder);

    fileScope.startLine("// This is a generated source.\n");
    fileScope.startLine("// See " + this.getClass().getName() + " for details\n");
    fileScope.append("\n");
    fileScope.startLine("package " + packageName + ";\n");
    fileScope.append("\n");
    fileScope.startLine("public class " + className + " implements " +
        rootImpl.getType().getCanonicalName() + " {\n");

    ClassScope rootClassScope = fileScope.newClassScope();
    rootClassScope.indentRight();

    rootImpl.writeStaticMethodJava(rootClassScope);

    for (TypeHandler<?> typeHandler : type2TypeHandler.values()) {
      typeHandler.writeStaticClassJava(rootClassScope);
    }

    rootClassScope.writeClassMembers();

    rootClassScope.indentLeft();

    rootClassScope.startLine("}\n");

    Map<Class<?>, String> type2ImplClassName = new HashMap<Class<?>, String>();
    for (TypeHandler<?> typeHandler : type2TypeHandler.values()) {
      String shortName = fileScope.getTypeImplShortName(typeHandler);
      String fullReference = packageName + "." + className + "." + shortName;
      type2ImplClassName.put(typeHandler.getTypeClass(), fullReference);
    }

    return new GeneratedCodeMap(type2ImplClassName);
  }
}
