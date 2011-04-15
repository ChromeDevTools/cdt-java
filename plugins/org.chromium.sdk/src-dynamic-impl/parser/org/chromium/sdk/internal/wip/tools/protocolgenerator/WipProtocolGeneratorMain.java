// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParser;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Application that generates WIP interfaces, both input and output according to
 * specification available at
 * "http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json".
 */
public class WipProtocolGeneratorMain {
  public static void main(String[] args) {
    Params params = parseParams(args);

    String modelJsonText;
    try {
      modelJsonText = loadJsonModelText();
    } catch (IOException e) {
      throw new RuntimeException("Failed to load JSON", e);
    }

    Object jsonValue;
    try {
      jsonValue = new JSONParser().parse(modelJsonText);
    } catch (ParseException e) {
      throw new RuntimeException("Failed to parse json", e);
    }

    JsonProtocolParser metaModelParser = WipMetamodelParser.get();

    WipMetamodel.Root metamodel;
    try {
      metamodel = metaModelParser.parseAnything(jsonValue, WipMetamodel.Root.class);
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException("Failed to parse metamodel", e);
    }

    Generator generator = new Generator(params.getOutputDir());
    try {
      generator.go(metamodel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String loadJsonModelText() throws IOException {
    URL resourceUrl = new URL(JSON_MODEL_FILE_URL);
    StringBuilder result = new StringBuilder();

    InputStream stream = resourceUrl.openStream();

    Reader reader = new InputStreamReader(stream, "UTF-8");

    char[] buffer = new char[1024];
    while (true) {
      int res = reader.read(buffer);
      if (res == -1) {
        break;
      }
      result.append(buffer, 0, res);
    }
    reader.close();
    stream.close();
    return result.toString();
  }

  private interface Params {
    String getOutputDir();
  }

  private static Params parseParams(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("One argument <dest dir> expected");
    }
    final String outputDir = args[0];
    return new Params() {
      @Override
      public String getOutputDir() {
        return outputDir;
      }
    };

  }

  private static final String JSON_MODEL_FILE_URL =
      "http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json";
}
