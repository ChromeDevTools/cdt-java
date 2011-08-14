// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.ArrayItemType;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.Command;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.Domain;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.Event;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.ObjectProperty;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.Parameter;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.StandaloneType;

/**
 * Read metamodel and generates set of files with Java classes/interfaces for the protocol.
 */
class Generator {
  private static final String ROOT_PACKAGE = "org.chromium.sdk.internal.wip.protocol";
  private static final String OUTPUT_PACKAGE = ROOT_PACKAGE + ".output";
  private static final String INPUT_PACKAGE = ROOT_PACKAGE + ".input";
  private static final String PARSER_INTERFACE_LIST_CLASS_NAME = "GeneratedParserInterfaceList";
  private static final String PARSER_ROOT_INTERFACE_NAME = "WipGeneratedParserRoot";

  private final List<String> jsonProtocolParserClassNames = new ArrayList<String>();
  private final List<ParserRootInterfaceItem> parserRootInterfaceItems =
      new ArrayList<ParserRootInterfaceItem>();
  private final TypeMap typeMap = new TypeMap();
  private final String originReference;

  private final FileSet fileSet;

  Generator(String outputDir, String originReference) {
    this.originReference = originReference;
    this.fileSet = new FileSet(new File(outputDir));
  }

  void go(WipMetamodel.Root metamodel) throws IOException {
    List<Domain> domainList;
    try {
      domainList = metamodel.asDomainList();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }

    initializeKnownTypes(typeMap);

    Set<String> domainTodoList = new HashSet<String>(Arrays.asList(DOMAIN_WHITE_LIST));

    Map<String, DomainGenerator> domainGeneratorMap =
        new HashMap<String, Generator.DomainGenerator>();

    for (Domain domain : domainList) {
      boolean found = domainTodoList.remove(domain.domain());
      if (!found) {
        System.out.println("Domain skipped: " + domain.domain());
        continue;
      }

      DomainGenerator domainGenerator = new DomainGenerator(domain);
      domainGeneratorMap.put(domain.domain(), domainGenerator);

      domainGenerator.registerTypes();
    }

    typeMap.setDomainGeneratorMap(domainGeneratorMap);

    if (!domainTodoList.isEmpty()) {
      throw new RuntimeException("Domains expected but not found: " + domainTodoList);
    }

    for (DomainGenerator domainGenerator : domainGeneratorMap.values()) {
      domainGenerator.generateCommandsAndEvents();
    }

    typeMap.generateRequestedTypes();

    generateParserInterfaceList();

    generateParserRoot(parserRootInterfaceItems);

    fileSet.deleteOtherFiles();
  }

  private class DomainGenerator {
    private final Domain domain;

    DomainGenerator(Domain domain) {
      this.domain = domain;
    }

    void registerTypes() {
      if (domain.types() != null) {
        for (StandaloneType type : domain.types()) {
          typeMap.getTypeData(domain.domain(), type.id()).setType(type);
        }
      }
    }

    void generateCommandsAndEvents() throws IOException {
      for (Command command : domain.commands()) {
        boolean hasResponse = command.returns() != null;
        generateCommandParams(command, hasResponse);
        if (hasResponse) {
          generateCommandData(command);
          String dataFullName = Naming.COMMAND_DATA.getFullName(domain.domain(), command.name());
          jsonProtocolParserClassNames.add(dataFullName);
          parserRootInterfaceItems.add(
              new ParserRootInterfaceItem(domain.domain(), command.name(), Naming.COMMAND_DATA));
        }
      }

      if (domain.events() != null) {
        for (Event event : domain.events()) {
          generateEvenData(event);
          jsonProtocolParserClassNames.add(
              Naming.EVENT_DATA.getFullName(domain.domain(), event.name()));
          parserRootInterfaceItems.add(
              new ParserRootInterfaceItem(domain.domain(), event.name(), Naming.EVENT_DATA));
        }
      }
    }

    private void generateCommandParams(Command command, boolean hasResponse) throws IOException {
      StringBuilder baseTypeBuilder = new StringBuilder();
      baseTypeBuilder.append("org.chromium.sdk.internal.wip.protocol.output.");
      if (hasResponse) {
        baseTypeBuilder.append("WipParamsWithResponse<" +
            Naming.COMMAND_DATA.getFullName(domain.domain(), command.name()) + ">");
      } else {
        baseTypeBuilder.append("WipParams");
      }

      StringBuilder additionalMemberBuilder = new StringBuilder();
      additionalMemberBuilder.append("  public static final String METHOD_NAME = " +
          "org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain." +
          domain.domain().toUpperCase() + " + \"." + command.name() + "\";\n");
      additionalMemberBuilder.append("\n");


      additionalMemberBuilder.append("  @Override protected String getRequestName() {\n");
      additionalMemberBuilder.append("    return METHOD_NAME;\n");
      additionalMemberBuilder.append("  }\n");
      additionalMemberBuilder.append("\n");

      if (hasResponse) {
        String dataInterfaceFullname =
            Naming.COMMAND_DATA.getFullName(domain.domain(), command.name());
        additionalMemberBuilder.append(
            "  @Override public " + dataInterfaceFullname + " parseResponse(" +
            "org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, " +
            "org.chromium.sdk.internal.wip.protocol.input." +
            PARSER_ROOT_INTERFACE_NAME + " parser) " +
            "throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n");
        additionalMemberBuilder.append("    return parser." +
            Naming.COMMAND_DATA.getParseMethodName(domain.domain(), command.name()) +
            "(data.getUnderlyingObject());\n");
        additionalMemberBuilder.append("  }\n");
        additionalMemberBuilder.append("\n");
      }

      generateOutputClass(Naming.PARAMS, command.name(), command.description(),
          baseTypeBuilder.toString(), additionalMemberBuilder.toString(), command.parameters(),
          PropertyLikeAccess.PARAMETER);
    }

