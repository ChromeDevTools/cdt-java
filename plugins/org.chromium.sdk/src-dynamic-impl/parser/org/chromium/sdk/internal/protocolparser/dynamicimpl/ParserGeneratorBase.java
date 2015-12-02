// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.dynamicimpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all generator main classes.
 */
public class ParserGeneratorBase {

  protected static void mainImpl(String[] args, GenerateConfiguration configuration) {
    Params params = parseArgs(args);

    StringBuilder stringBuilder = new StringBuilder();
    generateImpl(configuration, stringBuilder);

    String path = configuration.getPackageName().replace('.', '/');

    File directory = new File(params.outputDirectory() + "/" + path);
    directory.mkdirs();

    File output = new File(directory, configuration.getClassName() + ".java");
    try {
      Writer writer = new OutputStreamWriter(new FileOutputStream(output));

      writer.append(stringBuilder);
      writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static class GenerateConfiguration {
    private final String packageName;
    private final String className;
    private final DynamicParserImpl parserImpl;
    private final Collection<GeneratedCodeMap> basePackagesMap;

    public GenerateConfiguration(String packageName, String className,
        DynamicParserImpl parserImpl) {
      this(packageName, className, parserImpl, Collections.<GeneratedCodeMap>emptyList());
    }

    public GenerateConfiguration(String packageName, String className,
        DynamicParserImpl parserImpl, Collection<GeneratedCodeMap> basePackagesMap) {
      this.packageName = packageName;
      this.className = className;
      this.parserImpl = parserImpl;
      this.basePackagesMap = basePackagesMap;
    }

    public String getPackageName() {
      return packageName;
    }

    public String getClassName() {
      return className;
    }

    public DynamicParserImpl getParserImpl() {
      return parserImpl;
    }

    public Collection<GeneratedCodeMap> getBasePackagesMap() {
      return basePackagesMap;
    }
  }

  private interface Params {
    String outputDirectory();
  }

  private static Params parseArgs(String[] args) {
    final StringParam outputDirParam = new StringParam();

    Map<String, StringParam> paramMap = new HashMap<String, StringParam>(3);
    paramMap.put("output-dir", outputDirParam);

    for (String arg : args) {
      if (!arg.startsWith("--")) {
        throw new IllegalArgumentException("Unrecognized param: " + arg);
      }
      int equalsPos = arg.indexOf('=', 2);
      String key;
      String value;
      if (equalsPos == -1) {
        key = arg.substring(2).trim();
        value = null;
      } else {
        key = arg.substring(2, equalsPos).trim();
        value = arg.substring(equalsPos + 1).trim();
      }
      ParamListener paramListener = paramMap.get(key);
      if (paramListener == null) {
        throw new IllegalArgumentException("Unrecognized param name: " + key);
      }
      try {
        paramListener.setValue(value);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Failed to set value of " + key, e);
      }
    }
    for (Map.Entry<String, StringParam> en : paramMap.entrySet()) {
      if (en.getValue().getValue() == null) {
        throw new IllegalArgumentException("Parameter " + en.getKey() + " should be set");
      }
    }

    return new Params() {
      @Override
      public String outputDirectory() {
        return outputDirParam.getValue();
      }
    };
  }

  private interface ParamListener {
    void setValue(String value);
  }

  private static class StringParam implements ParamListener {
    private String value = null;

    @Override
    public void setValue(String value) {
      if (value == null) {
        throw new IllegalArgumentException("Argument with value expected");
      }
      if (this.value != null) {
        throw new IllegalArgumentException("Argument value already set");
      }
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  protected static GeneratedCodeMap buildParserMap(GenerateConfiguration configuration) {
    return generateImpl(configuration, new StringBuilder());
  }

  private static GeneratedCodeMap generateImpl(GenerateConfiguration configuration,
      StringBuilder stringBuilder) {
    return configuration.getParserImpl().generateStaticParser(stringBuilder,
        configuration.getPackageName(), configuration.getClassName(),
        configuration.getBasePackagesMap());
  }
}
