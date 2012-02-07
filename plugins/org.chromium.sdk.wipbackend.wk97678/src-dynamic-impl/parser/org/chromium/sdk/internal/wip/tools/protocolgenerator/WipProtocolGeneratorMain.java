// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
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

    JsonModelData jsonModelData;
    try {
      jsonModelData = loadJsonModelText(JSON_MODEL_FILE_URL);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load JSON", e);
    }

    Object jsonValue;
    try {
      jsonValue = new JSONParser().parse(jsonModelData.getJsonText());
    } catch (ParseException e) {
      throw new RuntimeException("Failed to parse json", e);
    }

    WipMetamodelParser metaModelParser = WipMetamodelParser.Impl.get();

    WipMetamodel.Root metamodel;
    try {
      metamodel = metaModelParser.parseRoot(jsonValue);
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException("Failed to parse metamodel", e);
    }

    Generator generator = new Generator(params.getOutputDir(), jsonModelData.getOriginReference());
    try {
      generator.go(metamodel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private interface JsonModelData {
    String getJsonText();
    String getOriginReference();
  }

  private interface ModelResourceLocation {
    Connected connect() throws IOException;

    interface Connected {
      String getResourceDescription();
      InputStream getContent() throws IOException;
    }
  }

  private static JsonModelData loadJsonModelText(ModelResourceLocation location)
      throws IOException {
    StringBuilder result = new StringBuilder();

    ModelResourceLocation.Connected connection = location.connect();

    final String originReference = connection.getResourceDescription();

    InputStream stream = connection.getContent();

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
    final String resultString = result.toString();

    return new JsonModelData() {
      @Override public String getOriginReference() {
        return originReference;
      }

      @Override public String getJsonText() {
        return resultString;
      }
    };
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

  private static class RemoteSvnLocation implements ModelResourceLocation {
    private final URL url;

    RemoteSvnLocation(String urlSpec) {
      try {
        this.url = new URL(urlSpec);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Connected connect() throws IOException {
      final URLConnection connection = url.openConnection();

      return new Connected() {
        @Override
        public String getResourceDescription() {
          return buildOriginReference(url.toExternalForm(), connection);
        }

        @Override
        public InputStream getContent() throws IOException {
          return connection.getInputStream();
        }
      };
    }

    private static String buildOriginReference(String url, URLConnection connection) {
      String revision = "<unknown>";
      String eTag = connection.getHeaderField("ETag");
      if (eTag != null) {
        Matcher matcher = REVISION_PATTERN.matcher(eTag);
        if (matcher.find()) {
          revision = matcher.group(1);
        }
      }
      return url + "@" + revision;
    }

    private static final Pattern REVISION_PATTERN = Pattern.compile("^\"([\\d]+)//");
  }

  private static class LocalFileLocation implements ModelResourceLocation {
    private final String fileName;

    LocalFileLocation(String fileName) {
      this.fileName = fileName;
    }

    @Override
    public Connected connect() throws IOException {
      return new Connected() {
        @Override public String getResourceDescription() {
          return "Local file " + fileName;
        }

        @Override
        public InputStream getContent() throws IOException {
          File file = new File(fileName);
          return new FileInputStream(file);
        }
      };
    }
  }

  private static final ModelResourceLocation JSON_MODEL_FILE_URL = new LocalFileLocation(
      "Inspector.json.rev97678.patch106787");
}