    private void generateCommandAdditionalParam(StandaloneType type) throws IOException {
      generateOutputClass(Naming.ADDITIONAL_PARAM, type.id(), type.description(),
          "org.json.simple.JSONObject", null, type.properties(), PropertyLikeAccess.PROPERTY);
    }

    private <P> void generateOutputClass(ClassNameScheme nameScheme, String baseName,
        String description, String baseType, String additionalMemberText,
        List<P> properties, PropertyLikeAccess<P> propertyAccess) throws IOException {
      String className = nameScheme.getShortName(baseName);
      JavaFileUpdater fileUpdater = startJavaFile(nameScheme, domain, baseName);

      Writer writer = fileUpdater.getWriter();

      if (description != null) {
        writer.write("/**\n" + description + "\n */\n");
      }
      writer.write("public class " + className +
          " extends " + baseType + " {\n");

      OutputClassScope classScope = new OutputClassScope(writer, className);

      if (additionalMemberText != null) {
        classScope.addMember("param-specific", additionalMemberText);
      }

      classScope.generateCommandParamsBody(properties, propertyAccess, baseName);

      classScope.writeAdditionalMembers(writer);

      writer.write("}\n");

      writer.close();

      fileUpdater.update();
    }

    StandaloneTypeBinding createStandaloneOutputTypeBinding(final StandaloneType type,
        final String name) {
      return switchByType(type, TypedObjectAccess.FOR_STANDALONE,
          new TypeVisitor<StandaloneTypeBinding>() {
        @Override
        public StandaloneTypeBinding visitObject(List<ObjectProperty> properties) {
          return new StandaloneTypeBinding() {
            @Override
            public String getJavaFullName() {
              return Naming.ADDITIONAL_PARAM.getFullName(domain.domain(), name);
            }

            @Override
            public void generate() throws IOException {
              generateCommandAdditionalParam(type);
            }
          };
        }

        @Override
        public StandaloneTypeBinding visitEnum(List<String> enumConstants) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitRef(String refName) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitBoolean() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitString() {
          return createTypedefTypeBinding(type, "String", Naming.OUTPUT_TYPEDEF);
        }
        @Override public StandaloneTypeBinding visitInteger() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitNumber() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitArray(ArrayItemType items) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitUnknown() {
          throw new RuntimeException();
        }
      });
    }

