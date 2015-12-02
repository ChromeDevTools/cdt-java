// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A set of interfaces and classes used to generate Java code of parser implementation.
 */
public interface JavaCodeGenerator {

  GlobalScope newGlobalScope(Collection<TypeHandler<?>> typeHandlers,
      Collection<GeneratedCodeMap> basePackages);

  interface GlobalScope {
    String getTypeImplReference(TypeHandler<?> typeHandler);
    String getTypeImplShortName(TypeHandler<?> typeHandler);

    /**
     * @return new {@link FileScope} that extends {@link GlobalScope} and shares the state
     *     with this {@link GlobalScope}
     */
    FileScope newFileScope(StringBuilder output);
  }

  interface FileScope extends GlobalScope {
    StringBuilder getStringBuilder();

    void startLine(String line);
    void append(String line);

    void indentRight();
    void indentLeft();

    /**
     * @return new {@link ClassScope} that extends {@link FileScope} and shares the state
     *     with this {@link FileScope}
     */
    ClassScope newClassScope();
  }

  interface ClassScope extends FileScope {
    ClassScope getRootClassScope();

    /**
     * @return new {@link ClassScope} that has different state as {@link ClassScope},
     *     but shares the state with this as {@link FileScope}
     */
    @Override
    ClassScope newClassScope();

    /**
     * Adds a member to the class. The member is identified by the key. Member Java code
     * is generated later. If the member with a particular key
     * has already been added, method return data instance if returned the previous time.
     *
     * @return user-defined field element data
     */
    <T extends ElementData> T addMember(Object key, ElementFactory<T> factory);

    /**
     * @return new {@link MethodScope} that extends {@link ClassScope} and shares the state
     *     with this {@link ClassScope}.
     */
    MethodScope newMethodScope();

    /**
     * Writes Java code of all added members.
     */
    void writeClassMembers();
  }

  interface MethodScope extends ClassScope {
    /**
     * @return a name unique to this scope with the provided prefix
     */
    String newMethodScopedName(String prefix);
  }

  interface ElementData {
    void generateCode(ClassScope classScope);
  }

  interface ElementFactory<T extends ElementData> {
    T create(int code);
  }

  class Util {
    /**
     * Generate Java type name of the passed type. Type may be parameterized.
     */
    public static void writeJavaTypeName(Type arg, StringBuilder output) {
      if (arg instanceof Class) {
        Class<?> clazz = (Class<?>) arg;
        output.append(clazz.getCanonicalName());
      } else if (arg instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) arg;
        writeJavaTypeName(parameterizedType.getRawType(), output);
        output.append("<");
        Type[] params = parameterizedType.getActualTypeArguments();
        for (int i = 0; i < params.length; i++) {
          if (i != 0) {
            output.append(", ");
          }
          writeJavaTypeName(params[i], output);
        }
        output.append(">");
      } else if (arg instanceof WildcardType) {
        WildcardType wildcardType = (WildcardType) arg;
        Type[] upperBounds = wildcardType.getUpperBounds();
        if (upperBounds == null) {
          throw new RuntimeException();
        }
        if (upperBounds.length != 1) {
          throw new RuntimeException();
        }
        output.append("? extends ");
        writeJavaTypeName(upperBounds[0], output);
      } else {
        output.append(arg);
      }
    }

    /**
     * Generates a commonly-used code that gets property from JSON in form of
     * 'value' and 'hasValue' pair of variables.
     */
    public static void writeReadValueAndHasValue(MethodScope scope, String fieldName,
        String jsonObjectRef, String valueRef, String hasValueRef) {
      scope.startLine("Object " + valueRef + " = " + jsonObjectRef + ".get(\"" +
          fieldName + "\");\n");
      scope.startLine("boolean " + hasValueRef + ";\n");
      scope.startLine("if (" + valueRef + " == null) {\n");
      scope.startLine("  " + hasValueRef + " = " + jsonObjectRef + ".containsKey(\"" +
          fieldName + "\");\n");
      scope.startLine("} else {\n");
      scope.startLine("  " + hasValueRef + " = true;\n");
      scope.startLine("}\n");
    }

