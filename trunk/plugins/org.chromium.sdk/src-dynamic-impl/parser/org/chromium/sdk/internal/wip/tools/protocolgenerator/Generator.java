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

  private final List<String> jsonProtocolParserClassNames = new ArrayList<String>();
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
          jsonProtocolParserClassNames.add(
              Naming.COMMAND_DATA.getFullName(domain.domain(), command.name()));
        }
      }

      if (domain.events() != null) {
        for (Event event : domain.events()) {
          generateEvenData(event);
          jsonProtocolParserClassNames.add(
              Naming.EVENT_DATA.getFullName(domain.domain(), event.name()));
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
            "org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) " +
            "throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n");
        additionalMemberBuilder.append("    return parser.parse(data.getUnderlyingObject(), " +
            dataInterfaceFullname + ".class);\n");
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

    void generateStandaloneInputType(StandaloneType type) throws IOException {
      if (!WipMetamodel.OBJECT_TYPE.equals(type.type())) {
        throw new RuntimeException();
      }
      List<ObjectProperty> properties = type.properties();
      if (properties == null) {
        throw new RuntimeException();
      }
      String name = type.id();
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

      String fullTypeName = Naming.INPUT_VALUE.getFullName(domain.domain(), name);
      jsonProtocolParserClassNames.add(fullTypeName);
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
          ">(\"" + domainName + "." + event.name() + "\", " + fullName + ".class);\n";

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

        private String getTypeName(ObjectProperty objectProperty) throws IOException {
          return getTypeName(objectProperty, TypedObjectAccess.FOR_OBJECT_PROPERTY);
        }

        private String getTypeName(Parameter parameter) throws IOException {
          return getTypeName(parameter, TypedObjectAccess.FOR_PARAMETER);
        }

        private String getTypeName(ArrayItemType arrayItemType) throws IOException {
          return getTypeName(arrayItemType, TypedObjectAccess.FOR_ARRAY_ITEM);
        }

        <T> String getTypeName(T typedObject, TypedObjectAccess<T> access) throws IOException {
          boolean isOptional = access.getOptional(typedObject) == Boolean.TRUE;
          String refName = access.getRef(typedObject);
          if (refName != null) {
            return resolveRefType(domain.domain(), refName, getTypeDirection());
          }
          String typeName = access.getType(typedObject);
          if (WipMetamodel.BOOLEAN_TYPE.equals(typeName)) {
            return isOptional ? "Boolean" : "boolean";
          } else if (WipMetamodel.STRING_TYPE.equals(typeName)) {
            if (access.getEnum(typedObject) != null) {
              return generateEnum(access.getDescription(typedObject), access.getEnum(typedObject));
            }
            return "String";
          } else if (WipMetamodel.INTEGER_TYPE.equals(typeName)) {
            return isOptional ? "Long" : "long";
          } else if (WipMetamodel.NUMBER_TYPE.equals(typeName)) {
            return "Number";
          } else if (WipMetamodel.ARRAY_TYPE.equals(typeName)) {
            return "java.util.List<" + getTypeName(access.getItems(typedObject)) + ">";
          } else if (WipMetamodel.OBJECT_TYPE.equals(typeName)) {
            return generateNestedObject(access.getDescription(typedObject),
                access.getProperties(typedObject));
          } else if (WipMetamodel.UNKNOWN_TYPE.equals(typeName)) {
            return "Object";
          }
          throw new RuntimeException("Unrecognized type " + typeName);
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

            if (param.optional() == Boolean.TRUE) {
              getWriter().write("  @org.chromium.sdk.internal.protocolparser.JsonOptionalField\n");
            }

            ClassScope.MemberScope memberScope = newMemberScope(param.name());

            getWriter().write("  " + memberScope.getTypeName(param) + " " + methodName + "();\n");
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

          if (objectProperty.optional() == Boolean.TRUE) {
            getWriter().write("  @org.chromium.sdk.internal.protocolparser.JsonOptionalField\n");
          }

          ClassScope.MemberScope memberScope = newMemberScope(propertyName);
          getWriter().write("  " + memberScope.getTypeName(objectProperty) + " " +
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
          builder.append("  @org.chromium.sdk.internal.protocolparser.JsonType\n");
          builder.append("  public interface " + objectName + " {\n");

          for (ObjectProperty property : propertyList) {
            if (property.description() != null) {
              builder.append("    /**\n     " + property.description() + "\n     */\n");
            }

            String methodName = generateMethodNameSubstitute(property.name(), builder);

            if (property.optional() == Boolean.TRUE) {
              builder.append("    @org.chromium.sdk.internal.protocolparser.JsonOptionalField\n");
            }

            MemberScope memberScope = newMemberScope(property.name());
            builder.append("    " + memberScope.getTypeName(property) + " " + methodName +
                "();\n");
            builder.append("\n");
          }

          builder.append("  }\n");
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
              getWriter().write(memberScope.getTypeName(param, access.forTypedObject()) +
                  " " + paramName);
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
              getWriter().write("    if (" + paramName + " == null) {\n  ");
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
          builder.append("  public enum " + enumName + " {\n");
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
          builder.append("    @Override public String toString() {\n");
          builder.append("      return protocolValue;\n");
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

  private void generateParserInterfaceList() throws IOException {
    JavaFileUpdater fileUpdater =
        startJavaFile(INPUT_PACKAGE, PARSER_INTERFACE_LIST_CLASS_NAME + ".java");

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

    ClassNameScheme COMMAND_DATA = new ClassNameScheme.Input("Data");
    ClassNameScheme EVENT_DATA = new ClassNameScheme.Input("EventData");
    ClassNameScheme INPUT_VALUE = new ClassNameScheme.Input("Value");
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
        throw new RuntimeException();
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

      String resolve(TypeMap typeMap) {
        if (!requested) {
          typeMap.addTypeToGenerate(this);
          requested = true;
        }
        return resolveImpl();
      }

      abstract String resolveImpl();

      String getDomainName() {
        return domain;
      }


      void generate(DomainGenerator domainGenerator) throws IOException {
        if (!generated) {
          if (type != null) {
            generateImpl(domainGenerator);
          }
          generated = true;
        }
      }

      abstract void generateImpl(DomainGenerator domainGenerator) throws IOException;
    }

    class Output extends TypeRef {
      void checkResolved() {
        if (type == null) {
          throw new RuntimeException();
        }
      }

      @Override
      String resolveImpl() {
        if (type == null) {
          throw new RuntimeException();
        }
        return Naming.ADDITIONAL_PARAM.getFullName(domain, name);
      }

      @Override
      void generateImpl(DomainGenerator domainGenerator) throws IOException {
        domainGenerator.generateCommandAdditionalParam(type);
      }
    }

    class Input extends TypeRef {
      private String predefinedJavaTypeName = null;

      @Override
      String resolveImpl() {
        if (predefinedJavaTypeName == null) {
          if (type == null) {
            throw new RuntimeException();
          }
          return Naming.INPUT_VALUE.getFullName(domain, name);
        } else {
          return predefinedJavaTypeName;
        }
      }

      void checkResolved() {
        if (type != null && predefinedJavaTypeName != null) {
          throw new RuntimeException();
        }
      }

      void setJavaTypeName(String javaTypeName) {
        if (this.predefinedJavaTypeName != null) {
          throw new RuntimeException("Type already initialized");
        }
        this.predefinedJavaTypeName = javaTypeName;
      }

      @Override
      void generateImpl(DomainGenerator domainGenerator) throws IOException {
        if (type != null) {
          domainGenerator.generateStandaloneInputType(type);
        }
      }
    }
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
      return getTypeData(domainName, typeName).get(direction).resolve(this);
    }

    void addTypeToGenerate(TypeData.TypeRef typeData) {
      typesToGenerate.add(typeData);
    }

    public void generateRequestedTypes() throws IOException {
      // Size may grow during iteration.
      for (int i = 0; i < typesToGenerate.size(); i++) {
        TypeData.TypeRef typeRef = typesToGenerate.get(i);
        DomainGenerator domainGenerator = domainGeneratorMap.get(typeRef.getDomainName());
        if (domainGenerator == null) {
          throw new RuntimeException();
        }
        typeRef.generate(domainGenerator);
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