    StandaloneTypeBinding createStandaloneInputTypeBinding(final StandaloneType type) {
      return switchByType(type, TypedObjectAccess.FOR_STANDALONE,
          new TypeVisitor<StandaloneTypeBinding>() {
        @Override
        public StandaloneTypeBinding visitObject(List<ObjectProperty> properties) {
          return createStandaloneObjectInputTypeBinding(type, properties);
        }

        @Override
        public StandaloneTypeBinding visitEnum(List<String> enumConstants) {
          return createStandaloneEnumInputTypeBinding(type, enumConstants);
        }

        @Override public StandaloneTypeBinding visitRef(String refName) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitBoolean() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitString() {
          return createTypedefTypeBinding(type, "String", Naming.INPUT_TYPEDEF);
        }
        @Override public StandaloneTypeBinding visitInteger() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitNumber() {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitArray(ArrayItemType items) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitUnknown() {
          throw new RuntimeException();
        }
      });
    }

    StandaloneTypeBinding createStandaloneObjectInputTypeBinding(final StandaloneType type,
        final List<ObjectProperty> properties) {
      final String name = type.id();
      final String fullTypeName = Naming.INPUT_VALUE.getFullName(domain.domain(), name);
      jsonProtocolParserClassNames.add(fullTypeName);

      return new StandaloneTypeBinding() {
        @Override
        public String getJavaFullName() {
          return fullTypeName;
        }

        @Override
        public void generate() throws IOException {
          String description = type.description();

          String className = Naming.INPUT_VALUE.getShortName(name);
          JavaFileUpdater fileUpdater = startJavaFile(Naming.INPUT_VALUE, domain, name);

          Writer writer = fileUpdater.getWriter();

          if (description != null) {
            writer.write("/**\n " + description + "\n */\n");
          }

          writer.write("@org.chromium.sdk.internal.protocolparser.JsonType\n");
          writer.write("public interface " + className +" {\n");

          InputClassScope classScope = new InputClassScope(writer, className);

          classScope.generateStandaloneTypeBody(properties);

          classScope.writeAdditionalMembers(writer);

          writer.write("}\n");

          fileUpdater.update();
        }
      };
    }

    StandaloneTypeBinding createStandaloneEnumInputTypeBinding(final StandaloneType type,
        final List<String> enumConstants) {
      final String name = type.id();
      return new StandaloneTypeBinding() {
        @Override
        public String getJavaFullName() {
          return Naming.INPUT_ENUM.getFullName(domain.domain(), name);
        }

        @Override
        public void generate() throws IOException {
          String description = type.description();

          String className = Naming.INPUT_ENUM.getShortName(name);
          JavaFileUpdater fileUpdater = startJavaFile(Naming.INPUT_ENUM, domain, name);

          Writer writer = fileUpdater.getWriter();

          if (description != null) {
            writer.write("/**\n " + description + "\n */\n");
          }

          writer.write("public enum " + className +" {\n");

          boolean first = true;
          for (String constName : enumConstants) {
            if (first) {
              writer.write("\n  ");
            } else {
              writer.write(",\n  ");
            }
            writer.write(constName.toUpperCase());
            first = false;
          }
          writer.write("\n");

          writer.write("}\n");

          fileUpdater.update();
        }
      };
    }

    /**
     * Typedef is an empty class that just holds description and
     * refers to an actual type (such as String).
     */
    StandaloneTypeBinding createTypedefTypeBinding(final StandaloneType type,
        String actualJavaName, final ClassNameScheme nameScheme) {
      final String name = type.id();
      String typedefJavaName = nameScheme.getFullName(domain.domain(), name);
      final String javaName = actualJavaName + "/*See " + typedefJavaName + "*/";

      return new StandaloneTypeBinding() {
        @Override public String getJavaFullName() {
          return javaName;
        }
        @Override public void generate() throws IOException {
          String description = type.description();

          String className = nameScheme.getShortName(name);
          JavaFileUpdater fileUpdater = startJavaFile(nameScheme, domain, name);

          Writer writer = fileUpdater.getWriter();

          if (description != null) {
            writer.write("/**\n " + description + "\n */\n");
          }

          writer.write("public class " + className +
              " {/*Typedef class, merely holds a javadoc.*/}\n");

          fileUpdater.update();
        }
      };
    }

    private void generateCommandData(Command command) throws IOException {
      String className = Naming.COMMAND_DATA.getShortName(command.name());
      JavaFileUpdater fileUpdater = startJavaFile(Naming.COMMAND_DATA, domain, command.name());

      Writer writer = fileUpdater.getWriter();

      generateJsonProtocolInterface(writer, className, command.description(),
          command.returns(), null);

      fileUpdater.update();
    }

    private void generateEvenData(Event event) throws IOException {
      String className = Naming.EVENT_DATA.getShortName(event.name());
      JavaFileUpdater fileUpdater = startJavaFile(Naming.EVENT_DATA, domain, event.name());
      String domainName = domain.domain();
      String fullName = Naming.EVENT_DATA.getFullName(domainName, event.name());

      String eventTypeMemberText =
          "  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<" +
          fullName +
          "> TYPE\n      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<" +
          fullName +
          ">(\"" + domainName + "." + event.name() + "\", " + fullName + ".class) {\n" +
          "    @Override public " + fullName + " parse(" + INPUT_PACKAGE + "." +
          PARSER_ROOT_INTERFACE_NAME + " parser, org.json.simple.JSONObject obj)" +
          " throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n" +
          "      return parser." + Naming.EVENT_DATA.getParseMethodName(domainName, event.name()) +
          "(obj);\n" +
          "    }\n" +
          "  };\n";

      Writer writer = fileUpdater.getWriter();

      generateJsonProtocolInterface(writer, className, event.description(), event.parameters(),
          eventTypeMemberText);

      fileUpdater.update();
    }

    private void generateJsonProtocolInterface(Writer writer, String className, String description,
        List<Parameter> parameters, String additionalMembersText) throws IOException {
      if (description != null) {
        writer.write("/**\n " + description + "\n */\n");
      }

      writer.write("@org.chromium.sdk.internal.protocolparser.JsonType\n");
      writer.write("public interface " + className +" {\n");

      InputClassScope classScope = new InputClassScope(writer, className);

      if (additionalMembersText != null) {
        classScope.addMember("extra", additionalMembersText);
      }

      classScope.generateMainJsonProtocolInterfaceBody(parameters);

      classScope.writeAdditionalMembers(writer);

      writer.write("}\n");
    }

    private abstract class ClassScope {
      private final List<String> additionalMemberTexts = new ArrayList<String>(2);
      private final Writer writer;
      private final String packageName;
      private final String shortClassName;

      ClassScope(Writer writer, String packageName, String shortClassName) {
        this.writer = writer;
        this.packageName = packageName;
        this.shortClassName = shortClassName;
      }

      protected String getShortClassName() {
        return shortClassName;
      }

      String getFullName() {
        return packageName + "." + shortClassName;
      }

      void addMember(String key, String text) {
        additionalMemberTexts.add(text);
      }

      void writeAdditionalMembers(Writer writer) throws IOException {
        for (String text : additionalMemberTexts) {
          writer.write(text);
        }
      }

      protected Writer getWriter() {
        return writer;
      }

      protected abstract MemberScope newMemberScope(String memberName);
      protected abstract TypeData.Direction getTypeDirection();

      /**
       * Member scope is used to generate additional types that are used only from method.
       * These types will be named after this method.
       */
      protected abstract class MemberScope {
        private final String memberName;

        protected MemberScope(String memberName) {
          this.memberName = memberName;
        }

        private ReturnTypeData resolveType(ObjectProperty objectProperty) {
          return resolveType(objectProperty, TypedObjectAccess.FOR_OBJECT_PROPERTY);
        }

        private ReturnTypeData resolveType(Parameter parameter) {
          return resolveType(parameter, TypedObjectAccess.FOR_PARAMETER);
        }

        private ReturnTypeData resolveType(ArrayItemType arrayItemType) {
          return resolveType(arrayItemType, TypedObjectAccess.FOR_ARRAY_ITEM);
        }

        <T> ReturnTypeData resolveType(final T typedObject, final TypedObjectAccess<T> access) {
          ResolvedTypeData resolvedTypeData =
              switchByType(typedObject, access, new TypeVisitor<ResolvedTypeData>() {
            @Override public ResolvedTypeData visitRef(String refName) {
              String refTypeName = resolveRefType(domain.domain(), refName, getTypeDirection());
              return new ResolvedTypeData.UserType(refTypeName);
            }
            @Override public ResolvedTypeData visitBoolean() {
              return ResolvedTypeData.BOOLEAN;
            }

            @Override public ResolvedTypeData visitEnum(List<String> enumConstants) {
              String enumName = generateEnum(getDescription(), enumConstants);
              return new ResolvedTypeData.UserType(enumName);
            }

            @Override public ResolvedTypeData visitString() {
              return ResolvedTypeData.STRING;
            }
            @Override public ResolvedTypeData visitInteger() {
              return ResolvedTypeData.LONG;
            }
            @Override public ResolvedTypeData visitNumber() {
              return ResolvedTypeData.NUMBER;
            }
            @Override public ResolvedTypeData visitArray(ArrayItemType items) {
              ReturnTypeData type = resolveType(items);
              return new ResolvedTypeData.UserType("java.util.List<" + type.getBoxedName() + ">");
            }
            @Override public ResolvedTypeData visitObject(List<ObjectProperty> properties) {
              String nestedObjectName;
              try {
                nestedObjectName = generateNestedObject(getDescription(), properties);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return new ResolvedTypeData.UserType(nestedObjectName);
            }
            @Override public ResolvedTypeData visitUnknown() {
              return ResolvedTypeData.ANY;
            }
            private <T> String getDescription() {
              return access.getDescription(typedObject);
            }
          });

          return resolvedTypeData.getReturnTypeData(
              access.getOptional(typedObject) == Boolean.TRUE);
        }

        protected String getMemberName() {
          return memberName;
        }

        protected abstract String generateEnum(String description, List<String> enumConstants);
        protected abstract String generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException;
      }
    }

    class InputClassScope extends ClassScope {
      InputClassScope(Writer writer, String shortClassName) {
        super(writer, ClassNameScheme.Input.getPackageName(domain.domain()), shortClassName);
      }

      public void generateMainJsonProtocolInterfaceBody(List<Parameter> parameters)
          throws IOException {
        if (parameters != null) {
          for (Parameter param : parameters) {
            if (param.description() != null) {
              getWriter().write("  /**\n   " + param.description() + "\n   */\n");
            }

            String methodName = generateMethodNameSubstitute(param.name(), getWriter());

            ClassScope.MemberScope memberScope = newMemberScope(param.name());

            ReturnTypeData returnTypeData = memberScope.resolveType(param);

            returnTypeData.writeAnnotations(getWriter(), "  ");

            getWriter().write("  " + returnTypeData.getName() + " " + methodName + "();\n");
            getWriter().write("\n");
          }
        }
      }

      void generateStandaloneTypeBody(List<ObjectProperty> properties) throws IOException {
        for (ObjectProperty objectProperty : properties) {
          String propertyName = objectProperty.name();

          if (objectProperty.description() != null) {
            getWriter().write("  /**\n   " + objectProperty.description() + "\n   */\n");
          }

          String methodName = generateMethodNameSubstitute(propertyName, getWriter());

          ClassScope.MemberScope memberScope = newMemberScope(propertyName);

          ReturnTypeData returnTypeData = memberScope.resolveType(objectProperty);

          returnTypeData.writeAnnotations(getWriter(), "  ");

          getWriter().write("  " + returnTypeData.getName() + " " +
              methodName + "();\n");
          getWriter().write("\n");
        }
      }

      @Override
      protected TypeData.Direction getTypeDirection() {
        return TypeData.Direction.INPUT;
      }

      @Override
      protected MemberScope newMemberScope(String memberName) {
        return new InputMemberScope(memberName);
      }

      class InputMemberScope extends MemberScope {
        InputMemberScope(String memberName) {
          super(memberName);
        }

        @Override
        protected String generateEnum(String description, List<String> enumConstants) {
          StringBuilder builder = new StringBuilder();
          if (description != null) {
            builder.append("  /**\n   " + description + "\n   */\n");
          }

          String enumName = capitalizeFirstChar(getMemberName());
          builder.append("  public enum " + enumName + " {\n");
          for (String constant : enumConstants) {
            builder.append("    " + constant.toUpperCase() + ",\n");
          }
          builder.append("  }\n");
          addMember(enumName, builder.toString());

          return enumName;
        }

        @Override
        protected String generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException {
          StringBuilder builder = new StringBuilder();

          if (description != null) {
            builder.append("  /**\n   " + description + "\n   */\n");
          }

          String objectName = capitalizeFirstChar(getMemberName());

          if (propertyList == null) {
            builder.append("  @org.chromium.sdk.internal.protocolparser.JsonType(" +
                "allowsOtherProperties=true)\n");
            builder.append("  public interface " + objectName +
                " extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {\n");
            builder.append("  }\n");
          } else {
            builder.append("  @org.chromium.sdk.internal.protocolparser.JsonType\n");
            builder.append("  public interface " + objectName + " {\n");
            for (ObjectProperty property : propertyList) {
              if (property.description() != null) {
                builder.append("    /**\n     " + property.description() + "\n     */\n");
              }

              String methodName = generateMethodNameSubstitute(property.name(), builder);

              MemberScope memberScope = newMemberScope(property.name());

              ReturnTypeData returnTypeData = memberScope.resolveType(property);

              returnTypeData.writeAnnotations(builder, "    ");

              builder.append("    " + returnTypeData.getName() + " " + methodName +
                  "();\n");
              builder.append("\n");
            }
            builder.append("  }\n");
          }

          addMember(objectName, builder.toString());

          jsonProtocolParserClassNames.add(getFullName() + "." + objectName);

          return objectName;
        }
      }
    }

    class OutputClassScope extends ClassScope {
      OutputClassScope(Writer writer, String shortClassName) {
        super(writer, ClassNameScheme.Output.getPackageName(domain.domain()), shortClassName);
      }

      <P> void generateCommandParamsBody(List<P> parameters, PropertyLikeAccess<P> access,
          String commandName) throws IOException {
        if (parameters != null) {
          boolean hasDoc = false;
          for (P param : parameters) {
            if (access.forTypedObject().getDescription(param) != null) {
              hasDoc = true;
              break;
            }
          }
          if (hasDoc) {
            getWriter().write("  /**\n");
            for (P param : parameters) {
              String propertyDescription = access.forTypedObject().getDescription(param);
              if (propertyDescription != null) {
                getWriter().write("   @param " + getParamName(param, access) + " " +
                    propertyDescription + "\n");
              }
            }
            getWriter().write("   */\n");
          }
        }
        getWriter().write("  public " + getShortClassName() +"(");
        {
          boolean needComa = false;
          if (parameters != null) {
            for (P param : parameters) {
              if (needComa) {
                getWriter().write(", ");
              }
              String paramName = getParamName(param, access);
              ClassScope.MemberScope memberScope = newMemberScope(paramName);
              ReturnTypeData typeData = memberScope.resolveType(param, access.forTypedObject());
              getWriter().write(typeData.getName() + " " + paramName);
              needComa = true;
            }
          }
        }
        getWriter().write(") {\n");
        if (parameters != null) {
          for (P param : parameters) {
            boolean isOptional = access.forTypedObject().getOptional(param) == Boolean.TRUE;
            String paramName = getParamName(param, access);
            if (isOptional) {
              getWriter().write("    if (" + paramName + " != null) {\n  ");
            }
            getWriter().write("    this.put(\"" + access.getName(param) + "\", " + paramName +
                ");\n");
            if (isOptional) {
              getWriter().write("    }\n");
            }
          }
        }
        getWriter().write("  }\n");
        getWriter().write("\n");
      }

      @Override
      protected TypeData.Direction getTypeDirection() {
        return TypeData.Direction.OUTPUT;
      }

      @Override
      protected MemberScope newMemberScope(String memberName) {
        return new OutputMemberScope(memberName);
      }

      class OutputMemberScope extends MemberScope {
        protected OutputMemberScope(String memberName) {
          super(memberName);
        }

        @Override
        protected String generateEnum(String description, List<String> enumConstants) {
          StringBuilder builder = new StringBuilder();
          if (description != null) {
            builder.append("  /**\n   " + description + "\n   */\n");
          }
          String enumName = capitalizeFirstChar(getMemberName());
          builder.append("  public enum " + enumName + " implements org.json.simple.JSONAware{\n");
          for (String constant : enumConstants) {
            builder.append("    " + constant.toUpperCase() + "(\"" + constant + "\"),\n");
          }
          builder.append("    ;\n");
          builder.append("    private final String protocolValue;\n");
          builder.append("\n");
          builder.append("    " + enumName + "(String protocolValue) {\n");
          builder.append("      this.protocolValue = protocolValue;\n");
          builder.append("    }\n");
          builder.append("\n");
          builder.append("    @Override public String toJSONString() {\n");
          builder.append("      return '\"' + protocolValue + '\"';\n");
          builder.append("    }\n");
          builder.append("  }\n");
          addMember(enumName, builder.toString());
          return enumName;
        }

        @Override
        protected String generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException {
          throw new UnsupportedOperationException();
        }
      }
    }
  }

  private static abstract class ResolvedTypeData {
    abstract ReturnTypeData getReturnTypeData(boolean optional);

    private static class PredefinedTypeData extends ResolvedTypeData {
      private final ReturnTypeData optionalReturnType;
      private final ReturnTypeData nonOptionalReturnType;

      PredefinedTypeData(String name, boolean nullable) {
        this(name, name, nullable);
      }

      PredefinedTypeData(String nullableName, String nonNullableName, boolean nullable) {
        this.optionalReturnType = new ReturnTypeImpl(nullableName, nullableName, true, nullable);
        this.nonOptionalReturnType =
            new ReturnTypeImpl(nonNullableName, nullableName, false, nullable);
      }

      @Override
      ReturnTypeData getReturnTypeData(boolean optional) {
        return optional ? optionalReturnType : nonOptionalReturnType;
      }

      private static class ReturnTypeImpl extends ReturnTypeData {
        private final String name;
        private final String boxedName;
        private final boolean optional;
        private final boolean nullable;

        ReturnTypeImpl(String name, String boxedName, boolean optional, boolean nullable) {
          this.name = name;
          this.boxedName = boxedName;
          this.optional = optional;
          this.nullable = nullable;
        }
        @Override boolean isOptional() {
          return optional;
        }
        @Override boolean isNullable() {
          return nullable;
        }
        @Override String getName() {
          return name;
        }
        @Override String getBoxedName() {
          return boxedName;
        }
      }
    }

    static class UserType extends ResolvedTypeData {
      private final String name;

      UserType(String name) {
        this.name = name;
      }

      @Override
      ReturnTypeData getReturnTypeData(final boolean optional) {
        return new ReturnTypeData() {
          @Override boolean isOptional() {
            return optional;
          }
          @Override boolean isNullable() {
            return false;
          }
          @Override String getName() {
            return name;
          }
          @Override
          String getBoxedName() {
            return name;
          }
        };
      }
    }

    static final ResolvedTypeData BOOLEAN = new PredefinedTypeData("Boolean", "boolean", false);
    static final ResolvedTypeData STRING = new PredefinedTypeData("String", false);
    static final ResolvedTypeData LONG = new PredefinedTypeData("Long", "long", false);
    static final ResolvedTypeData NUMBER = new PredefinedTypeData("Number", false);
    static final ResolvedTypeData ANY = new PredefinedTypeData("Object", true);
  }

  private static abstract class ReturnTypeData {
    abstract boolean isOptional();
    abstract boolean isNullable();
    abstract String getName();
    abstract String getBoxedName();

    void writeAnnotations(Appendable appendable, String indent) throws IOException {
      if (isOptional()) {
        appendable.append(indent + "@org.chromium.sdk.internal.protocolparser.JsonOptionalField\n");
      }
      if (isNullable()) {
        appendable.append(indent + "@org.chromium.sdk.internal.protocolparser.JsonNullable\n");
      }
    }
  }


  private void generateParserInterfaceList() throws IOException {
    JavaFileUpdater fileUpdater =
        startJavaFile(INPUT_PACKAGE, PARSER_INTERFACE_LIST_CLASS_NAME + ".java");

    // Write classes in stable order.
    Collections.sort(jsonProtocolParserClassNames);

    Writer writer = fileUpdater.getWriter();

    writer.write("public class " + PARSER_INTERFACE_LIST_CLASS_NAME + " {\n");
    writer.write("  public static final Class<?>[] LIST = {\n");
    for (String name : jsonProtocolParserClassNames) {
      writer.write("    " + name + ".class" + ",\n");
    }
    writer.write("  };\n");
    writer.write("}\n");

    fileUpdater.update();
  }

  private void generateParserRoot(List<ParserRootInterfaceItem> parserRootInterfaceItems)
      throws IOException {
    JavaFileUpdater fileUpdater =
        startJavaFile(INPUT_PACKAGE, PARSER_ROOT_INTERFACE_NAME + ".java");

    // Write classes in stable order.
    Collections.sort(parserRootInterfaceItems);

    Writer writer = fileUpdater.getWriter();

    writer.write("@org.chromium.sdk.internal.protocolparser.JsonParserRoot\n");
    writer.write("public interface " + PARSER_ROOT_INTERFACE_NAME + " {\n");
    for (ParserRootInterfaceItem item : parserRootInterfaceItems) {
      item.writeCode(writer);
    }
    writer.write("}\n");

    fileUpdater.update();
  }

  private static class ParserRootInterfaceItem implements Comparable<ParserRootInterfaceItem> {
    private final String domain;
    private final String name;
    private final ClassNameScheme.Input nameScheme;
    private final String fullName;

    public ParserRootInterfaceItem(String domain, String name, ClassNameScheme.Input nameScheme) {
      this.domain = domain;
      this.name = name;
      this.nameScheme = nameScheme;
      fullName = nameScheme.getFullName(domain, name);
    }

    void writeCode(Writer writer) throws IOException {
      writer.write("  @org.chromium.sdk.internal.protocolparser.JsonParseMethod\n");
      writer.write("  " + fullName + " " + nameScheme.getParseMethodName(domain, name) +
          "(org.json.simple.JSONObject obj)" +
          " throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;\n");
      writer.write("\n");
    }

    @Override
    public int compareTo(ParserRootInterfaceItem o) {
      return this.fullName.compareTo(o.fullName);
    }
  }

  private abstract static class ClassNameScheme {
    private final String suffix;

    ClassNameScheme(String suffix) {
      this.suffix = suffix;
    }

    String getFullName(String domainName, String baseName) {
      return getPackageNameVirtual(domainName) + "." + getShortName(baseName);
    }

    String getShortName(String baseName) {
      return capitalizeFirstChar(baseName) + suffix;
    }

    protected abstract String getPackageNameVirtual(String domainName);

    static class Input extends ClassNameScheme {
      Input(String suffix) {
        super(suffix);
      }

      @Override protected String getPackageNameVirtual(String domainName) {
        return getPackageName(domainName);
      }

      String getParseMethodName(String domain, String name) {
        return "parse" + capitalizeFirstChar(domain) + getShortName(name);
      }

      static String getPackageName(String domainName) {
        return INPUT_PACKAGE + "." + domainName.toLowerCase();
      }
    }

    static class Output extends ClassNameScheme {
      Output(String suffix) {
        super(suffix);
      }

      @Override protected String getPackageNameVirtual(String domainName) {
        return getPackageName(domainName);
      }

      static String getPackageName(String domainName) {
        return OUTPUT_PACKAGE + "." + domainName.toLowerCase();
      }
    }
  }

  interface Naming {
    ClassNameScheme PARAMS = new ClassNameScheme.Output("Params");
    ClassNameScheme ADDITIONAL_PARAM = new ClassNameScheme.Output("Param");
    ClassNameScheme OUTPUT_TYPEDEF = new ClassNameScheme.Output("Typedef");

    ClassNameScheme.Input COMMAND_DATA = new ClassNameScheme.Input("Data");
    ClassNameScheme.Input EVENT_DATA = new ClassNameScheme.Input("EventData");
    ClassNameScheme INPUT_VALUE = new ClassNameScheme.Input("Value");
    ClassNameScheme INPUT_ENUM = new ClassNameScheme.Input("Enum");
    ClassNameScheme INPUT_TYPEDEF = new ClassNameScheme.Input("Typedef");
  }

  private static <P> String getParamName(P param, PropertyLikeAccess<P> access) {
    String paramName = access.getName(param);
    if (access.forTypedObject().getOptional(param) == Boolean.TRUE) {
      paramName = paramName + "Opt";
    }
    return paramName;
  }

  /**
   * A set of external methods that provides a type-safe polymorphous access
   * to several unrelated types.
   */
  private static abstract class TypedObjectAccess<T> {
    abstract String getDescription(T obj);
    abstract Boolean getOptional(T obj);
    abstract String getRef(T obj);
    abstract List<String> getEnum(T obj);
    abstract String getType(T obj);
    abstract ArrayItemType getItems(T obj);
    abstract List<ObjectProperty> getProperties(T obj);

    static final TypedObjectAccess<Parameter> FOR_PARAMETER = new TypedObjectAccess<Parameter>() {
      @Override String getDescription(Parameter obj) {
        return obj.description();
      }
      @Override Boolean getOptional(Parameter obj) {
        return obj.optional();
      }
      @Override String getRef(Parameter obj) {
        return obj.ref();
      }
      @Override List<String> getEnum(Parameter obj) {
        return obj.getEnum();
      }
      @Override String getType(Parameter obj) {
        return obj.type();
      }
      @Override ArrayItemType getItems(Parameter obj) {
        return obj.items();
      }
      @Override List<ObjectProperty> getProperties(Parameter obj) {
        return obj.properties();
      }
    };

    static final TypedObjectAccess<ObjectProperty> FOR_OBJECT_PROPERTY =
        new TypedObjectAccess<ObjectProperty>() {
      @Override String getDescription(ObjectProperty obj) {
        return obj.description();
      }
      @Override Boolean getOptional(ObjectProperty obj) {
        return obj.optional();
      }
      @Override String getRef(ObjectProperty obj) {
        return obj.ref();
      }
      @Override List<String> getEnum(ObjectProperty obj) {
        return obj.getEnum();
      }
      @Override String getType(ObjectProperty obj) {
        return obj.type();
      }
      @Override ArrayItemType getItems(ObjectProperty obj) {
        return obj.items();
      }
      @Override List<ObjectProperty> getProperties(ObjectProperty obj) {
        throw new RuntimeException();
      }
    };

    static final TypedObjectAccess<ArrayItemType> FOR_ARRAY_ITEM =
        new TypedObjectAccess<ArrayItemType>() {
      @Override String getDescription(ArrayItemType obj) {
        return obj.description();
      }
      @Override Boolean getOptional(ArrayItemType obj) {
        return obj.optional();
      }
      @Override String getRef(ArrayItemType obj) {
        return obj.ref();
      }
      @Override List<String> getEnum(ArrayItemType obj) {
        return obj.getEnum();
      }
      @Override String getType(ArrayItemType obj) {
        return obj.type();
      }
      @Override ArrayItemType getItems(ArrayItemType obj) {
        return obj.items();
      }
      @Override List<ObjectProperty> getProperties(ArrayItemType obj) {
        return obj.properties();
      }
    };

    static final TypedObjectAccess<StandaloneType> FOR_STANDALONE =
        new TypedObjectAccess<StandaloneType>() {
      @Override String getDescription(StandaloneType obj) {
        return obj.description();
      }
      @Override Boolean getOptional(StandaloneType obj) {
        return null;
      }
      @Override String getRef(StandaloneType obj) {
        return null;
      }
      @Override List<String> getEnum(StandaloneType obj) {
        return obj.getEnum();
      }
      @Override String getType(StandaloneType obj) {
        return obj.type();
      }
      @Override ArrayItemType getItems(StandaloneType obj) {
        return null;
      }
      @Override List<ObjectProperty> getProperties(StandaloneType obj) {
        return obj.properties();
      }
    };
  }

  /**
   * A polymorphopus access to something like property (with name and type).
   */
  private static abstract class PropertyLikeAccess<T> {
    abstract TypedObjectAccess<T> forTypedObject();
    abstract String getName(T obj);

    static final PropertyLikeAccess<Parameter> PARAMETER = new PropertyLikeAccess<Parameter>() {
      @Override TypedObjectAccess<Parameter> forTypedObject() {
        return TypedObjectAccess.FOR_PARAMETER;
      }
      @Override String getName(Parameter obj) {
        return obj.name();
      }
    };

    static final PropertyLikeAccess<ObjectProperty> PROPERTY =
        new PropertyLikeAccess<ObjectProperty>() {
      @Override TypedObjectAccess<ObjectProperty> forTypedObject() {
        return TypedObjectAccess.FOR_OBJECT_PROPERTY;
      }
      @Override String getName(ObjectProperty obj) {
        return obj.name();
      }
    };
  }

  /**
   * Resolve absolute (DOMAIN.TYPE) or relative (TYPE) typename.
   */
  private String resolveRefType(String scopeDomainName, String refName,
      TypeData.Direction direction) {
    int pos = refName.indexOf('.');
    String domainName;
    String shortName;
    if (pos == -1) {
      domainName = scopeDomainName;
      shortName = refName;
    } else {
      domainName = refName.substring(0, pos);
      shortName = refName.substring(pos + 1);
    }
    return typeMap.resolve(domainName, shortName, direction);
  }

  private String generateMethodNameSubstitute(String originalName, Appendable output)
      throws IOException {
    if (!BAD_METHOD_NAMES.contains(originalName)) {
      return originalName;
    }
    output.append("  @org.chromium.sdk.internal.protocolparser.JsonField(jsonLiteralName=\"" +
        originalName + "\")\n");
    return "get" + Character.toUpperCase(originalName.charAt(0)) + originalName.substring(1);
  }

  private static String capitalizeFirstChar(String str) {
    if (Character.isLowerCase(str.charAt(0))) {
      str = Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    return str;
  }

  private JavaFileUpdater startJavaFile(ClassNameScheme nameScheme, Domain domain,
      String baseName) throws IOException {
    String packageName = nameScheme.getPackageNameVirtual(domain.domain());
    String fileName = nameScheme.getShortName(baseName) + ".java";
    return startJavaFile(packageName, fileName);
  }

  private JavaFileUpdater startJavaFile(String packageName, String filename) throws IOException {
    String filePath = packageName.replace('.', '/');

    JavaFileUpdater fileUpdater = fileSet.createFileUpdater(filePath + "/" + filename);
    Writer writer = fileUpdater.getWriter();
    writer.write("// Generated source.\n");
    writer.write("// Generator: " + this.getClass().getCanonicalName() + "\n");
    writer.write("// Origin: " + originReference + "\n\n");
    writer.write("package " + packageName + ";\n\n");
    return fileUpdater;
  }

  private interface TypeVisitor<R> {

    R visitRef(String refName);

    R visitBoolean();

    R visitEnum(List<String> enumConstants);

    R visitString();

    R visitInteger();

    R visitNumber();

    R visitArray(ArrayItemType items);

    R visitObject(List<ObjectProperty> properties);

    R visitUnknown();

  }

  private static <R, T> R switchByType(T typedObject, TypedObjectAccess<T> access,
      TypeVisitor<R> visitor) {
    String refName = access.getRef(typedObject);
    if (refName != null) {
      return visitor.visitRef(refName);
    }
    String typeName = access.getType(typedObject);
    if (WipMetamodel.BOOLEAN_TYPE.equals(typeName)) {
      return visitor.visitBoolean();
    } else if (WipMetamodel.STRING_TYPE.equals(typeName)) {
      if (access.getEnum(typedObject) != null) {
        return visitor.visitEnum(access.getEnum(typedObject));
      }
      return visitor.visitString();
    } else if (WipMetamodel.INTEGER_TYPE.equals(typeName)) {
      return visitor.visitInteger();
    } else if (WipMetamodel.NUMBER_TYPE.equals(typeName)) {
      return visitor.visitNumber();
    } else if (WipMetamodel.ARRAY_TYPE.equals(typeName)) {
      return visitor.visitArray(access.getItems(typedObject));
    } else if (WipMetamodel.OBJECT_TYPE.equals(typeName)) {
      return visitor.visitObject(access.getProperties(typedObject));
    } else if (WipMetamodel.ANY_TYPE.equals(typeName)) {
      return visitor.visitUnknown();
    } else if (WipMetamodel.UNKNOWN_TYPE.equals(typeName)) {
      return visitor.visitUnknown();
    }
    throw new RuntimeException("Unrecognized type " + typeName);
  }

  private static class TypeData {
    private final String domain;
    private final String name;

    private Input input = null;
    private Output output = null;

    private StandaloneType type = null;

    TypeData(String domain, String name) {
      this.domain = domain;
      this.name = name;
    }

    void setType(StandaloneType type) {
      this.type = type;
    }

    Input getInput() {
      if (input == null) {
        input = new Input();
      }
      return input;
    }

    Output getOutput() {
      if (output == null) {
        output = new Output();
      }
      return output;
    }

    TypeRef get(Direction direction) {
      return direction.get(this);
    }

    void checkComplete() {
      if (input != null) {
        input.checkResolved();
      }
      if (output != null) {
        output.checkResolved();
      }
    }

    enum Direction {
      INPUT() {
        @Override TypeRef get(TypeData typeData) {
          return typeData.getInput();
        }
      },
      OUTPUT() {
        @Override TypeRef get(TypeData typeData) {
          return typeData.getOutput();
        }
      };
      abstract TypeRef get(TypeData typeData);
    }

    abstract class TypeRef {
      private boolean requested = false;
      private boolean generated = false;
      private StandaloneTypeBinding binding = null;

      String resolve(TypeMap typeMap, DomainGenerator domainGenerator) {
        if (binding == null) {
          binding = resolveImpl(domainGenerator);
          typeMap.addTypeToGenerate(this);
          requested = true;
        }
        return binding.getJavaFullName();
      }

      abstract StandaloneTypeBinding resolveImpl(DomainGenerator domainGenerator);

      void generate() throws IOException {
        if (!generated) {
          generated = true;
          binding.generate();
        }
      }

      boolean isRequested() {
        return requested;
      }
    }

    class Output extends TypeRef {
      void checkResolved() {
        if (type == null) {
          throw new RuntimeException();
        }
      }

      @Override
      StandaloneTypeBinding resolveImpl(final DomainGenerator domainGenerator) {
        if (type == null) {
          throw new RuntimeException();
        }
        return domainGenerator.createStandaloneOutputTypeBinding(type, name);
      }
    }

    class Input extends TypeRef {
      private String predefinedJavaTypeName = null;

      @Override
      StandaloneTypeBinding resolveImpl(DomainGenerator domainGenerator) {
        if (predefinedJavaTypeName == null) {
          if (type == null) {
            throw new RuntimeException();
          }
          return domainGenerator.createStandaloneInputTypeBinding(type);
        } else {
          return new StandaloneTypeBinding() {
            @Override
            public String getJavaFullName() {
              return predefinedJavaTypeName;
            }

            @Override
            public void generate() {
            }
          };
        }
      }

      void checkResolved() {
        if (predefinedJavaTypeName != null) {
          if (type != null) {
            throw new RuntimeException();
          }
          if (!isRequested()) {
            throw new RuntimeException("Unused predifined type");
          }
        }
      }

      void setJavaTypeName(String javaTypeName) {
        if (this.predefinedJavaTypeName != null) {
          throw new RuntimeException("Type already initialized");
        }
        this.predefinedJavaTypeName = javaTypeName;
      }
    }
  }

  private interface StandaloneTypeBinding {
    abstract String getJavaFullName();
    abstract void generate() throws IOException;
  }


  /**
   * Keeps track of all referenced types.
   * A type may be used and resolved (generated or hard-coded).
   */
  private static class TypeMap {
    private final Map<List<String>, TypeData> map = new HashMap<List<String>, TypeData>();
    private Map<String, DomainGenerator> domainGeneratorMap = null;
    private List<TypeData.TypeRef> typesToGenerate = new ArrayList<TypeData.TypeRef>();

    void setDomainGeneratorMap(Map<String, DomainGenerator> domainGeneratorMap) {
      this.domainGeneratorMap = domainGeneratorMap;
    }

    String resolve(String domainName, String typeName,
        TypeData.Direction direction) {
      DomainGenerator domainGenerator = domainGeneratorMap.get(domainName);
      if (domainGenerator == null) {
        throw new RuntimeException();
      }
      return getTypeData(domainName, typeName).get(direction).resolve(this, domainGenerator);
    }

    void addTypeToGenerate(TypeData.TypeRef typeData) {
      typesToGenerate.add(typeData);
    }

    public void generateRequestedTypes() throws IOException {
      // Size may grow during iteration.
      for (int i = 0; i < typesToGenerate.size(); i++) {
        typesToGenerate.get(i).generate();
      }

      for (TypeData typeData : map.values()) {
        typeData.checkComplete();
      }
    }

    TypeData getTypeData(String domainName, String typeName) {
      List<String> key = createKey(domainName, typeName);
      TypeData result = map.get(key);
      if (result == null) {
        result = new TypeData(domainName, typeName);
        map.put(key, result);
      }
      return result;
    }

    private List<String> createKey(String domainName, String typeName) {
      return Arrays.asList(domainName, typeName);
    }
  }

  /**
   * Records a list of files in the root directory and deletes files that were not re-generated.
   */
  private static class FileSet {
    private final File rootDir;
    private final Set<File> unusedFiles;

    FileSet(File rootDir) {
      this.rootDir = rootDir;
      List<File> files = new ArrayList<File>();
      collectFilesRecursive(rootDir, files);
      unusedFiles = new HashSet<File>(files);
    }

    JavaFileUpdater createFileUpdater(String filePath) {
      File file = new File(rootDir, filePath);
      unusedFiles.remove(file);
      return new JavaFileUpdater(file);
    }

    void deleteOtherFiles() {
      for (File file : unusedFiles) {
        file.delete();
      }
    }

    private static void collectFilesRecursive(File file, Collection<File> list) {
      if (file.isFile()) {
        list.add(file);
      } else if (file.isDirectory()) {
        for (File inner : file.listFiles()) {
          collectFilesRecursive(inner, list);
        }
      }
    }
  }

  private static final String[] DOMAIN_WHITE_LIST = {
    "Debugger",
    "Runtime",
    "Page",
  };

  private static void initializeKnownTypes(TypeMap typeMap) {
    typeMap.getTypeData("Page", "Cookie").getInput().setJavaTypeName("Object");
  }

  private static final Set<String> BAD_METHOD_NAMES = new HashSet<String>(Arrays.asList(
      "this"
      ));
}