    public static final String BASE_PACKAGE = "org.chromium.sdk.internal.protocolparser";
    public static final String THROWS_CLAUSE =
        " throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException";
  }


  class Impl implements JavaCodeGenerator {

    @Override
    public GlobalScope newGlobalScope(Collection<TypeHandler<?>> typeHandlers,
        Collection<GeneratedCodeMap> basePackages) {
      return new GlobalScopeImpl(typeHandlers, basePackages);
    }

    private static class GlobalScopeImpl implements GlobalScope {
      private final State state;

      GlobalScopeImpl(Collection<TypeHandler<?>> typeHandlers,
          Collection<GeneratedCodeMap> basePackages) {
        state = new State(typeHandlers, basePackages);
      }

      GlobalScopeImpl(GlobalScopeImpl globalScopeImpl) {
        state = globalScopeImpl.state;
      }

      @Override
      public String getTypeImplReference(TypeHandler<?> typeHandler) {
        return state.getTypeImplReference(typeHandler);
      }

      @Override
      public String getTypeImplShortName(TypeHandler<?> typeHandler) {
        return state.getTypeImplShortName(typeHandler);
      }

      @Override
      public FileScope newFileScope(StringBuilder output) {
        return new FileScopeImpl(this, output);
      }

      private static class State {
        private final Map<TypeHandler<?>, String> type2Name;
        private final Collection<GeneratedCodeMap> basePackages;

        State(Collection<TypeHandler<?>> typeHandlers, Collection<GeneratedCodeMap> basePackages) {
          this.basePackages = basePackages;
          type2Name = buildLocalTypeNameMap(typeHandlers);
        }

        String getTypeImplReference(TypeHandler<?> typeHandler) {
          String localName = type2Name.get(typeHandler);
          if (localName == null) {
            for (GeneratedCodeMap base : basePackages) {
              String result = base.getTypeImplementationReference(typeHandler.getTypeClass());
              if (result != null) {
                return result;
              }
            }
          } else {
            return localName;
          }
          throw new RuntimeException();
        }

        String getTypeImplShortName(TypeHandler<?> typeHandler) {
          String result = type2Name.get(typeHandler);
          if (result == null) {
            throw new RuntimeException();
          }
          return result;
        }

        private static Map<TypeHandler<?>, String> buildLocalTypeNameMap(
            Collection<TypeHandler<?>> typeHandlers) {
          List<TypeHandler<?>> list = new ArrayList<TypeHandler<?>>(typeHandlers);

          // Sort to produce consistent GeneratedCodeMap later.
          Collections.sort(list, new Comparator<TypeHandler<?>>() {
            @Override
            public int compare(TypeHandler<?> o1, TypeHandler<?> o2) {
              return getName(o1).compareTo(getName(o2));
            }

            private String getName(TypeHandler<?> handler) {
              return handler.getTypeClass().getName();
            }
          });

          int uniqueCode = 0;
          Map<TypeHandler<?>, String> result = new HashMap<TypeHandler<?>, String>();

          for (TypeHandler<?> handler : list) {
            String name = "Value_" + uniqueCode++;
            Object conflict = result.put(handler, name);
            if (conflict != null) {
              throw new RuntimeException();
            }
          }
          return result;
        }
      }
    }

    private static class FileScopeImpl extends GlobalScopeImpl implements FileScope {
      private final State state;

      FileScopeImpl(GlobalScopeImpl globalScopeImpl, StringBuilder stringBuilder) {
        super(globalScopeImpl);
        this.state = new State(stringBuilder);
      }

      FileScopeImpl(FileScopeImpl fileScopeImpl) {
        super(fileScopeImpl);
        this.state = fileScopeImpl.state;
      }

      @Override
      public StringBuilder getStringBuilder() {
        return state.getStringBuilder();
      }

      @Override
      public void startLine(String line) {
        state.startLine(line);
      }

      @Override
      public void append(String line) {
        state.append(line);
      }

      @Override
      public void indentRight() {
        state.indentRight();
      }

      @Override
      public void indentLeft() {
        state.indentLeft();
      }

      @Override
      public ClassScope newClassScope() {
        return new ClassScopeImpl(this, asClassScopeImpl());
      }

      protected ClassScopeImpl asClassScopeImpl() {
        return null;
      }

      private static class State {
        private final StringBuilder stringBuilder;
        private int indent = 0;

        State(StringBuilder stringBuilder) {
          this.stringBuilder = stringBuilder;
        }

        StringBuilder getStringBuilder() {
          return stringBuilder;
        }

        void startLine(String line) {
          for (int i = 0; i < indent; i++) {
            stringBuilder.append(' ');
          }
          stringBuilder.append(line);
        }

        void append(String line) {
          stringBuilder.append(line);
        }

        void indentRight() {
          indent += 2;
        }

        void indentLeft() {
          indent -= 2;
        }
      }
    }

    private static class ClassScopeImpl extends FileScopeImpl implements ClassScope {
      private final State state;
      private final ClassScope parentClass;

      ClassScopeImpl(FileScopeImpl fileScopeImpl, ClassScope parentClass) {
        super(fileScopeImpl);
        this.state = new State();
        this.parentClass = parentClass;
      }

      ClassScopeImpl(ClassScopeImpl classScopeImpl) {
        super(classScopeImpl);
        this.state = classScopeImpl.state;
        this.parentClass = classScopeImpl.parentClass;
      }

      @Override
      public ClassScope getRootClassScope() {
        if (parentClass == null) {
          return this;
        } else {
          return parentClass.getRootClassScope();
        }
      }

      @Override
      public <T extends ElementData> T addMember(Object key,
          ElementFactory<T> factory) {
        return state.addMember(key, factory);
      }

      @Override
      public void writeClassMembers() {
        state.writeClassElements(this);
      }

      @Override
      public MethodScope newMethodScope() {
        return new MethodScopeImpl(this);
      }

      @Override
      protected ClassScopeImpl asClassScopeImpl() {
        return this;
      }

      private static class State {
        private final Map<Object, ElementData> key2ElementData =
            new HashMap<Object, JavaCodeGenerator.ElementData>(2);
        private int nextCode = 0;

        <T extends ElementData> T addMember(Object key, ElementFactory<T> factory) {
          List<Object> extendedKey = Arrays.asList(key, factory);
          ElementData rawData = key2ElementData.get(extendedKey);
          T data = (T) rawData;
          if (data == null) {
            data = factory.create(nextCode++);
            key2ElementData.put(extendedKey, data);
          }
          return data;
        }

        void writeClassElements(ClassScope classScope) {
          for (ElementData data : key2ElementData.values()) {
            data.generateCode(classScope);
          }
          key2ElementData.clear();
        }
      }
    }

    private static class MethodScopeImpl extends ClassScopeImpl implements MethodScope {
      private final State state;

      public MethodScopeImpl(ClassScopeImpl classScopeImpl) {
        super(classScopeImpl);
        state = new State();
      }

      @Override
      public String newMethodScopedName(String prefix) {
        return state.newMethodScopedName(prefix);
      }

      private static class State {
        private int nextId = 0;

        String newMethodScopedName(String prefix) {
          return prefix + nextId++;
        }
      }
    }
  }
}
