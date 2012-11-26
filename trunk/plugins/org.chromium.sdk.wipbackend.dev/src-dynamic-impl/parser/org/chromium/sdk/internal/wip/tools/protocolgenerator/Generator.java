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

import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
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
  private static final String COMMON_PACKAGE = ROOT_PACKAGE + ".common";
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
    List<Domain> domainList = metamodel.domains();

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
          String dataFullName = Naming.COMMAND_DATA.getFullName(domain.domain(),
              command.name()).getFullText();
          jsonProtocolParserClassNames.add(dataFullName);
          parserRootInterfaceItems.add(
              new ParserRootInterfaceItem(domain.domain(), command.name(), Naming.COMMAND_DATA));
        }
      }

      if (domain.events() != null) {
        for (Event event : domain.events()) {
          generateEvenData(event);
          jsonProtocolParserClassNames.add(
              Naming.EVENT_DATA.getFullName(domain.domain(), event.name()).getFullText());
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
            Naming.COMMAND_DATA.getFullName(domain.domain(), command.name()).getFullText() + ">");
      } else {
        baseTypeBuilder.append("WipParams");
      }

      DeferredWriter additionalMemberBuilder = new DeferredWriter();
      additionalMemberBuilder.append("\t  public static final String METHOD_NAME = " +
          "org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain." +
          domain.domain().toUpperCase() + " + \"." + command.name() + "\";\n");
      additionalMemberBuilder.append("\n");


      additionalMemberBuilder.append("\t  @Override protected String getRequestName() {\n");
      additionalMemberBuilder.append("\t    return METHOD_NAME;\n");
      additionalMemberBuilder.append("\t  }\n");
      additionalMemberBuilder.append("\t\n");

      if (hasResponse) {
        String dataInterfaceFullname =
            Naming.COMMAND_DATA.getFullName(domain.domain(), command.name()).getFullText();
        additionalMemberBuilder.append(
            "\t  @Override public " + dataInterfaceFullname + " parseResponse(" +
            "org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, " +
            "org.chromium.sdk.internal.wip.protocol.input." +
            PARSER_ROOT_INTERFACE_NAME + " parser) " +
            "throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n");
        additionalMemberBuilder.append("\t    return parser." +
            Naming.COMMAND_DATA.getParseMethodName(domain.domain(), command.name()) +
            "(data.getUnderlyingObject());\n");
        additionalMemberBuilder.append("\t  }\n");
        additionalMemberBuilder.append("\t\n");
      }

      generateTopLevelOutputClass(Naming.PARAMS, command.name(), command.description(),
          baseTypeBuilder.toString(), additionalMemberBuilder, command.parameters(),
          PropertyLikeAccess.PARAMETER);
    }

    private void generateCommandAdditionalParam(StandaloneType type) throws IOException {
      generateTopLevelOutputClass(Naming.ADDITIONAL_PARAM, type.id(), type.description(),
          "org.json.simple.JSONObject", null, type.properties(), PropertyLikeAccess.PROPERTY);
    }

    private <P> void generateTopLevelOutputClass(ClassNameScheme nameScheme, String baseName,
        String description, String baseType, DeferredWriter additionalMemberText,
        List<P> properties, PropertyLikeAccess<P> propertyAccess) throws IOException {
      JavaFileUpdater fileUpdater = startJavaFile(nameScheme, domain, baseName);

      Writer writer = fileUpdater.getWriter();
      IndentWriter indentWriter = new IndentWriterImpl(writer, "");

      NamePath classNamePath = nameScheme.getFullName(domain.domain(), baseName);

      generateOutputClass(indentWriter, classNamePath, description, baseType, additionalMemberText,
          properties, propertyAccess);

      writer.close();

      fileUpdater.update();
    }

    private <P> void generateOutputClass(IndentWriter writer, NamePath classNamePath,
        String description, String baseType, DeferredWriter additionalMemberText,
        List<P> properties, PropertyLikeAccess<P> propertyAccess) throws IOException {
      if (description != null) {
        writer.append("\t/**\n" + description + "\n */\n");
      }
      writer.append("\tpublic class " + classNamePath.getLastComponent() +
          " extends " + baseType + " {\n");

      OutputClassScope classScope = new OutputClassScope(classNamePath);

      if (additionalMemberText != null) {
        classScope.addMember("param-specific", additionalMemberText);
      }

      classScope.generateCommandParamsBody(writer, properties, propertyAccess,
          classNamePath.getLastComponent());

      classScope.writeAdditionalMembers(writer);

      writer.append("\t}\n");
    }

    abstract class CreateStandalonTypeBindingVisitorBase implements
        TypeVisitor<StandaloneTypeBinding> {
      private final StandaloneType type;

      CreateStandalonTypeBindingVisitorBase(StandaloneType type) {
        this.type = type;
      }

      protected StandaloneType getType() {
        return type;
      }

      @Override public StandaloneTypeBinding visitString() {
        return createTypedefTypeBinding(type, StandaloneTypeBinding.PredefinedTarget.STRING,
            Naming.COMMON_TYPEDEF, null);
      }
      @Override public StandaloneTypeBinding visitInteger() {
        return createTypedefTypeBinding(type, StandaloneTypeBinding.PredefinedTarget.LONG,
            Naming.COMMON_TYPEDEF, null);
      }
      @Override public StandaloneTypeBinding visitRef(String refName) {
        throw new RuntimeException();
      }
      @Override public StandaloneTypeBinding visitBoolean() {
        throw new RuntimeException();
      }
      @Override public StandaloneTypeBinding visitNumber() {
        return createTypedefTypeBinding(type, StandaloneTypeBinding.PredefinedTarget.NUMBER,
            Naming.COMMON_TYPEDEF, null);
      }
      @Override public StandaloneTypeBinding visitUnknown() {
        throw new RuntimeException();
      }
    }

    StandaloneTypeBinding createStandaloneOutputTypeBinding(StandaloneType type,
        final String name) {
      return switchByType(type, TypedObjectAccess.FOR_STANDALONE,
          new CreateStandalonTypeBindingVisitorBase(type) {
        @Override
        public StandaloneTypeBinding visitObject(List<ObjectProperty> properties) {
          return new StandaloneTypeBinding() {
            @Override
            public BoxableType getJavaType() {
              return BoxableType.createReference(
                  Naming.ADDITIONAL_PARAM.getFullName(domain.domain(), name));
            }

            @Override public void generate() throws IOException {
              generateCommandAdditionalParam(getType());
            }

            @Override public TypeData.Direction getDirection() {
              return TypeData.Direction.OUTPUT;
            }
          };
        }

        @Override
        public StandaloneTypeBinding visitEnum(List<String> enumConstants) {
          throw new RuntimeException();
        }
        @Override public StandaloneTypeBinding visitArray(final ArrayItemType items) {

          StandaloneTypeBinding.Target target = new StandaloneTypeBinding.Target() {
            @Override
            public BoxableType resolve(final ResolveContext context) {
              ResolveAndGenerateScope resolveAndGenerateScope = new ResolveAndGenerateScope() {
                // This class is responsible for generating ad hoc type.
                // If we ever are to do it, we should generate into string buffer and put strings
                // inside TypeDef class.
                @Override public String getDomainName() {
                  return domain.domain();
                }
                @Override public TypeData.Direction getTypeDirection() {
                  return TypeData.Direction.OUTPUT;
                }
                @Override public BoxableType generateEnum(String description,
                    List<String> enumConstants) {
                  throw new UnsupportedOperationException();
                }

                @Override
                public <T> QualifiedTypeData resolveType(T typedObject,
                    Generator.TypedObjectAccess<T> access) {
                  throw new UnsupportedOperationException();
                }
                @Override
                public BoxableType generateNestedObject(String description,
                    List<ObjectProperty> properties) throws IOException {
                  return context.generateNestedObject("Item", description, properties);
                }
              };
              QualifiedTypeData itemTypeData =
                  resolveType(items, TypedObjectAccess.FOR_ARRAY_ITEM, resolveAndGenerateScope);
              BoxableType itemBoxableType = itemTypeData.getJavaType();

              final BoxableType arrayType = BoxableType.createList(itemBoxableType);

              return arrayType;
            }
          };

          return createTypedefTypeBinding(getType(), target,
              Naming.OUTPUT_TYPEDEF, TypeData.Direction.OUTPUT);
        }
      });
    }

    StandaloneTypeBinding createStandaloneInputTypeBinding(StandaloneType type) {
      return switchByType(type, TypedObjectAccess.FOR_STANDALONE,
          new CreateStandalonTypeBindingVisitorBase(type) {
        @Override
        public StandaloneTypeBinding visitObject(List<ObjectProperty> properties) {
          return createStandaloneObjectInputTypeBinding(getType(), properties);
        }

        @Override
        public StandaloneTypeBinding visitEnum(List<String> enumConstants) {
          return createStandaloneEnumInputTypeBinding(getType(), enumConstants,
              TypeData.Direction.INPUT);
        }

        @Override public StandaloneTypeBinding visitArray(ArrayItemType items) {
          ResolveAndGenerateScope resolveAndGenerateScope = new ResolveAndGenerateScope() {
            // This class is responsible for generating ad hoc type.
            // If we ever are to do it, we should generate into string buffer and put strings
            // inside TypeDef class.
            @Override public String getDomainName() {
              return domain.domain();
            }
            @Override public TypeData.Direction getTypeDirection() {
              return TypeData.Direction.INPUT;
            }
            @Override public BoxableType generateEnum(String description,
                List<String> enumConstants) {
              throw new UnsupportedOperationException();
            }

            @Override
            public <T> QualifiedTypeData resolveType(T typedObject,
                Generator.TypedObjectAccess<T> access) {
              throw new UnsupportedOperationException();
            }
            @Override
            public BoxableType generateNestedObject(String description,
                List<ObjectProperty> properties) throws IOException {
              throw new UnsupportedOperationException();
            }
          };
          QualifiedTypeData itemTypeData =
              resolveType(items, TypedObjectAccess.FOR_ARRAY_ITEM, resolveAndGenerateScope);
          BoxableType itemBoxableType = itemTypeData.getJavaType();

          final BoxableType arrayType = BoxableType.createList(itemBoxableType);

          StandaloneTypeBinding.Target target = new StandaloneTypeBinding.Target() {
            @Override
            public BoxableType resolve(ResolveContext context) {
              return arrayType;
            }
          };

          return createTypedefTypeBinding(getType(), target,
              Naming.INPUT_TYPEDEF, TypeData.Direction.INPUT);
        }
      });
    }

    StandaloneTypeBinding createStandaloneObjectInputTypeBinding(final StandaloneType type,
        final List<ObjectProperty> properties) {
      final String name = type.id();
      final NamePath fullTypeName = Naming.INPUT_VALUE.getFullName(domain.domain(), name);
      jsonProtocolParserClassNames.add(fullTypeName.getFullText());

      return new StandaloneTypeBinding() {
        @Override public BoxableType getJavaType() {
          return BoxableType.createReference(fullTypeName);
        }

        @Override
        public void generate() throws IOException {
          String description = type.description();

          NamePath className = Naming.INPUT_VALUE.getFullName(domain.domain(), name);
          JavaFileUpdater fileUpdater = startJavaFile(Naming.INPUT_VALUE, domain, name);

          IndentWriter writer = new IndentWriterImpl(fileUpdater.getWriter(), "");

          if (description != null) {
            writer.append("\t/**\n " + description + "\n */\n");
          }

          writer.append("\t@org.chromium.sdk.internal.protocolparser.JsonType");
          if (properties == null) {
            writer.append("(allowsOtherProperties=true)");
          }
          writer.append("\n");
          writer.append("\tpublic interface " + className.getLastComponent() +" {\n");

          InputClassScope classScope = new InputClassScope(className);

          classScope.generateStandaloneTypeBody(writer, properties);

          classScope.writeAdditionalMembers(writer);

          writer.append("\t}\n");

          fileUpdater.update();
        }

        @Override public TypeData.Direction getDirection() {
          return TypeData.Direction.INPUT;
        }
      };
    }

    StandaloneTypeBinding createStandaloneEnumInputTypeBinding(final StandaloneType type,
        final List<String> enumConstants, final TypeData.Direction direction) {
      final String name = type.id();
      return new StandaloneTypeBinding() {
        @Override public BoxableType getJavaType() {
          return BoxableType.createReference(Naming.INPUT_ENUM.getFullName(domain.domain(), name));
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
            writer.write(EnumValueCondition.decorateEnumConstantName(constName));
            first = false;
          }
          writer.write("\n");

          writer.write("}\n");

          fileUpdater.update();
        }

        @Override public TypeData.Direction getDirection() {
          return direction;
        }
      };
    }

    /**
     * Typedef is an empty class that just holds description and
     * refers to an actual type (such as String).
     */
    StandaloneTypeBinding createTypedefTypeBinding(final StandaloneType type,
        StandaloneTypeBinding.Target target,
        final ClassNameScheme nameScheme, final TypeData.Direction direction) {
      final String name = type.id();
      final NamePath typedefJavaName = nameScheme.getFullName(domain.domain(), name);
      final BoxableType typedefJavaType = BoxableType.createReference(typedefJavaName);

      final List<DeferredWriter> deferredWriters = new ArrayList<Generator.DeferredWriter>(0);

      class ResolveContextImpl implements StandaloneTypeBinding.Target.ResolveContext {
        @Override
        public BoxableType generateNestedObject(String shortName,
            String description, List<ObjectProperty> properties)
            throws IOException {

          DeferredWriter writer = new DeferredWriter();

          NamePath classNamePath = new NamePath(shortName, typedefJavaName);

          if (direction == null) {
            throw new RuntimeException("Unsupported");
          } else {
            switch (direction) {
            case INPUT:
              throw new RuntimeException("TODO");
            case OUTPUT:
              generateOutputClass(writer, classNamePath, description, "org.json.simple.JSONObject",
                  null, properties, PropertyLikeAccess.PROPERTY);
              break;
            default:
              throw new RuntimeException();
            }
          }

          deferredWriters.add(writer);

          return BoxableType.createReference(new NamePath(shortName, typedefJavaName));
        }
      }

      ResolveContextImpl resolveContext = new ResolveContextImpl();

      final BoxableType actualJavaType = target.resolve(resolveContext);

      return new StandaloneTypeBinding() {
        @Override public BoxableType getJavaType() {
          return new DecoratedBoxableType(actualJavaType);
        }

        class DecoratedBoxableType extends BoxableType {
          private final BoxableType original;

          DecoratedBoxableType(BoxableType original) {
            this.original = original;
          }
          @Override String getFullText() {
            return decorateTypeName(original.getFullText(), typedefJavaType.getFullText());
          }
          @Override String getShortText(NamePath contextNamespace) {
            return decorateTypeName(original.getShortText(contextNamespace),
                typedefJavaType.getShortText(contextNamespace));
          }
          @Override BoxableType convertToPureReference() {
            BoxableType pureReference = original.convertToPureReference();
            if (pureReference == original) {
              return this;
            } else {
              return new DecoratedBoxableType(pureReference);
            }
          }
          private String decorateTypeName(String actualTypeName, String innerTypeName) {
            return actualTypeName + "/*See " + innerTypeName + "*/";
          }
        }

        @Override
        public void generate() throws IOException {
          String description = type.description();

          String className = nameScheme.getShortName(name);
          JavaFileUpdater fileUpdater = startJavaFile(nameScheme, domain, name);

          NamePath contextNamespace = typedefJavaName;

          IndentWriter writer = new IndentWriterImpl(fileUpdater.getWriter(), "");

          if (description != null) {
            writer.append("\t/**\n " + description + "\n */\n");
          }

          writer.append("\tpublic class " + className + " {\n");
          writer.append("\t  /*\n   The class is 'typedef'.\n" +
              "\t   It merely holds a type javadoc and its only field refers to an " +
              "actual type.\n" +
              "\t   */\n");
          writer.append("\t  " + actualJavaType.getShortText(contextNamespace) + " actualType;\n");

          IndentWriter innerWriter = writer.createInner();
          for (DeferredWriter memberWriter : deferredWriters) {
            memberWriter.writeContent(innerWriter);
          }

          writer.append("\t}\n");
          fileUpdater.update();
        }

        @Override public TypeData.Direction getDirection() {
          return direction;
        }
      };
    }

    private void generateCommandData(Command command) throws IOException {
      String className = Naming.COMMAND_DATA.getShortName(command.name());
      JavaFileUpdater fileUpdater = startJavaFile(Naming.COMMAND_DATA, domain, command.name());

      IndentWriter writer = new IndentWriterImpl(fileUpdater.getWriter(), "");

      generateJsonProtocolInterface(writer, className, command.description(),
          command.returns(), null);

      fileUpdater.update();
    }

    private void generateEvenData(Event event) throws IOException {
      String className = Naming.EVENT_DATA.getShortName(event.name());
      JavaFileUpdater fileUpdater = startJavaFile(Naming.EVENT_DATA, domain, event.name());
      String domainName = domain.domain();
      String fullName = Naming.EVENT_DATA.getFullName(domainName, event.name()).getFullText();

      DeferredWriter eventTypeMemberText = new DeferredWriter();
      eventTypeMemberText.append(
          "\t  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<" +
          fullName +
          "> TYPE\n\t      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<" +
          fullName +
          ">(\"" + domainName + "." + event.name() + "\", " + fullName + ".class) {\n" +
          "\t    @Override public " + fullName + " parse(" + INPUT_PACKAGE + "." +
          PARSER_ROOT_INTERFACE_NAME + " parser, org.json.simple.JSONObject obj)" +
          " throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {\n" +
          "\t      return parser." +
          Naming.EVENT_DATA.getParseMethodName(domainName, event.name()) +
          "(obj);\n" +
          "\t    }\n" +
          "\t  };\n");

      IndentWriter writer = new IndentWriterImpl(fileUpdater.getWriter(), "");

      generateJsonProtocolInterface(writer, className, event.description(), event.parameters(),
          eventTypeMemberText);

      fileUpdater.update();
    }

    private void generateJsonProtocolInterface(IndentWriter writer, String className,
        String description, List<Parameter> parameters, DeferredWriter additionalMembersText)
        throws IOException {
      if (description != null) {
        writer.append("\t/**\n " + description + "\n */\n");
      }

      writer.append("\t@org.chromium.sdk.internal.protocolparser.JsonType");
      if (parameters == null) {
        writer.append("(allowsOtherProperties=true)");
      }
      writer.append("\n");
      writer.append("\tpublic interface " + className +" {\n");

      InputClassScope classScope = new InputClassScope(new NamePath(className,
          new NamePath(ClassNameScheme.Input.getPackageName(domain.domain()))));

      if (additionalMembersText != null) {
        classScope.addMember("extra", additionalMembersText);
      }

      classScope.generateMainJsonProtocolInterfaceBody(writer, parameters);

      classScope.writeAdditionalMembers(writer);

      writer.append("\t}\n");
    }

    private abstract class ClassScope {
      private final List<DeferredWriter> additionalMemberTexts = new ArrayList<DeferredWriter>(2);
      private final NamePath contextNamespace;

      ClassScope(NamePath classNamespace) {
        this.contextNamespace = classNamespace;
      }

      protected String getShortClassName() {
        return contextNamespace.getLastComponent();
      }

      String getFullName() {
        return contextNamespace.getFullText();
      }

      NamePath getClassContextNamespace() {
        return contextNamespace;
      }

      void addMember(String key, DeferredWriter deferredWriter) {
        additionalMemberTexts.add(deferredWriter);
      }

      void writeAdditionalMembers(IndentWriter writer) throws IOException {
        for (DeferredWriter deferredWriter : additionalMemberTexts) {
          deferredWriter.writeContent(writer);
        }
      }

      protected abstract MemberScope newMemberScope(String memberName);
      protected abstract TypeData.Direction getTypeDirection();

      /**
       * Member scope is used to generate additional types that are used only from method.
       * These types will be named after this method.
       */
      protected abstract class MemberScope implements ResolveAndGenerateScope {
        private final String memberName;

        protected MemberScope(String memberName) {
          this.memberName = memberName;
        }

        private QualifiedTypeData resolveType(ObjectProperty objectProperty) {
          return resolveType(objectProperty, TypedObjectAccess.FOR_OBJECT_PROPERTY);
        }

        private QualifiedTypeData resolveType(Parameter parameter) {
          return resolveType(parameter, TypedObjectAccess.FOR_PARAMETER);
        }

        @Override
        public <T> QualifiedTypeData resolveType(T typedObject, TypedObjectAccess<T> access) {
          return Generator.this.resolveType(typedObject, access, this);
        }

        protected String getMemberName() {
          return memberName;
        }

        public abstract BoxableType generateEnum(String description, List<String> enumConstants);
        public abstract BoxableType generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException;

        @Override
        public String getDomainName() {
          return domain.domain();
        }

        @Override
        public TypeData.Direction getTypeDirection() {
          return ClassScope.this.getTypeDirection();
        }
      }
    }

    class InputClassScope extends ClassScope {
      InputClassScope(NamePath namePath) {
        super(namePath);
      }

      public void generateMainJsonProtocolInterfaceBody(IndentWriter writer,
          List<Parameter> parameters) throws IOException {
        if (parameters != null) {
          for (Parameter param : parameters) {
            if (param.description() != null) {
              writer.append("\t  /**\n   " + param.description() + "\n   */\n");
            }

            String methodName = generateMethodNameSubstitute(param.name(), writer);

            ClassScope.MemberScope memberScope = newMemberScope(param.name());

            QualifiedTypeData paramTypeData = memberScope.resolveType(param);

            paramTypeData.writeAnnotations(writer, "  ");

            writer.append("\t  " +
                paramTypeData.getJavaType().getShortText(getClassContextNamespace()) + " " +
                methodName + "();\n");
            writer.append("\t\n");
          }
        }
      }

      void generateStandaloneTypeBody(IndentWriter writer, List<ObjectProperty> properties)
          throws IOException {
        if (properties != null) {
          for (ObjectProperty objectProperty : properties) {
            String propertyName = objectProperty.name();

            if (objectProperty.description() != null) {
              writer.append("\t  /**\n   " + objectProperty.description() + "\n   */\n");
            }

            String methodName = generateMethodNameSubstitute(propertyName, writer);

            ClassScope.MemberScope memberScope = newMemberScope(propertyName);

            QualifiedTypeData propertyTypeData = memberScope.resolveType(objectProperty);

            propertyTypeData.writeAnnotations(writer, "  ");

            writer.append("\t  " +
                propertyTypeData.getJavaType().getShortText(getClassContextNamespace()) + " " +
                methodName + "();\n");
            writer.append("\t\n");
          }
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
        public BoxableType generateEnum(String description, List<String> enumConstants) {
          DeferredWriter builder = new DeferredWriter();
          if (description != null) {
            builder.append("\t  /**\n   " + description + "\n   */\n");
          }

          String enumName = capitalizeFirstChar(getMemberName());
          builder.append("\t  public enum " + enumName + " {\n");
          for (String constant : enumConstants) {
            builder.append("\t    " +
                EnumValueCondition.decorateEnumConstantName(constant) + ",\n");
          }
          builder.append("\t  }\n");
          addMember(enumName, builder);

          return BoxableType.createReference(new NamePath(enumName, getClassContextNamespace()));
        }

        @Override
        public BoxableType generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException {
          DeferredWriter builder = new DeferredWriter();

          if (description != null) {
            builder.append("\t  /**\n   " + description + "\n   */\n");
          }

          String objectName = capitalizeFirstChar(getMemberName());

          if (propertyList == null) {
            builder.append("\t  @org.chromium.sdk.internal.protocolparser.JsonType(" +
                "allowsOtherProperties=true)\n");
            builder.append("\t  public interface " + objectName +
                " extends org.chromium.sdk.internal.protocolparser.JsonObjectBased {\n");
            builder.append("\t  }\n");
          } else {
            builder.append("\t  @org.chromium.sdk.internal.protocolparser.JsonType\n");
            builder.append("\t  public interface " + objectName + " {\n");
            for (ObjectProperty property : propertyList) {
              if (property.description() != null) {
                builder.append("\t    /**\n     " + property.description() + "\n     */\n");
              }

              String methodName = generateMethodNameSubstitute(property.name(), builder);

              MemberScope memberScope = newMemberScope(property.name());

              QualifiedTypeData propertyTypeData = memberScope.resolveType(property);

              propertyTypeData.writeAnnotations(builder, "    ");

              builder.append("\t    " +
                  propertyTypeData.getJavaType().getShortText(getClassContextNamespace()) + " " +
                  methodName +  "();\n");
              builder.append("\t\n");
            }
            builder.append("\t  }\n");
          }

          addMember(objectName, builder);

          jsonProtocolParserClassNames.add(getFullName() + "." + objectName);

          return BoxableType.createReference(new NamePath(objectName, getClassContextNamespace()));
        }
      }
    }

    class OutputClassScope extends ClassScope {
      OutputClassScope(NamePath classNamePath) {
        super(classNamePath);
      }

      <P> void generateCommandParamsBody(IndentWriter writer, List<P> parameters,
          PropertyLikeAccess<P> access, String commandName) throws IOException {

        if (parameters != null) {
          boolean hasDoc = false;
          for (P param : parameters) {
            if (access.forTypedObject().getDescription(param) != null) {
              hasDoc = true;
              break;
            }
          }
          if (hasDoc) {
            writer.append("\t  /**\n");
            for (P param : parameters) {
              String propertyDescription = access.forTypedObject().getDescription(param);
              if (propertyDescription != null) {
                writer.append("\t   @param " + getParamName(param, access) + " " +
                    propertyDescription + "\n");
              }
            }
            writer.append("\t   */\n");
          }
        }
        writer.append("\t  public " + getShortClassName() +"(");
        {
          boolean needComa = false;
          if (parameters != null) {
            for (P param : parameters) {
              if (needComa) {
                writer.append(", ");
              }
              String paramName = getParamName(param, access);
              ClassScope.MemberScope memberScope = newMemberScope(paramName);
              QualifiedTypeData paramTypeData =
                  memberScope.resolveType(param, access.forTypedObject());
              writer.append(paramTypeData.getJavaType().getShortText(getClassContextNamespace()) +
                  " " + paramName);
              needComa = true;
            }
          }
        }
        writer.append(") {\n");
        if (parameters != null) {
          for (P param : parameters) {
            boolean isOptional = access.forTypedObject().getOptional(param) == Boolean.TRUE;
            String paramName = getParamName(param, access);
            if (isOptional) {
              writer.append("\t    if (" + paramName + " != null) {\n  ");
            }
            writer.append("\t    this.put(\"" + access.getName(param) + "\", " + paramName +
                ");\n");
            if (isOptional) {
              writer.append("\t    }\n");
            }
          }
        }
        writer.append("\t  }\n");
        writer.append("\n");
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
        public BoxableType generateEnum(String description, List<String> enumConstants) {
          DeferredWriter builder = new DeferredWriter();
          if (description != null) {
            builder.append("\t  /**\n   " + description + "\n   */\n");
          }
          String enumName = capitalizeFirstChar(getMemberName());
          builder.append("\t  public enum " + enumName +
              " implements org.json.simple.JSONAware{\n");
          for (String constant : enumConstants) {
            builder.append("\t    " + EnumValueCondition.decorateEnumConstantName(constant) +
                "(\"" + constant + "\"),\n");
          }
          builder.append("\t    ;\n");
          builder.append("\t    private final String protocolValue;\n");
          builder.append("\t\n");
          builder.append("\t    " + enumName + "(String protocolValue) {\n");
          builder.append("\t      this.protocolValue = protocolValue;\n");
          builder.append("\t    }\n");
          builder.append("\t\n");
          builder.append("\t    @Override public String toJSONString() {\n");
          builder.append("\t      return '\"' + protocolValue + '\"';\n");
          builder.append("\t    }\n");
          builder.append("\t  }\n");
          addMember(enumName, builder);

          return BoxableType.createReference(new NamePath(enumName, getClassContextNamespace()));
        }

        @Override
        public BoxableType generateNestedObject(String description,
            List<ObjectProperty> propertyList) throws IOException {
          throw new UnsupportedOperationException();
        }
      }
    }
  }

  /**
   * An abstract code writer that supports indents. This approach is an attempt to build
   * purely data-driven writer. In other versions similar interfaces usually had
   * "new line" and "append" separate methods that have to be called in proper order.
   * <p>
   * Here "\t" symbol has a special treatment -- it denotes line start spaces in any
   * fragment. The writer simply replaces it with a necessary number of spaces.
   */
  private interface IndentWriter {
    void append(String text);

    IndentWriter createInner();

    String INDENT = "  ";
  }

  private static class DeferredWriter implements IndentWriter {
    private final String indent;
    private final StringBuilder builder;

    DeferredWriter() {
      this("", new StringBuilder());
    }

    DeferredWriter(String indent, StringBuilder builder) {
      this.indent = indent;
      this.builder = builder;
    }

    @Override
    public void append(String text) {
      text = text.replaceAll("\t", "\t" + indent);
      builder.append(text);
    }

    @Override
    public IndentWriter createInner() {
      return new DeferredWriter(indent + INDENT, builder);
    }

    void writeContent(IndentWriter output) {
      output.append(builder.toString());
    }
  }

  private static class IndentWriterImpl implements IndentWriter {
    private final String indent;
    private final Appendable output;

    IndentWriterImpl(Appendable output, String indent) {
      this.output = output;
      this.indent = indent;
    }

    @Override
    public void append(String text) {
      text = text.replaceAll("\t", indent);
      try {
        output.append(text);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public IndentWriter createInner() {
      return new IndentWriterImpl(output, indent + INDENT);
    }
  }

  private interface ResolveAndGenerateScope {
    String getDomainName();
    TypeData.Direction getTypeDirection();

    <T> QualifiedTypeData resolveType(T typedObject, TypedObjectAccess<T> access);

    BoxableType generateEnum(String description, List<String> enumConstants);
    BoxableType generateNestedObject(String description,
        List<ObjectProperty> properties) throws IOException;
  }

  <T> QualifiedTypeData resolveType(final T typedObject, final TypedObjectAccess<T> access,
      final ResolveAndGenerateScope scope) {
    UnqualifiedTypeData unqualifiedType =
        switchByType(typedObject, access, new TypeVisitor<UnqualifiedTypeData>() {
      @Override public UnqualifiedTypeData visitRef(String refName) {
        BoxableType typeRef = resolveRefType(scope.getDomainName(), refName,
            scope.getTypeDirection());
        return new UnqualifiedTypeData(typeRef);
      }
      @Override public UnqualifiedTypeData visitBoolean() {
        return UnqualifiedTypeData.BOOLEAN;
      }

      @Override public UnqualifiedTypeData visitEnum(List<String> enumConstants) {
        BoxableType enumName = scope.generateEnum(getDescription(), enumConstants);
        return new UnqualifiedTypeData(enumName);
      }

      @Override public UnqualifiedTypeData visitString() {
        return UnqualifiedTypeData.STRING;
      }
      @Override public UnqualifiedTypeData visitInteger() {
        return UnqualifiedTypeData.LONG;
      }
      @Override public UnqualifiedTypeData visitNumber() {
        return UnqualifiedTypeData.NUMBER;
      }
      @Override public UnqualifiedTypeData visitArray(ArrayItemType items) {
        QualifiedTypeData itemQualifiedType =
            scope.resolveType(items, TypedObjectAccess.FOR_ARRAY_ITEM);
        return new UnqualifiedTypeData(BoxableType.createList(itemQualifiedType.getJavaType()));
      }
      @Override public UnqualifiedTypeData visitObject(List<ObjectProperty> properties) {
        BoxableType nestedObjectName;
        try {
          nestedObjectName = scope.generateNestedObject(getDescription(), properties);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return new UnqualifiedTypeData(nestedObjectName, false);
      }
      @Override public UnqualifiedTypeData visitUnknown() {
        return UnqualifiedTypeData.ANY;
      }
      private <S> String getDescription() {
        return access.getDescription(typedObject);
      }
    });

    return unqualifiedType.getQualifiedType(access.getOptional(typedObject) == Boolean.TRUE);
  }

  private static class UnqualifiedTypeData {
    private final BoxableType typeRef;
    private final boolean nullable;

    UnqualifiedTypeData(BoxableType typeRef) {
      this(typeRef, false);
    }

    UnqualifiedTypeData(BoxableType typeRef, boolean nullable) {
      this.typeRef = typeRef;
      this.nullable = nullable;
    }

    QualifiedTypeData getQualifiedType(boolean optional) {
      BoxableType ref;
      if (optional) {
        ref = typeRef.convertToPureReference();
      } else {
        ref = typeRef;
      }
      return new QualifiedTypeData(ref, optional, nullable);
    }

    static final UnqualifiedTypeData BOOLEAN = new UnqualifiedTypeData(BoxableType.BOOLEAN, false);
    static final UnqualifiedTypeData STRING = new UnqualifiedTypeData(BoxableType.STRING, false);
    static final UnqualifiedTypeData LONG = new UnqualifiedTypeData(BoxableType.LONG, false);
    static final UnqualifiedTypeData NUMBER = new UnqualifiedTypeData(BoxableType.NUMBER, false);
    static final UnqualifiedTypeData ANY = new UnqualifiedTypeData(BoxableType.OBJECT, true);
  }

  private static class QualifiedTypeData {
    private final BoxableType typeRef;
    private final boolean optional;
    private final boolean nullable;

    QualifiedTypeData(BoxableType typeRef, boolean optional, boolean nullable) {
      this.typeRef = typeRef;
      this.optional = optional;
      this.nullable = nullable;
    }

    boolean isOptional() {
      return optional;
    }
    boolean isNullable() {
      return nullable;
    }
    BoxableType getJavaType() {
      return typeRef;
    }

    void writeAnnotations(IndentWriter appendable, String indent) throws IOException {
      if (isOptional()) {
        appendable.append("\t" + indent +
            "@org.chromium.sdk.internal.protocolparser.JsonOptionalField\n");
      }
      if (isNullable()) {
        appendable.append("\t" + indent +
            "@org.chromium.sdk.internal.protocolparser.JsonNullable\n");
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

  private static abstract class BoxableType {
    public static BoxableType create(String boxed, String unboxed) {
      return new Base(boxed, unboxed);
    }

    public static BoxableType createReference(NamePath namePath) {
      return new Reference(namePath);
    }

    public static BoxableType createList(BoxableType itemType) {
      return new ListType(itemType.convertToPureReference());
    }

    abstract String getFullText();

    abstract String getShortText(NamePath contextNamespace);

    abstract BoxableType convertToPureReference();

    private static class Base extends BoxableType {
      private final NamePath boxed;
      private final String unboxed;

      private Base(String boxed, String unboxed) {
        this.boxed = new NamePath(boxed);
        this.unboxed = unboxed;
      }

      @Override String getFullText() {
        return unboxed;
      }

      @Override String getShortText(NamePath contextNamespace) {
        return getFullText();
      }

      @Override BoxableType convertToPureReference() {
        return new Reference(boxed);
      }
    }

    private static class Reference extends BoxableType {
      private final NamePath namePath;

      private Reference(NamePath namePath) {
        this.namePath = namePath;
      }

      String getFullText() {
        return namePath.getFullText();
      }

      String getShortText(NamePath contextNamespace) {
        int nameLength = namePath.getLength();
        int contextLength = contextNamespace.getLength();
        if (nameLength > contextLength) {
          StringBuilder builder = subtractContextRecursively(namePath, nameLength - contextLength,
              contextNamespace);
          if (builder != null) {
            return builder.toString();
          }
        }
        return namePath.getFullText();
      }

      private StringBuilder subtractContextRecursively(NamePath namePos, int count,
          NamePath prefix) {
        if (count > 1) {
          StringBuilder result =
              subtractContextRecursively(namePos.getParent(), count - 1, prefix);
          if (result == null) {
            return null;
          }
          result.append('.');
          result.append(namePos.getLastComponent());
          return result;
        } else {
          String nameComponent = namePos.getLastComponent();
          namePos = namePos.getParent();
          do {
            if (!namePos.getLastComponent().equals(prefix.getLastComponent())) {
              return null;
            }
            namePos = namePos.getParent();
            prefix = prefix.getParent();
          } while (namePos != null);

          StringBuilder result = new StringBuilder();
          result.append(nameComponent);
          return result;
        }
      }

      BoxableType convertToPureReference() {
        return this;
      }
    }

    private static class ListType extends BoxableType {
      private final BoxableType itemType;

      public ListType(BoxableType itemType) {
        this.itemType = itemType;
      }

      @Override String getFullText() {
        return "java.util.List<" + itemType.getFullText() + ">";
      }

      @Override String getShortText(NamePath contextNamespace) {
        return "java.util.List<" + itemType.getShortText(contextNamespace) + ">";
      }

      @Override
      BoxableType convertToPureReference() {
        return this;
      }
    }

    static final BoxableType STRING = BoxableType.createReference(new NamePath("String"));
    static final BoxableType OBJECT = BoxableType.createReference(new NamePath("Object"));
    static final BoxableType NUMBER = BoxableType.createReference(new NamePath("Number"));
    static final BoxableType LONG = BoxableType.create("Long", "long");
    static final BoxableType BOOLEAN = BoxableType.create("Boolean", "boolean");
  }

  private static class NamePath {
    private final String lastComponent;
    private final NamePath parent;

    NamePath(String component) {
      this(component, null);
    }

    NamePath(String component, NamePath parent) {
      this.lastComponent = component;
      this.parent = parent;
    }

    NamePath getParent() {
      return parent;
    }

    String getLastComponent() {
      return lastComponent;
    }

    int getLength() {
      int res = 1;
      for (NamePath current = this; current != null; current = current.getParent()) {
        res++;
      }
      return res;
    }

    String getFullText() {
      StringBuilder result = new StringBuilder();
      fillFullPath(result);
      return result.toString();
    }

    private void fillFullPath(StringBuilder result) {
      if (parent != null) {
        parent.fillFullPath(result);
        result.append('.');
      }
      result.append(lastComponent);
    }
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
      fullName = nameScheme.getFullName(domain, name).getFullText();
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

    NamePath getFullName(String domainName, String baseName) {
      return new NamePath(getShortName(baseName), new NamePath(getPackageNameVirtual(domainName)));
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

    static class Common extends ClassNameScheme {
      Common(String suffix) {
        super(suffix);
      }

      @Override protected String getPackageNameVirtual(String domainName) {
        return getPackageName(domainName);
      }

      static String getPackageName(String domainName) {
        return COMMON_PACKAGE + "." + domainName.toLowerCase();
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

    ClassNameScheme COMMON_TYPEDEF = new ClassNameScheme.Common("Typedef");
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
        return obj.items();
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
  private BoxableType resolveRefType(String scopeDomainName, String refName,
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

  private String generateMethodNameSubstitute(String originalName, IndentWriter output)
      throws IOException {
    if (!BAD_METHOD_NAMES.contains(originalName)) {
      return originalName;
    }
    output.append("\t  @org.chromium.sdk.internal.protocolparser.JsonField(jsonLiteralName=\"" +
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
    private StandaloneTypeBinding commonBinding = null;

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
      private StandaloneTypeBinding oneDirectionBinding = null;

      BoxableType resolve(TypeMap typeMap, DomainGenerator domainGenerator) {
        if (commonBinding != null) {
          return commonBinding.getJavaType();
        }
        if (oneDirectionBinding != null) {
          return oneDirectionBinding.getJavaType();
        }
        StandaloneTypeBinding binding = resolveImpl(domainGenerator);
        if (binding.getDirection() == null) {
          commonBinding = binding;
        } else {
          oneDirectionBinding = binding;
        }
        typeMap.addTypeToGenerate(binding);
        return binding.getJavaType();
      }

      abstract StandaloneTypeBinding resolveImpl(DomainGenerator domainGenerator);

      void checkResolved() {
        if (type == null) {
          throw new RuntimeException();
        }
      }
    }

    class Output extends TypeRef {
      @Override
      StandaloneTypeBinding resolveImpl(final DomainGenerator domainGenerator) {
        if (type == null) {
          throw new RuntimeException();
        }
        return domainGenerator.createStandaloneOutputTypeBinding(type, name);
      }
    }

    class Input extends TypeRef {
      @Override
      StandaloneTypeBinding resolveImpl(DomainGenerator domainGenerator) {
        if (type == null) {
          throw new RuntimeException();
        }
        return domainGenerator.createStandaloneInputTypeBinding(type);
      }
    }
  }

  private interface StandaloneTypeBinding {
    BoxableType getJavaType();
    void generate() throws IOException;

    /** @return null if not direction-specific */
    TypeData.Direction getDirection();

    interface Target {
      BoxableType resolve(ResolveContext context);

      interface ResolveContext {
        BoxableType generateNestedObject(String shortName, String description,
            List<ObjectProperty> properties) throws IOException;
      }
    }

    class PredefinedTarget implements Target {
      private final BoxableType resolvedType;

      PredefinedTarget(BoxableType resolvedType) {
        this.resolvedType = resolvedType;
      }

      @Override public BoxableType resolve(ResolveContext context) {
        return resolvedType;
      }

      public static final PredefinedTarget STRING = new PredefinedTarget(BoxableType.STRING);
      public static final PredefinedTarget LONG = new PredefinedTarget(BoxableType.LONG);
      public static final PredefinedTarget NUMBER = new PredefinedTarget(BoxableType.NUMBER);
    }

  }


  /**
   * Keeps track of all referenced types.
   * A type may be used and resolved (generated or hard-coded).
   */
  private static class TypeMap {
    private final Map<List<String>, TypeData> map = new HashMap<List<String>, TypeData>();
    private Map<String, DomainGenerator> domainGeneratorMap = null;
    private List<StandaloneTypeBinding> typesToGenerate = new ArrayList<StandaloneTypeBinding>();

    void setDomainGeneratorMap(Map<String, DomainGenerator> domainGeneratorMap) {
      this.domainGeneratorMap = domainGeneratorMap;
    }

    BoxableType resolve(String domainName, String typeName,
        TypeData.Direction direction) {
      DomainGenerator domainGenerator = domainGeneratorMap.get(domainName);
      if (domainGenerator == null) {
        throw new RuntimeException("Failed to find domain generator: " + domainName);
      }
      return getTypeData(domainName, typeName).get(direction).resolve(this, domainGenerator);
    }

    void addTypeToGenerate(StandaloneTypeBinding binding) {
      typesToGenerate.add(binding);
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
    "Network",
    "Console",
    "DOM",
  };

  private static void initializeKnownTypes(TypeMap typeMap) {
    // Code example:
    // typeMap.getTypeData("Page", "Cookie").getInput().setJavaTypeName("Object");
  }

  private static final Set<String> BAD_METHOD_NAMES = new HashSet<String>(Arrays.asList(
      "this"
      ));
}
