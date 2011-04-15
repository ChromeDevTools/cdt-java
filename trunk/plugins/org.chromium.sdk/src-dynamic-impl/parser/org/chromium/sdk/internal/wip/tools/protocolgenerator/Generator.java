// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
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

  private final String outputDir;
  private final List<String> jsonProtocolParserClassNames = new ArrayList<String>();
  private final TypeMap typeMap = new TypeMap();

  Generator(String outputDir) {
    this.outputDir = outputDir;
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

    for (Domain domain : domainList) {
      boolean found = domainTodoList.remove(domain.domain());
      if (!found) {
        System.out.println("Domain skipped: " + domain.domain());
        continue;
      }

      new DomainGenerator(domain).go();
    }

    generateParserInterfaceList();

    if (!domainTodoList.isEmpty()) {
      throw new RuntimeException("Domains expected but not found: " + domainTodoList);
    }

    typeMap.checkTypesResolved();
  }

  private class DomainGenerator {
    private final Domain domain;

    DomainGenerator(Domain domain) {
      this.domain = domain;
    }

    void go() throws IOException {
      if (domain.types() != null) {
        for (StandaloneType type : domain.types()) {
          generateStandaloneType(type);
        }
      }

      for (Command command : domain.commands()) {
        boolean hasResponse = command.returns() != null;
        generateCommandParams(command, hasResponse);
        if (hasResponse) {
          generateCommandData(command);
          jsonProtocolParserClassNames.add(
              getDataInterfaceFullName(domain.domain(), command.name()));
        }
      }

      if (domain.events() != null) {
        for (Event event : domain.events()) {
          generateEvenData(event);
          jsonProtocolParserClassNames.add(
              getEventInterfaceFullName(domain.domain(), event.name()));
        }
      }
    }

    private void generateCommandParams(Command command, boolean hasResponse) throws IOException {
      String className = getClassName(command.name()) + "Params";
      Writer writer = startJavaFile(
          getDomainOutputPackageName(domain.domain()), className + ".java");

      if (command.description() != null) {
        writer.write("/**\n" + command.description() + "\n */\n");
      }
      writer.write("public class " + className +
          " extends org.chromium.sdk.internal.wip.protocol.output.");
      if (hasResponse) {
        writer.write("WipParamsWithResponse<" +
            getDataInterfaceFullName(domain.domain(), command.name()) + ">");
      } else {
        writer.write("WipParams");
      }
      writer.write(" {\n");
      writer.write("  public static final String METHOD_NAME = " +
          "org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain." +
          domain.domain().toUpperCase() + " + \"." + command.name() + "\";\n");
      writer.write("\n");

      if (command.parameters() != null) {
        boolean hasDoc = false;
        for (Parameter param : command.parameters()) {
          if (param.description() != null) {
            hasDoc = true;
            break;
          }
        }
        if (hasDoc) {
          writer.write("  /**\n");
          for (Parameter param : command.parameters()) {
            if (param.description() != null) {
              writer.write("   @param " + getParamName(param) + " " + param.description() + "\n");
            }
          }
          writer.write("   */\n");
        }
      }

      OutputClassScope classScope = new OutputClassScope(writer, className);

      classScope.generateCommandParamsBody(command.parameters(), hasResponse, command.name());

      classScope.writeAdditionalMembers(writer);

      writer.write("}\n");

      writer.close();
    }

    private void generateStandaloneType(StandaloneType type) throws IOException {
      if (!WipMetamodel.OBJECT_TYPE.equals(type.type())) {
        throw new RuntimeException();
      }
      List<ObjectProperty> properties = type.properties();
      if (properties == null) {
        throw new RuntimeException();
      }
      String name = type.id();
      String description = type.description();

      String className = getObjectInterfaceShortName(name);
      String domainName = domain.domain();
      Writer writer = startJavaFile(getDomainInputPackageName(domainName), className + ".java");

      if (description != null) {
        writer.write("/**\n " + description + "\n */\n");
      }

      writer.write("@org.chromium.sdk.internal.protocolparser.JsonType\n");
      writer.write("public interface " + className +" {\n");

      InputClassScope classScope = new InputClassScope(writer, className);

      classScope.generateStandaloneTypeBody(properties);

      classScope.writeAdditionalMembers(writer);

      writer.write("}\n");

      writer.close();

      String fullTypeName = getObjectInterfaceFullName(domainName, name);
      jsonProtocolParserClassNames.add(fullTypeName);

      typeMap.getTypeData(domain.domain(), type.id()).setJavaTypeName(
          getObjectInterfaceFullName(domain.domain(), type.id()));
    }

    private void generateCommandData(Command command) throws IOException {
      String className = getDataInterfaceShortName(command.name());
      Writer writer = startJavaFile(
          getDomainInputPackageName(domain.domain()), className + ".java");

      generateJsonProtocolInterface(writer, className, command.description(),
          command.returns(), null);

      writer.close();
    }

    private void generateEvenData(Event event) throws IOException {
      String className = getEventInterfaceShortName(event.name());
      String domainName = domain.domain();
      Writer writer = startJavaFile(getDomainInputPackageName(domainName), className + ".java");

      String fullName = getEventInterfaceFullName(domainName, event.name());
      String eventTypeMemberText =
          "  public static final org.chromium.sdk.internal.wip.WipEventType<" + fullName +
          "> TYPE\n    = new org.chromium.sdk.internal.wip.WipEventType<" + fullName +
          ">(\"" + domainName + "." + event.name() + "\", " + fullName + ".class);\n";

      generateJsonProtocolInterface(writer, className, event.description(), event.parameters(),
          eventTypeMemberText);

      writer.close();
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
            return resolveRefType(domain.domain(), refName);
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
        super(writer, getDomainInputPackageName(domain.domain()), shortClassName);
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
        super(writer, getDomainOutputPackageName(domain.domain()), shortClassName);
      }

      void generateCommandParamsBody(List<Parameter> parameters, boolean hasResponse,
          String commandName) throws IOException {
        getWriter().write("  public " + getShortClassName() +"(");
        {
          boolean needComa = false;
          if (parameters != null) {
            for (Parameter param : parameters) {
              if (needComa) {
                getWriter().write(", ");
              }
              String paramName = getParamName(param);
              ClassScope.MemberScope memberScope = newMemberScope(paramName);
              getWriter().write(memberScope.getTypeName(param) + " " + paramName);
              needComa = true;
            }
          }
        }
        getWriter().write(") {\n");
        if (parameters != null) {
          for (Parameter param : parameters) {
            getWriter().write("    this.put(\"" + param.name() + "\", " + getParamName(param) +
                ");\n");
          }
        }
        getWriter().write("  }\n");
        getWriter().write("\n");

        getWriter().write("  @Override protected String getRequestName() {\n");
        getWriter().write("    return METHOD_NAME;\n");
        getWriter().write("  }\n");
        getWriter().write("\n");

        if (hasResponse) {
          String dataInterfaceFullname = getDataInterfaceFullName(domain.domain(), commandName);
          getWriter().write("  @Override public " + dataInterfaceFullname + " parseResponse(" +
              "org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, " +
              "org.chromium.sdk.internal.protocolparser.JsonProtocolParser parser) " +
              "throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n");
          getWriter().write("    return parser.parse(data.getUnderlyingObject(), " +
              dataInterfaceFullname + ".class);\n");
          getWriter().write("  }\n");
          getWriter().write("\n");
        }
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
    Writer writer = startJavaFile(INPUT_PACKAGE, PARSER_INTERFACE_LIST_CLASS_NAME + ".java");

    writer.write("public class " + PARSER_INTERFACE_LIST_CLASS_NAME + " {\n");
    writer.write("  public static final Class<?>[] LIST = {\n");
    for (String name : jsonProtocolParserClassNames) {
      writer.write("    " + name + ".class" + ",\n");
    }
    writer.write("  };\n");
    writer.write("}\n");

    writer.close();
  }

  private static String getDataInterfaceFullName(String domainName, String commandName) {
    return getDomainInputPackageName(domainName) + "." + getDataInterfaceShortName(commandName);
  }

  private static String getDataInterfaceShortName(String commandName) {
    return getClassName(commandName) + "Data";
  }

  private static String getEventInterfaceFullName(String domainName, String eventName) {
    return getDomainInputPackageName(domainName) + "." + getEventInterfaceShortName(eventName);
  }

  private static String getEventInterfaceShortName(String eventName) {
    return getClassName(eventName) + "EventData";
  }

  private static String getObjectInterfaceFullName(String domainName, String typeName) {
    return getDomainInputPackageName(domainName) + "." + getObjectInterfaceShortName(typeName);
  }

  private static String getObjectInterfaceShortName(String typeName) {
    return getClassName(typeName) + "Value";
  }

  private static String getParamName(Parameter param) {
    String paramName = param.name();
    if (param.optional() == Boolean.TRUE) {
      paramName = paramName + "Opt";
    }
    return paramName;
  }

  private static String getClassName(String commandOrEventName) {
    return capitalizeFirstChar(commandOrEventName);
  }

  private static String getDomainInputPackageName(String domainName) {
    return INPUT_PACKAGE + "." + domainName.toLowerCase();
  }

  private static String getDomainOutputPackageName(String domainName) {
    return OUTPUT_PACKAGE + "." + domainName.toLowerCase();
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
   * Resolve absolute (DOMAIN.TYPE) or relative (TYPE) typename.
   */
  private String resolveRefType(String scopeDomainName, String refName) {
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
    String javaTypeName = typeMap.getTypeData(domainName, shortName).getJavaTypeName();
    if (javaTypeName == null) {
      javaTypeName = getObjectInterfaceFullName(domainName, shortName);
    }
    return javaTypeName;
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

  private Writer startJavaFile(String packageName, String filename) throws IOException {
    String filePath = packageName.replace('.', '/');
    File dir = new File(outputDir, filePath);
    boolean dirCreated = dir.mkdirs();
    if (!dirCreated && !dir.isDirectory()) {
      throw new RuntimeException("Failed to create directory " + dir.getPath());
    }
    File file = new File(dir, filename);
    FileOutputStream fileOutputStream = new FileOutputStream(file);
    Writer writer = new OutputStreamWriter(fileOutputStream, "UTF-8");
    writer.write("// Generated source.\n// See " + this.getClass().getCanonicalName() + ".\n\n");
    writer.write("package " + packageName + ";\n\n");
    return writer;
  }

  private static class TypeData {
    private final String domain;
    private final String name;
    private String javaTypeName = null;

    TypeData(String domain, String name) {
      this.domain = domain;
      this.name = name;
    }

    String getJavaTypeName() {
      return javaTypeName;
    }

    void setJavaTypeName(String javaTypeName) {
      if (this.javaTypeName != null) {
        throw new RuntimeException("Type already initialized");
      }
      this.javaTypeName = javaTypeName;
    }

    void checkResolved() {
      if (javaTypeName == null) {
        throw new RuntimeException("Type not resolved " + domain + "." + name);
      }
    }
  }

  /**
   * Keeps track of all referenced types.
   * A type may be used and resolved (generated or hard-coded).
   */
  private static class TypeMap {
    private final Map<List<String>, TypeData> map = new HashMap<List<String>, TypeData>();

    void setTypeResolved(String domainName, String typeName, String javaTypeName) {
      getTypeData(domainName, typeName).setJavaTypeName(javaTypeName);
    }

    void checkTypesResolved() {
      for (TypeData data : map.values()) {
        data.checkResolved();
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

  private static final String[] DOMAIN_WHITE_LIST = {
    "Debugger",
    "Runtime",
    "Page",
  };

  private static void initializeKnownTypes(TypeMap typeMap) {
    typeMap.setTypeResolved("Runtime", "RuntimeProperty",
        "org.chromium.sdk.internal.wip.protocol.input.GetPropertiesData2.Property");
    typeMap.setTypeResolved("Page", "Cookie", "Object");
  }

  private static final Set<String> BAD_METHOD_NAMES = new HashSet<String>(Arrays.asList(
      "this"
      ));
}
